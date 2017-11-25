package fs2;

public class SuperBlock {

	private Volume volume;
	private int offset;

	private static int numInodesFileSys = 0;
	private static int numBlocksFileSys = 4;
	private static int numBlocksGroup = 32;
	private static int numInodesGroup = 40;
	private static int inodeSize = 88;
	private static int volLable = 120;

	public SuperBlock(Volume vol, int offset) {
		volume = vol;
		this.offset = offset;
	}

	public int inodesInFileSystem() {
		return volume.getIntAt(offset + numInodesFileSys, 4);
	}

	public int blocksInFileSystem() {
		return volume.getIntAt(offset + numBlocksFileSys, 4);
	}

	public int blocksInGroups() {
		return volume.getIntAt(offset + numBlocksGroup, 4);
	}

	public int inodesInGroup() {
		return volume.getIntAt(offset + numInodesGroup, 4);
	}

	public int inodeSize() {
		return volume.getIntAt(offset + inodeSize, 4);
	}

	public String volumeName() {
		return volume.getStringAt(offset + volLable, 16);
	}
}
