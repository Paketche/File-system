package fs2;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import util.Utils;

public class Ext2File {

	private Volume volume;
	private Inode inode;

	/**
	 * Hold the path of the file
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
	 * Holds the number of the blocks in the volume that hold the contents of the
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
	 * @throws IOException
	 */
	public Ext2File(Volume vol, String name) throws FileNotFoundException {
		volume = vol;
		fullname = name;
		nameIndex = name.lastIndexOf('/');
		inode = getParent().getFileInode(getName());
	}

	/**
	 * Creates a new file
	 * 
	 * @param vol
	 * @param inode
	 * @param name
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
		if (!fullname.equals(""))
			return volume.getFile(fullname.substring(0, nameIndex));
		return this;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
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
	 * Returns an array of file locate in this directory( if the this is a
	 * directory) {@link #listExt2Files() }
	 * 
	 * @return Returns an array of file locate in this directory if this is a
	 *         directory </br>
	 *         null - otherwise
	 * @throws IOException
	 */
	public Ext2File[] listExt2Files() {
		return listExt2Files(null);
	}

	/**
	 * 
	 * @param filter
	 * @return
	 * @throws IOException
	 */
	public Ext2File[] listExt2Files(Ext2FilenameFilter filter) {
		if (!isDirectory()) {
			return null;
		}
		else {
			if (blocks == null)
				initFileContents();

			ArrayList<String> i_n = getInodes_and_Names();
			Ext2File[] subfiles = new Ext2File[i_n.size() - 2];

			for (int i = 2; i < i_n.size(); i++) {
				String entry = i_n.get(i);
				int sep = entry.indexOf(' ');

				int inodeNum = Integer.parseInt(entry.substring(0, sep));
				String name = entry.substring(sep + 1);

				subfiles[i - 2] = new Ext2File(volume, volume.getInode(inodeNum), fullname + "/" + name);
			}

			return subfiles;
		}

	}

	public void ls() throws IOException {
		ArrayList<String> i_n = getInodes_and_Names();

		for (int i = 0; i < i_n.size(); i++) {
			String entry = i_n.get(i);
			int sep = entry.indexOf(' ');

			int inodeNum = Integer.parseInt(entry.substring(0, sep));
			String name = entry.substring(sep + 1);

			System.out.println(volume.getInode(inodeNum).fileInfo(name));
		}
	}

	/**
	 * 
	 * @param startByte
	 * @param length
	 * @return
	 * @throws EOFException
	 * @throws IOException
	 */
	public byte[] read(long startByte, int length) {
		if (blocks == null)
			initFileContents();

		// hold the read bytes
		byte[] temp = new byte[length];

		// index for temp
		int i = 0;
		while (i < length) {
			// get the number of the block(the number is tha index in the blocks array)
			int blockNumber = (int) (startByte / Volume.blockSize);

			// get the offset from the start of the block
			int startInBlock = (int) (startByte % Volume.blockSize);

			// get the number of bytes you're going to copy from that block
			// it can be: until the end of the block; the whole block; just a chunk in of
			// size length
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
	 * 
	 * @param length
	 * @return
	 * @throws IOException
	 */
	public byte[] read(int length) throws IOException {
		byte[] temp = read(possition, length);
		possition += length;
		return temp;
	}

	/**
	 * 
	 * @param possition
	 */
	public void seek(long possition) {
		this.possition = possition;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
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

			ArrayList<String> i_n = this.getInodes_and_Names();

			for (int i = 2; i < i_n.size(); i++) {
				String entry = i_n.get(i);
				int sep = entry.indexOf(' ');

				//separate the inode from the the file name
				int inodeNum = Integer.parseInt(entry.substring(0, sep));
				String name2 = entry.substring(sep + 1);

				if (name.equals(name2)) {
					return volume.getInode(inodeNum);
				}
			}
		}
		return null;
	}

	/**
	 * Initializes the contents of a file
	 */
	private void initFileContents() {

		// this holds the numbers of all the blocks that hold contents of this file
		blocks = inode.getBlockPointers();
		possition = 0;
	}

	private ArrayList<String> getInodes_and_Names() {
		ArrayList<String> subfiles = new ArrayList<>();

		int offset = 0;

		ByteBuffer buffer = ByteBuffer.wrap(this.fullyRead());
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		while (offset < size()) {
			// int inode = Utils.byteArrayToInt(this.read(offset, 4));
			int inode = buffer.getInt(offset);

			
			int length = buffer.getShort(offset + 4);
			int namelen = buffer.get(offset + 6);
			String name = Utils.byteArrayToASCIIString(this.read(offset + 8, namelen));

			subfiles.add(inode + " " + name);

			offset += length;
		}

		return subfiles;
	}
}
