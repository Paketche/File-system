package fylesys;

import util.Utils;

public class SuperBlock extends DataBlock {

	/**
	 * 
	 */
	int s_inodes_count = 0;
	int s_blocks_count = 4;
	int s_blocks_per_group = 32;
	int s_inodes_per_group = 40;
	int s_magic = 56;
	int s_volume_name = 120;

	public SuperBlock(byte[] data) {
		super(data);
	}

	public int getInodeCount() {
		return Utils.byteArrayToInt(super.getPortion(s_inodes_count, 4));
	}

	public int getBlocksCount() {
		return Utils.byteArrayToInt(super.getPortion(s_blocks_count, 4));
	}

	public int getBlocksPerGroup() {
		return Utils.byteArrayToInt(super.getPortion(s_blocks_per_group, 4));
	}

	public int getInodesPerGroup() {
		return Utils.byteArrayToInt(super.getPortion(s_inodes_per_group, 4));
	}

	public String getVolumeName() {
		String name = "";
		byte[] nameInbytes = getPortion(s_volume_name, 16);
		
		for (int i = 0; i < nameInbytes.length; i++) {
			name += (char) (nameInbytes[i] & 0xff);
		}
		
		return name;
	}

}
