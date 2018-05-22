package fs2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Volume {

	public static final int blockSize = 1024;

	/**
	 * A reference to the first super block in the file system
	 */
	private SuperBlock superblock;
	/**
	 * Reference to the root directory in the file system
	 */
	private Ext2File root;
	/**
	 * Holds the whole directory
	 */
	private ByteBuffer buffer;

	/**
	 * Creates a new volume
	 * 
	 * @param path
	 *          location of the file that hold the contents of the volume
	 * @throws IOException
	 *           when the file is not found
	 */
	public Volume(String path) throws IOException {
		RandomAccessFile file = new RandomAccessFile(path, "r");
		byte[] full = new byte[(int) file.length()];
		file.readFully(full);
		file.close();

		buffer = ByteBuffer.wrap(full);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		initSuperBlock();
		initRoot();
	}

	/**
	 * Returns the name of the volume
	 * 
	 * @return the name of the volume
	 */
	public String getName() {
		return superblock.volumeName();
	}

	/**
	 * Returns the super block of the volume
	 * 
	 * @return the super block of the volume
	 */
	public SuperBlock getSuperBlock() {
		return this.superblock;
	}

	/**
	 * Returns a group descriptor from this volume
	 * 
	 * @param number
	 *          on the group descriptor(starts at 0)
	 * @return a group descriptor
	 */
	public GroupDescriptor getGroupDescriptor(int number) {
		return new GroupDescriptor(this, 2 * blockSize + GroupDescriptor.size * number);
	}

	/**
	 * Returns a file from the volume
	 * 
	 * @param absolutePath
	 *          of the file
	 * @return a file from the volume
	 * @throws FileNotFoundException
	 *           - when the file does not exist
	 */
	public Ext2File getFile(String absolutePath) throws FileNotFoundException {
		if (absolutePath.equals("")) {
			return root;
		}

		// get the absolute path of the parent and just the name of the child
		String parent = absolutePath.substring(0, absolutePath.lastIndexOf('/'));
		String child = absolutePath.substring(absolutePath.lastIndexOf('/') + 1);

		Inode newFileInode = null;

		newFileInode = getFile(parent).getFileInode(child);

		if (newFileInode == null) {
			throw new FileNotFoundException();
		}

		Ext2File newFile = new Ext2File(this, newFileInode, absolutePath);

		return newFile;
	}

	public Ext2File getRoot() {
		return root;
	}

	/**
	 * Return the specified inode
	 * 
	 * @param number
	 *          of inode
	 * @return inode
	 */
	Inode getInode(int number) {
		number--;
		// calculate the number of the descriptor where the sought inode is located
		int gdNum = number / superblock.inodesInGroup();

		// the offset from which the group descriptor starts
		int gdoffset = 2 * blockSize + gdNum * GroupDescriptor.size;

		// calculate the offset from which the group descriptor's inode table start
		int tablep = new GroupDescriptor(this, gdoffset).inodeTablePointer() * blockSize;

		// calculate the offset from the start of the inode table pointer to the desired
		// inode
		int inodepointer = tablep + (number % superblock.inodesInGroup()) * Inode.size;

		return new Inode(this, inodepointer);
	}

	/**
	 * Reads {@code length} number of bytes starting from {@code offset}
	 * 
	 * @param offset
	 *          byte from which the reading starts
	 * @param length
	 *          number of bytes to be read
	 * @return Array of the read bytes
	 */
	byte[] getBytes(int offset, int length) {
		byte[] temp = new byte[length];

		buffer.position(offset);
		buffer.get(temp);

		return temp;
	}

	/**
	 * Copies data from this volume and puts it into an array
	 * 
	 * @param block
	 *          - number of the block where the data is located
	 * @param startInBlock
	 *          - offset from the start of the block
	 * @param array
	 *          - where the data will be copied
	 * @param startArray
	 *          - position in the array from where the data will start to be writen
	 * @param length
	 *          - of read data from volume
	 */
	void copy(int block, int startInBlock, byte[] array, int startArray, int length) {

		buffer.position(block * blockSize + startInBlock);
		buffer.get(array, startArray, length);
	}

	/**
	 * Reads {@code length} number of bytes starting from {@code offset} and
	 * converts them to a {@link String}
	 * 
	 * @param offset
	 *          byte from which the reading starts
	 * @param length
	 *          number of bytes to be read
	 * @return String representation of the read bytes
	 */
	String getStringAt(int offset, int length) {

		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < length; i++) {
			char chard = (char) (buffer.getChar(offset + i) & 0xff);
			builder.append(chard);
		}

		return builder.toString();
	}

	/**
	 * Reads an {@code int} starting from {@code offset} and converts them to an
	 * unsigned {@link Integer}
	 * 
	 * @param offset
	 *          byte from which the reading starts
	 * @return Unsigned integer representation of the read bytes
	 */
	int getIntAt(int offset) {
		return buffer.getInt(offset);
	}

	/**
	 * Reads an {@code short} starting from {@code offset} and converts them to an
	 * unsigned {@link Integer}
	 * 
	 * @param offset
	 * @return
	 */
	short getShortAt(int offset) {
		return buffer.getShort(offset);
	}

	/**
	 * Initializes a copy a super block
	 */
	private void initSuperBlock() {
		// get the block that is just after the boot block
		superblock = new SuperBlock(this, blockSize);
	}

	/**
	 * Initializes the root directory
	 * 
	 * @throws FileNotFoundException
	 */
	private void initRoot() throws FileNotFoundException {
		root = new Ext2File(this, getInode(2), "/");
	}

	/**
	 * Traverses a directory and prints out all files
	 * 
	 * @param file
	 *          directory to be traversed
	 * @param indent
	 *          number of indentation
	 */
	public static void traverse(Ext2File file, int indent) {
		Ext2File[] subs = file.listExt2Files();

		for (Ext2File sub : subs) {
			for (int i = 0; i < indent; i++) {
				System.out.print(" ");
			}

			System.out.println(sub.getName());
			if (sub.getName().equals("lost+found"))
				continue;
			if (sub.isDirectory()) {
				Volume.traverse(sub, indent + 2);
			}
		}
	}

}
