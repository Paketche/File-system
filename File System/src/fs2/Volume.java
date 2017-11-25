package fs2;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import util.Utils;

public class Volume {

	static final int blockSize = 1024;

	private SuperBlock superblock;
	private GroupDescriptor[] groupDescriptors;
	private Inode[] inodeTable;
	private Ext2File root;

	private ByteBuffer buffer;


	public static Volume initVolume(String path) throws IOException {
		Volume vol = new Volume(path);
		vol.initSuperBlock();
		vol.initGroupDescriptors();
		vol.initInodeTable();
		vol.initRoot();

//		LittleEndianBuffer.dumpHexRepresnetation(vol.getBytes(2048, 32));
//		// LittleEndianBuffer.dumpHexRepresnetation(vol.getBytes(2048 + 32, 32));
//		// LittleEndianBuffer.dumpHexRepresnetation(vol.getBytes(2048 + 32 * 2, 32));
//
//		System.out.println(vol.getInode(2).offset);
//		LittleEndianBuffer.dumpHexRepresnetation(vol.getBytes(vol.getInode(2).offset, 128));
//		
//		System.out.println(vol.getInode(1715).offset);
//		LittleEndianBuffer.dumpHexRepresnetation(vol.getBytes(vol.getInode(1715).offset, 128));
		
		

	
//			LittleEndianBuffer.dumpHexRepresnetation(vol.getFromBlock(0x54, 128, 128));
		
		// for (int i = 0; i < 3; i++) {
		// System.out.println(0x2054 + i);
		// LittleEndianBuffer.dumpHexRepresnetation(vol.getBytes(0x2054 * 1024, 128));
		// }
		// for (int i = 0; i < 3; i++) {
		// System.out.println(0x4003 + i);
		// LittleEndianBuffer.dumpHexRepresnetation(vol.getBytes(0x4003 * 1024, 128));
		// }

		// LittleEndianBuffer.dumpHexRepresnetation(vol.getBytes(8475136, 128));
		// System.out.println(vol.getInode(1715).offset);
		//
		// System.out.println(vol.getInode(1716).offset);
		// LittleEndianBuffer.dumpHexRepresnetation(vol.getBytes(vol.getInode(1716).offset,
		// 128));

		//Volume.traverse(vol.root, 0);

		Volume.traverse(vol.root, 0);
		
		return vol;
	}

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

	private Volume(String path) throws IOException {
		RandomAccessFile file = new RandomAccessFile(path, "r");
		byte[] full = new byte[(int) file.length()];
		file.readFully(full);
		file.close();
		buffer = ByteBuffer.wrap(full);
	}

	/**
	 * Initializes a copy a super block
	 */
	private void initSuperBlock() {
		// get the block that is just after the boot block
		superblock = new SuperBlock(this, blockSize);
	}

	/**
	 * Initializes an array of Group Descriptors
	 */
	private void initGroupDescriptors() {
		// gets the number of group descriptors
		// if the division has a remainder we add 1 to round it up
		int gds = superblock.inodesInFileSystem() / superblock.inodesInGroup()
				+ (superblock.inodesInFileSystem() % superblock.inodesInGroup() == 0 ? 0 : 1);

		groupDescriptors = new GroupDescriptor[gds];

		for (int i = 0; i < groupDescriptors.length; i++) {
			// Read data of size equal to a group descriptors size,
			// i * group descriptor's size is used as an offset
			// start reading from the second block where the first group descriptor is
			groupDescriptors[i] = new GroupDescriptor(this, 2 * blockSize + i * GroupDescriptor.size);
		}
	}

	/**
	 * Initializes an array of Inodes
	 */
	private void initInodeTable() {
		inodeTable = new Inode[superblock.inodesInFileSystem()];

		// filles in the inode table
		for (int i = 0; i < inodeTable.length; i++) {
			/*
			 * get the offset from the begging of an inode table:
			 * 
			 * say we want inode 5 and there are 3 inodes per group block this variable will
			 * then be 1 because inode 5 will be the second inode in the second block group
			 */
			int inode = i % superblock.inodesInGroup();

			/*
			 * index pointing to a group descriptor int the group descriptor array that the
			 * inode will be in
			 */
			int groupDescritor = i / superblock.inodesInGroup();

			if (inode == 2 && groupDescritor == 0) {

			}

			// get the inode table pointer
			inodeTable[i] = new Inode(this, groupDescriptors[groupDescritor].inodeTablePointer() * blockSize + inode * Inode.size);
		}
	}

	/**
	 * Return the specified inode
	 * 
	 * @param number
	 *          of inode
	 * @return inode
	 */
	Inode getInode(int number) {
		return inodeTable[number - 1];
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
		byte[] str = getBytes(offset, length);
		String string = "";

		for (int i = 0; i < str.length; i++) {
			string += (char) (str[i] & 0xff);
		}

		return string;
	}

	/**
	 * * Reads {@code length} number of bytes starting from {@code offset} and
	 * converts them to an unsigned {@link Integer}
	 * 
	 * @param offset
	 *          byte from which the reading starts
	 * @param length
	 *          number of bytes to be read
	 * @return Unsigned integer representation of the read bytes
	 */
	int getIntAt(int offset, int length) {
		return Utils.byteArrayToInt(getBytes(offset, length));
	}

	/**
	 * Initializes the root directory
	 */
	void initRoot() {
		root = new Ext2File(this, getInode(2), "root");
	}

	/**
	 * 
	 * @param substring
	 * @return
	 */
	public Ext2File getFile(String substring) {
		try {
			root.fullyRead();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
