package fs2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;

import util.Utils;

public class Ext2File {

	private Volume volume;
	private Inode inode;

	/**
	 * Hold the absolute path of the file
	 */
	private String fullname;
	/**
	 * Hold the index of the last '/' in {@link #fullname}
	 */
	private int nameIndex;
	/**
	 * Holds the of the next byte to be read(used as a bookmark)
	 */
	private long possition;

	/**
	 * Holds the numbers of the blocks in the volume that hold the contents of the
	 * file
	 */
	private Integer[] blocks;

	/**
	 * Creates a new Ext2File object
	 * 
	 * @param vol
	 *          - the volume where the file is expected to be
	 * @param name
	 *          - absolute path of the file
	 * @throws FileNotFoundException
	 *           when the file cannot
	 */
	public Ext2File(Volume vol, String name) throws FileNotFoundException {
		volume = vol;
		fullname = name;
		nameIndex = name.lastIndexOf('/');
		inode = getParent().getFileInode(getName());
		if (inode == null)
			throw new FileNotFoundException();
	}

	/**
	 * Creates a new file
	 * 
	 * @param vol
	 *          - the volume where the file is expected to be
	 * @param inode
	 *          - of the file
	 * @param name
	 *          - of the file
	 */
	Ext2File(Volume vol, Inode inode, String name) {
		volume = vol;
		this.inode = inode;
		fullname = name;
		nameIndex = name.lastIndexOf('/');
	}

	/**
	 * Returns the name of the file
	 * 
	 * @return name of file
	 */
	public String getName() {
		return fullname.substring(nameIndex + 1);
	}

	/**
	 * Returns the parent file
	 * 
	 * @return parent file
	 * @throws FileNotFoundException
	 */
	public Ext2File getParent() throws FileNotFoundException {
		if (fullname.equals("/"))// the root returns itself
			return volume.getRoot();

		return volume.getFile(fullname.substring(0, nameIndex));
	}

	/**
	 * Returns the absolute pathname of this file
	 * 
	 * @return the absolute pathname string of this file
	 */
	public String getAbsolutePath() {
		return fullname;
	}

	/**
	 * Returns the size of the file in bytes
	 * 
	 * @return the size of the file in bytes
	 */
	public long size() {
		return inode.getI_size();
	}

	/**
	 * Tests whether the file is a directory.
	 * 
	 * @return true if the file is a directory
	 */
	public boolean isDirectory() {
		return (inode.getI_mode() & 0x4000) != 0 ? true : false;
	}

	/**
	 * Returns an array of files located in this directory( if this file is a
	 * directory)
	 * 
	 * @return an array of files located in this directory if this is a directory.
	 *         </br>
	 *         null - otherwise
	 */
	public Ext2File[] listExt2Files() {
		return listExt2Files(null);
	}

	/**
	 * Returns an array of files located in this directory( if this file is a
	 * directory)
	 * 
	 * @param filter
	 *          - A file filter
	 * @return an array of files located in this directory filtered by the filter.
	 *         The array will be empty if the directory is empty</br>
	 *         null - if the file does not denote a directory
	 */
	public Ext2File[] listExt2Files(Ext2FilenameFilter filter) {
		if (isDirectory()) {
			ArrayList<InodeandName> i_n = getInodes_and_Names(filter);
			Ext2File[] subfiles = new Ext2File[i_n.size() - 2];

			// starts from second to pass self and parent references
			for (int i = 2; i < i_n.size(); i++) {
				InodeandName entry = i_n.get(i);

				subfiles[i - 2] = new Ext2File(volume, volume.getInode(entry.inode), fullname + "/" + entry.name);
			}

			return subfiles;
		}
		return null;
	}

	/**
	 * Prints out the contents of this directory in Unix like format
	 */
	public void ls() {
		if (isDirectory()) {
			ArrayList<InodeandName> i_n = getInodes_and_Names(null);

			for (Iterator<InodeandName> iterator = i_n.iterator(); iterator.hasNext();) {
				InodeandName entry = iterator.next();

				System.out.println(volume.getInode(entry.inode).fileInfo(entry.name));
			}
		}
	}

	/**
	 * Initializes the contents of a file
	 */
	private void initFileContents() {
		// this holds the numbers of all the blocks that hold contents of this file
		blocks = inode.getBlockPointers();
		possition = 0;
	}

