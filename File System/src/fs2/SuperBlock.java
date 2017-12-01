package fs2;

public class SuperBlock {
	/*
	 * Offsets
	 */
	private static final int numInodesFileSys = 0;
	private static final int numBlocksFileSys = 4;
	private static final int numBlocksGroup = 32;
	private static final int numInodesGroup = 40;
	private static final int inodeSize = 88;
	private static final int volLabel = 120;

	/**
	 * The number of inodes in the file system
	 */
	private final int inodefs;
	/**
	 * The number of data blocks in the file system
	 */
	private final int blocksfs;
	/**
	 * The number of blocks per block group
	 */
	private final int blocksgr;
	/**
	 * The number of inodes per block group
	 */
	private final int inodesgr;
	/**
	 * Size of an inode in bytes
	 */
	private final int inodesz;
	/**
	 * Label of the volume
	 */
	private final String label;

	/**
	 * Creates Super Block data block
	 * 
	 * @param vol
	 *          volume where The super block data is written
	 * @param offset
	 *          the byte from which the data of the Super Block is written on the
	 *          volume
	 */
	public SuperBlock(Volume volume, int offset) {
		inodefs = volume.getIntAt(offset + numInodesFileSys);
		blocksfs = volume.getIntAt(offset + numBlocksFileSys);
		blocksgr = volume.getIntAt(offset + numBlocksGroup);
		inodesgr = volume.getIntAt(offset + numInodesGroup);
		inodesz = volume.getIntAt(offset + inodeSize);
		label = volume.getStringAt(offset + volLabel, 16);
	}

	/**
	 * Returns the number of inodes in the file system
	 * 
	 * @return number of inodes in the file system
	 */
	public int inodesInFileSystem() {
		return inodefs;
	}

	/**
	 * Returns the number of data blocks in the file system
	 * 
	 * @return number of data blocks in the file system
	 */
	public int blocksInFileSystem() {
		return blocksfs;
	}

	/**
	 * Returns the number of blocks per block group
	 * 
	 * @return the number of blocks per block group
	 */
	public int blocksInGroups() {
		return blocksgr;
	}

	/**
	 * Returns the number of inodes per block group
	 * 
	 * @return the number of inodes per block group
	 */
	public int inodesInGroup() {
		return inodesgr;
	}

	/**
	 * Returns the size of an inode in bytes
	 * 
	 * @return size of an inode in bytes
	 */
	public int inodeSize() {
		return inodesz;
	}

	/**
	 * Returns the label of the volume
	 * 
	 * @return the label of the volume
	 */
	public String volumeName() {
		return label;
	}
}
