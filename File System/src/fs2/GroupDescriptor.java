package fs2;

public class GroupDescriptor {

	/**
	 * Size of group descriptor in bytes
	 */
	public static final int size = 32;

	/**
	 * The offset of the inode table pointer
	 */
	private static final int inodeTablePointer = 8;
	
	/**
	 * Block number the group's inode table
	 */
	private int inodeTableBlNumber;

	int offset;
	/**
	 * Creates a new Group Descriptor
	 * @param volume
	 * @param offset
	 */
	public GroupDescriptor(Volume volume, int offset) {
		this.offset = offset;
		inodeTableBlNumber =volume.getIntAt(offset + inodeTablePointer);
	}

	/**
	 * Returns the block number the group's inode table
	 * @return Block number the group's inode table
	 */
	public int inodeTablePointer() {
		return inodeTableBlNumber ;
	}
}