	/**
	 * Returns an array of data from this file.<br>
	 * If the method tries to read from a valid start byte past the end of the file
	 * it will return data only up to the end of the file.<br>
	 * If the method tries to start reading past the end of the file it will return
	 * null.
	 * 
	 * @param startByte
	 *          - the start from where the data will be read
	 * @param length
	 *          - number of bytes to read
	 * @return Returns an array of data from this file
	 */
	public byte[] read(long startByte, int length) {
		if (blocks == null)
			initFileContents();

		if (startByte == size()) {// for reading from past end
			return null;
		}
		else if (startByte + length > size()) {// getting only available bytes
			length = (int) (size() - startByte);
		}

		// hold the read bytes
		byte[] temp = new byte[length];

		// index for temp
		int i = 0;
		while (i < length) {
			// get the number of the block(the number is that index in the blocks array)
			int blockNumber = (int) (startByte / Volume.blockSize);

			// get the offset from the start of the block
			int startInBlock = (int) (startByte % Volume.blockSize);

			// get the number of bytes you're going to copy from that block
			// it can be: until the end of the block; just a chunk in it of size length
			int copyLength = Math.min(Volume.blockSize - startInBlock, length - i);

			// use the fact that java initializes the temp array with zeros and just skip
			// over this when the user asks to read from a hole
			if (blocks[blockNumber] != 0)
				volume.copy(blocks[blockNumber], startInBlock, temp, i, copyLength);

			// forwards the index of temp
			i += copyLength;
			// forward the start byte variable so that in the next iteration of the loop you
			// can read from a different block (if needed)
			startByte += copyLength;
		}
		return temp;
	}

	/**
	 * Returns an array of data from this file. Starting from the mark in this file
	 * 
	 * @param length
	 *          - number of bytes to read
	 * @return Returns an array of data from this file
	 */
	public byte[] read(int length) throws IOException {
		byte[] temp = read(possition, length);
		possition += length;
		return temp;
	}

	/**
	 * Changes the position of the mark
	 * 
	 * @param possition
	 */
	public void seek(long possition) {
		this.possition = possition;
	}

	/**
	 * Returns the whole contents of the file
	 * 
	 * @return whole contents of the file
	 */
	public byte[] fullyRead() {
		if (blocks == null) {
			initFileContents();
		}
		return read(0, blocks.length * 1024);
	}

	/**
	 * If this file is a directory it scans it for the file specified in the
	 * parameter and returns its inode object
	 * 
	 * @param name
	 *          of the searched file
	 * @return inode object if the file is found and this is a directory </br>
	 *         null - otherwise
	 */
	Inode getFileInode(String name) {
		if (isDirectory()) {

			ArrayList<InodeandName> i_n = this.getInodes_and_Names((subfileName) -> {
				return subfileName.equals(name);
			});

			// returning the first because there would be only one file with that name
			if (i_n.size() > 0)
				return volume.getInode(i_n.get(0).inode);

		}
		return null;
	}

	/**
	 * Returns a list with entries mapping inode numbers and names of subfiles
	 * 
	 * @param filter
	 *          a file filter
	 * @return a list with entries mapping inode numbers and names of subfiles
	 */
	private ArrayList<InodeandName> getInodes_and_Names(Ext2FilenameFilter filter) {
		ArrayList<InodeandName> subFiles = new ArrayList<>();

		ByteBuffer buffer = ByteBuffer.wrap(fullyRead());
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		int offset = 0;
		while (offset < size()) {
			int inode = buffer.getInt(offset);

			int length = buffer.getShort(offset + 4);
			int namelen = buffer.get(offset + 6);
			String name = Utils.byteArrayToASCIIString(this.read(offset + 8, namelen));

			if (filter == null || filter.accept(name)) {
				subFiles.add(new InodeandName(inode, name));
			}

			offset += length;
		}

		return subFiles;
	}

	/**
	 * Used to map inodes to file names (a hash map reorders the sequence of
	 * subfiles)
	 * 
	 * @author Georgi Valchanov
	 */
	private class InodeandName {
		int inode;
		String name;

		public InodeandName(int inode, String name) {
			this.inode = inode;
			this.name = name;
		}
	}
}
