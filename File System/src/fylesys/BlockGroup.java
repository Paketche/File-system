package fylesys;

import java.util.ArrayList;

import util.Utils;

public class BlockGroup extends DataBlock {

	private int id;

	private int superblock = 0;
	private int[] groupDescriptors;
	private int inodetable;
	private int datablocks;

	public BlockGroup(byte[] data, int id, Volume vol) {
		super(data);
		int offset = 0;
		superblock = hasSuperBlock(id) ? offset : 1024;
		if (superblock == 0)
			offset = 1024;

		//SuperBlock thissuper = vol.getSuperBlock();
		GroupDescriptor thisgroup = new GroupDescriptor(getPortion(offset, 32 * (id + 1)));
		//inodetable = thisgroup.getInodeTablePointer();
		//datablocks = inodetable + thissuper.getInodesPerGroup() * 128;

	}

	public static boolean hasSuperBlock(int id) {
		return id == 0 || id == 1 || Utils.isPowerOf(id, 3) || Utils.isPowerOf(id, 5) || Utils.isPowerOf(id, 7);
	}

	public SuperBlock getSuperBlock() {
		if (superblock != 0)
			return new SuperBlock(getPortion(superblock, 1024));
		return null;
	}

	public GroupDescriptor[] getGroupDescriptors() {
		GroupDescriptor[] gd = new GroupDescriptor[groupDescriptors.length];
		for (int i = 0; i < gd.length; i++) {
			gd[i] = new GroupDescriptor(getPortion(groupDescriptors[i], 32));
		}
		return gd;
	}

}
