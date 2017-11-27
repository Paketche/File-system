package fs2;

public class GroupDescriptor {

	public static final int size = 32;

	private Volume volume;
	int offset;

	private static final int inodeTablePointer = 8;

	public GroupDescriptor(Volume vol, int offset) {
		volume = vol;
		this.offset = offset;
	}

	public int inodeTablePointer() {
		return volume.getIntAt(offset + inodeTablePointer, 4);
	}
}
