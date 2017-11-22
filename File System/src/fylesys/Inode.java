package fylesys;

import java.util.HashMap;

import util.Utils;

public class Inode extends DataBlock {

	public static final int size = 128;
	int EXT2_N_BLOCKS = 12;

	public static final HashMap<String, String> flags = new HashMap<>();
	static {
		flags.put("C000", "Socket");
		flags.put("A000", "Symbolic Link");
		flags.put("8000", "Regular File");
		flags.put("6000", "Block Device");
		flags.put("4000", "Directory");
		flags.put("2000", "Character Device");
		flags.put("1000", "FIFO");

		flags.put("0800", "Set process User ID");
		flags.put("0400", "Set process Group ID");
		flags.put("0200", "Sticky bit");

		flags.put("0100", "User read");
		flags.put("0080", "User write");
		flags.put("0040", "User execute");

		flags.put("0020", "Group read");
		flags.put("0010", "Group write");
		flags.put("0008", "Group execute");

		flags.put("0004", "Others read");
		flags.put("0002", "Others write");
		flags.put("0001", "Others execute");
	}

	private int i_mode = 0;
	private int i_uid = 2;
	private int i_size_l = 4;
	private int i_atime = 8;
	private int i_ctime = 12;
	private int i_mtime = 16;
	private int i_dtime = 20;
	private int i_gid = 24;
	private int i_links_count = 26;
	private int i_block = 40;
	private int i_size_u = 108;

	public Inode(byte[] data) {
		super(data);
	}

	/**
	 * Retrieve file type and access rights.
	 * 
	 * @return file type and access rights
	 */
	public byte[] getI_mode() {
		return getPortion(i_mode, 2);
	}

	/**
	 * Returns Owner identifier
	 * 
	 * @return Owner identifier
	 */
	public int getI_uid() {
		return Utils.byteArrayToInt(getPortion(i_uid, 2));
	}

	/**
	 * Returns file length in bytes
	 * 
	 * @return
	 */
	public int getI_size() {

		return Utils.byteArrayToInt(getPortion(i_size_l, 4)) + Utils.byteArrayToInt(getPortion(i_size_u, 4)) * (int) Math.pow(2, 16);
	}

	/**
	 * Returns Time of last file access
	 * 
	 * @return
	 */
	public byte[] getI_atime() {
		return getPortion(i_atime, 4);
	}

	/**
	 * Returns Time that inode last changed
	 * 
	 * @return
	 */
	public byte[] getI_ctime() {
		return getPortion(i_ctime, 4);
	}

	/**
	 * Returns Time that file contents last changed
	 * 
	 * @return
	 */
	public byte[] getI_mtime() {
		return getPortion(i_mtime, 4);
	}

	/**
	 * Returns Time of file deletion
	 * 
	 * @return
	 */
	public byte[] getI_dtime() {
		return getPortion(i_dtime, 4);
	}

	/**
	 * Returns Group identifier
	 * 
	 * @return
	 */
	public int getI_gid() {
		return Utils.byteArrayToInt(getPortion(i_gid, 2));
	}

	/**
	 * Returns Hard links counter
	 * 
	 * @return
	 */
	public int getI_links_count() {
		return Utils.byteArrayToInt(getPortion(i_links_count, 2));
	}

	/**
	 * Returns an array of Pointers to data blocks
	 * 
	 * @return
	 */
	public int[] getI_block() {
		int[] blocks = new int[12];

		for (int i = 0; i < blocks.length; i++) {
			blocks[i] = Utils.byteArrayToInt(getPortion(i_block + i * 4, 4));
		}

		return blocks;
	}

	public String fileType() {

		byte[] bytes = Utils.LogicalAND(getI_mode(), Utils.HexStringTobyteArray("0xF000"));
		String flag = Utils.byteArrayToHexString(bytes);
		return flags.get(flag);
	}

	public String fileInfo() {
		char[] info = { 'r', 'w', 'x' };

		String fileinfo = (this.fileType().equals("Directory")) ? "d" : "-";
		byte[] flags = getI_mode();
		byte[] currentFlag = Utils.HexStringTobyteArray("0x0100");

		for (int i = 0; Utils.byteArrayToInt(currentFlag) > 0; i++) {
			// add to fileinfo: (flag & currentFlag > 0) ? r/w/x : -
			fileinfo = fileinfo + (Utils.byteArrayToInt(Utils.LogicalAND(flags, currentFlag)) > 0 ? info[i % 3] : "-");

			Utils.RigthLogicalShift(currentFlag, 1);
		}

		return fileinfo;
	}

	public static void main(String[] args) {

	}

}
