package fs2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import util.Utils;

public class Ext2File {

	private Volume volume;
	Inode inode;

	/**
	 * Hold the path of the file
	 */
	private String fullname;
	/**
	 * Hold the index of the last '/' in {@link #fullname}
	 */
	private int nameIndex;
	/**
	 * Size of file in bytes
	 */
	private long size;
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
	 * @throws FileNotFoundException
	 *           when the path is either incorrect or the file doesnt exist
	 */
	public Ext2File(Volume vol, String name) throws FileNotFoundException {
		this(vol, null, name);
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
		size = inode.getI_size();
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
	 */
	public Ext2File getParent() {
		try {
			return volume.getFile(fullname.substring(0, nameIndex), fullname.substring(nameIndex + 1));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * 
	 * @return
	 */
	public String ls() {
		StringBuilder builder = new StringBuilder();
		builder.append(isDirectory() ? 'd' : '-');

		// checking the access permissions
		int access = inode.getI_mode() & 0x1FF;
		char[] rwx = { 'r', 'w', 'x' };
		int flagchecker = 0x100;

		for (int i = 0; flagchecker > 0; i++) {
			builder.append(((access & flagchecker) > 0) ? rwx[i % 3] : '-');
			flagchecker >>= 1;
		}

		builder.append(" " + inode.getI_links_count());
		builder.append("" + inode.getI_uid());
		builder.append(" " + inode.getI_gid());

		builder.append(" " + new SimpleDateFormat("MMM dd HH:mm").format(inode.getI_mtime()));
		builder.append(" " + getName());

		return builder.toString();
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
	public Inode getFileInode(String name) {
		return null;
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
	 * Retrieves the first occurrence of a file that is accepted by the file filter
	 * @param filter
	 * @return
	 * @throws IOException
	 */
	public Ext2File getFirstFile(Ext2FilenameFilter filter) throws IOException {
		Ext2File[] fiels = listExt2Files(filter);

		return (fiels.length == 0) ? null : fiels[0];
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
	public Ext2File[] listExt2Files() throws IOException {
		return listExt2Files(null);
	}

	/**
	 * 
	 * @param filter
	 * @return
	 * @throws IOException
	 */
	public Ext2File[] listExt2Files(Ext2FilenameFilter filter) throws IOException {
		if (!isDirectory()) {
			return null;
		}
		else {
			if (blocks == null)
				initFileContents();

			ArrayList<Ext2File> inodes = new ArrayList<>();

			// get past current and parent
			int offset = Utils.byteArrayToInt(this.read(4, 2));
			offset += Utils.byteArrayToInt(this.read(offset + 4, 2));

			int inode = Utils.byteArrayToInt(this.read(offset, 4));
			while (inode > 0 && offset < size()) {

				int length = Utils.byteArrayToInt(this.read(offset + 4, 2));
				int namelen = Utils.byteArrayToInt(this.read(offset + 6, 1));
				String name = Utils.byteArrayToASCIIString(this.read(offset + 8, namelen));

				if (filter == null || filter.accept(name))
					inodes.add(new Ext2File(volume, volume.getInode(inode), fullname + "/" + name));

				offset += length;
				if (offset == Volume.blockSize)
					break;
				inode = Utils.byteArrayToInt(this.read(offset, 4));
			}

			Ext2File[] subfiles = new Ext2File[inodes.size()];
			inodes.toArray(subfiles);
			return subfiles;
		}

	}

	/**
	 * 
	 * @param startByte
	 * @param length
	 * @return
	 * @throws IOException
	 */
	public byte[] read(long startByte, int length) throws IOException {
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
	public long size() {
		return size;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public byte[] fullyRead() throws IOException {
		if (blocks == null) {
			initFileContents();
		}
		return read(0, blocks.length * 1024);
	}

	/**
	 * Initializes the contents of a file
	 */
	private void initFileContents() {

		// this holds the numbers of all the blocks that hold contents of this file
		blocks = inode.getBlockPointers();
		possition = 0;
	}
}
