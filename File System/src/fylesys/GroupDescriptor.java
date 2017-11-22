package fylesys;

import util.Utils;

public class GroupDescriptor extends DataBlock {

	public static final int size = 32;
	
	private static int groupdescriptorid = 0;
	private int gDid;
	private int inode_table_pointer = 8;

	public GroupDescriptor(byte[] data) {
		super(data);
		gDid = groupdescriptorid++;
	}

	public int getgDid() {
		return gDid;
	}

	public int getInodeTablePointer() {
		return Utils.byteArrayToInt(super.getPortion(inode_table_pointer, 4));
	}

}
