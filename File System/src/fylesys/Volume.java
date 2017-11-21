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


	private void initSuperBlock(){
		// get the block that is just after the boot block
		superBlock = new SuperBlock(buffer.readBlock(blockSize));
	}

	private void initGroupDescriptors(){
		// gets the number of group descriptors
		// if the division has a remainder we add 1 to round it up
		int gds = superBlock.getInodeCount() / superBlock.getInodesPerGroup()
				+ (superBlock.getInodeCount() % superBlock.getInodesPerGroup() == 0 ? 0 : 1);
		
		groupDescriptors = new GroupDescriptor[gds];

		for (int i = 0; i < groupDescriptors.length; i++) {
			// reads 32 bytes into the descriptor
			// i is used as an offset
			groupDescriptors[i] = new GroupDescriptor(buffer.read(2048 + 32 * i, 32));
		}
	}

	private void initInodeTable(){

		inodeTable = new Inode[superBlock.getInodeCount()];

		// filles in the inode table
		for (int i = 0; i < inodeTable.length; i++) {
			/*
			 * 
			 * 
			 * 
			 * get the offset of the inode in the table:
			 * 
			 * say we want inode 5 and there are 3 inodes per group block this variable will
			 * then be 1 because inode 5 will be the second inode in the second block group
			 */
			int inode = i % superBlock.getInodesPerGroup();

			/*
			 * index of the group descriptor that the inode will be
			 * 
			 * say we want inode 6 and there are 3 inodes per group giving us 2; in cases
			 * where the division has no remainder remove 1 -> 6/3 = 2 -> 2-1 = 1 (1 is the
			 * index of the second group descriptor)
			 */
			int groupDescritor = i / superBlock.getInodesPerGroup();// + (i % superBlock.getInodesPerGroup() == 0 && i != 0 ? -1 : 0);

			// get the inode table pointer and read the inode that was calculated in the
			// "inode" variable (inodes are 128 bytes)
			inodeTable[i] = new Inode(buffer.read(groupDescriptors[groupDescritor].getInodeTablePointer() + inode * 128, 128));
		}
	}
}
