package fylesys;

import java.io.IOException;
import java.io.RandomAccessFile;

public class Volume {

	private int blockSize = 1024;

	private SuperBlock superBlock;
	private GroupDescriptor[] groupDescriptors;
	private Inode[] inodeTable;

	private RandomAccessFile file;
	private LittleEndianBuffer buffer;

	public Volume(String path) throws IOException {
		file = new RandomAccessFile(path, "r");
		byte[] full = new byte[(int) file.length()];
		file.readFully(full);
		buffer = new LittleEndianBuffer(full, 1024);

		initSuperBlock();
		initGroupDescriptors();
		initInodeTable();
	}

	/**
	 * Initializes a copy a super block
	 */
	private void initSuperBlock() {
		// get the block that is just after the boot block
		superBlock = new SuperBlock(buffer.readBlock(blockSize));
	}

	/**
	 * Initializes an array of Group Descriptors
	 */
	private void initGroupDescriptors() {
		// gets the number of group descriptors
		// if the division has a remainder we add 1 to round it up
		int gds = superBlock.getInodeCount() / superBlock.getInodesPerGroup()
				+ (superBlock.getInodeCount() % superBlock.getInodesPerGroup() == 0 ? 0 : 1);

		groupDescriptors = new GroupDescriptor[gds];

		for (int i = 0; i < groupDescriptors.length; i++) {
			// Read data of size equal to a group descriptors size,
			// i * group descriptor's size is used as an offset
			// start reading from the second block where the first group descriptor is
			groupDescriptors[i] = new GroupDescriptor(buffer.read(2 * blockSize + i * GroupDescriptor.size, GroupDescriptor.size));
		}
	}

	/**
	 * Initializes an array of all the inodes
	 */
	private void initInodeTable() {

		inodeTable = new Inode[superBlock.getInodeCount()];

		// filles in the inode table
		for (int i = 0; i < inodeTable.length; i++) {
			/*
			 * get the offset from the begging of an inode table:
			 * 
			 * say we want inode 5 and there are 3 inodes per group block this variable will
			 * then be 1 because inode 5 will be the second inode in the second block group
			 */
			int inode = i % superBlock.getInodesPerGroup();

			/*
			 * index pointing to a group descriptor int the group descriptor array that the
			 * inode will be in
			 */
			int groupDescritor = i / superBlock.getInodesPerGroup();

			// get the inode table pointer
			inodeTable[i] = new Inode(
					buffer.read(groupDescriptors[groupDescritor].getInodeTablePointer() * blockSize + inode * Inode.size, Inode.size));
		}
	}

	/**
	 * Retrieves an Inode
	 * 
	 * @param inodeNumber
	 *          id number of the inode
	 * @return specified inode
	 */
	Inode getInode(int inodeNumber) {
		if (inodeNumber < 1) {
			throw new NullPointerException();
		}

		return inodeTable[inodeNumber - 1];
	}
}
