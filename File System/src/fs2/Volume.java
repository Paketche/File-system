package fs2;

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
	Ext2File root;
	/**
	 * Holds the whole directory
	 */
	private ByteBuffer buffer;

	public static void traverse(Ext2File file, int indent) throws IOException {
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

	public Volume(String path) throws IOException {
		RandomAccessFile file = new RandomAccessFile(path, "r");
		byte[] full = new byte[(int) file.length()];
		file.readFully(full);
		file.close();

		buffer = ByteBuffer.wrap(full);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		initSuperBlock();
		System.out.println(superblock.volumeName());
		initRoot();
	}

	/**
	 * Initializes a copy a super block
	 */
	private void initSuperBlock() {
		// get the block that is just after the boot block
		superblock = new SuperBlock(this, blockSize);
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

		// calculate the offset fromthe start of the inode table pointer to the desired
		// inode
		int inodepointer = tablep + (number % superblock.inodesInGroup()) * Inode.size;

		return new Inode(this, inodepointer);
	}

	/**
	 * Reads a block from the volume
	 * 
	 * @param block
	 *          number of the block
	 * @return block's data
	 */
	byte[] getBlock(int block) {
		return getBytes(block * blockSize, blockSize);
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
	 * Reads {@code length} number of bytes from {@code offset} in {@code block}
	 * 
	 * 
	 * @param block
	 *          the block which will be read
	 * @param offset
	 *          position from start of block from where the reading will start
	 * @param length
	 *          number of bytes to be read
	 * @return Array of the read bytes
	 */
	byte[] getFromBlock(int block, int offset, int length) {
		return getBytes(block * blockSize + offset, length);
	}

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
	 * Initializes the root directory
	 */
	private void initRoot() {
		root = new Ext2File(this, getInode(2), "");
	}

	/**
	 * 
	 * @param parentPath
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public Ext2File getFile(String parentPath, String fileName) throws IOException {
		if (parentPath.equals("")) {
			return root.listExt2Files((str) -> {
				return str.equals(fileName);
			})[0];
		}

		int last = parentPath.lastIndexOf('/');
		String newParent = parentPath.substring(0, last);
		final String newChild = parentPath.substring(last + 1);

		Ext2File newFile = getFile(newParent, newChild).listExt2Files((str) -> {
			return str.equals(fileName);
		})[0];
		return newFile;
	}

	public int getByte(int i) {
		// TODO Auto-generated method stub
		return buffer.get(i);
	}

	public char readChar(int i) {
		return buffer.getChar(i);
	}
}
