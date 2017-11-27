package fs2;

import java.io.IOException;
import java.util.ArrayList;

import util.Utils;

public class Ext2File {

	private Volume volume;
	private Inode inode;

	private String fullname;
	private int nameIndex;
	private int size;

	private long possition;

	private LittleEndianBuffer buffer;

	private Ext2File[] subfiles;

	public Ext2File(Volume vol, String name) {
		volume = vol;
		fullname = name;
		nameIndex = name.lastIndexOf('/');
		

	}

	Ext2File(Volume vol, Inode inode, String name) {
		volume = vol;
		this.inode = inode;
		fullname = name;
		size = inode.getI_size();
		//nameIndex = name.lastIndexOf('/');
	}

	/**
	 * Returns the name of the file
	 * 
	 * @return name of file
	 */
	public String getName() {
		return fullname.substring(nameIndex);
	}

	/**
	 * Returns the parent file
	 * 
	 * @return parent file if it exists <br>
	 *         null - if not
	 */
	public Ext2File getParent() {
		try {
			return volume.getFile(fullname.substring(0, nameIndex), fullname.substring(nameIndex));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	public void ls() {
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
	 * Tests whether the file denoted by this abstract pathname is a directory.
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
			initFileContents();
			if (subfiles == null) {
				ArrayList<Ext2File> inodes = new ArrayList<>();

				// get past current and parent
				int offset = Utils.byteArrayToInt(buffer.read(4, 2));
				offset += Utils.byteArrayToInt(buffer.read(offset + 4, 2));

				int inode = buffer.readInt(offset, 4);
				while (inode > 0 && offset < size()) {

					int length = buffer.readInt(offset + 4, 2);
					int namelen = buffer.readInt(offset + 6, 1);
					String name = buffer.readString(offset + 8, namelen);

					if (filter == null || filter.accept(name))
						inodes.add(new Ext2File(volume, volume.getInode(inode), name));

					offset += length;
					if (offset == Volume.blockSize)
						break;
					inode = buffer.readInt(offset, 4);
				}

				subfiles = new Ext2File[inodes.size()];
				inodes.toArray(subfiles);
			}
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
	public byte[] read(long startByte, long length) throws IOException {

		return buffer.read((int) startByte, (int) length);
	}

	/**
	 * 
	 * @param length
	 * @return
	 * @throws IOException
	 */
	public byte[] read(long length) throws IOException {
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
	public int size() throws IOException {
		return size;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public byte[] fullyRead() throws IOException {
		return read(0, size());
	}

	/**
	 * Initializes the contents of a file
	 */
	private void initFileContents() {
		// this holds the numbers of all the blocks that hold contents of this file
		Integer[] dataBlocks = inode.getBlockPointers();

		buffer = new LittleEndianBuffer(dataBlocks.length * Volume.blockSize);

		for (Integer db : dataBlocks) {
			buffer.write(volume.getBlock(db));
		}

		buffer.flip();
	}

}
