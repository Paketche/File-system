package fs2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class Inode {

	/**
	 * Size of an inode in bytes
	 */
	public static final int size = 128;

	/**
	 * Offsets
	 */
	private static final int i_mode = 0;
	private static final int i_uid = 2;
	private static final int i_size_l = 4;
	private static final int i_atime = 8;
	private static final int i_ctime = 12;
	private static final int i_mtime = 16;
	private static final int i_dtime = 20;
	private static final int i_gid = 24;
	private static final int i_links_count = 26;
	private static final int i_block = 40;
	private static final int first_ind = 88;
	private static final int i_size_u = 108;

	/**
	 * File mode
	 */
	private int mode;
	/**
	 * User ID of owner
	 */
	private int uid;
	/**
	 * Size in bytes
	 */
	private long sizeB;
	/**
	 * Last access time
	 */
	private Date atime;
	/**
	 * Last time the inode changed
	 */
	private Date ctime;
	/**
	 * Last time that file contents changed
	 */
	private Date mtime;
	/**
	 * Time the file was deleted
	 */
	private Date dtime;
	/**
	 * Group ID of owner
	 */
	private int gid;
	/**
	 * Count of hard links to file
	 */
	private short link_count;

	/**
	 * Hold The number of block that we
	 */
	private long BlocksPointersTogo;

	private Volume volume;
	private int offset;

	public Inode(Volume vol, int offset) {
		volume = vol;
		this.offset = offset;

		mode = volume.getShortAt(offset + i_mode);
		uid = volume.getShortAt(offset + i_uid);

		// combines the upper and lower bits of the size
		sizeB = (volume.getIntAt(offset + i_size_u) * (long) Math.pow(2, 32)) + volume.getIntAt(offset + i_size_l);

		atime = new Date(volume.getIntAt(offset + i_atime) * 1000);
		ctime = new Date(volume.getIntAt(offset + i_ctime) * 1000);
		mtime = new Date(volume.getIntAt(offset + i_mtime) * 1000);
		dtime = new Date(volume.getIntAt(offset + i_dtime) * 1000);

		gid = volume.getShortAt(offset + i_gid);
		link_count = volume.getShortAt(offset + i_links_count);

		BlocksPointersTogo = getI_size() / Volume.blockSize + (getI_size() % Volume.blockSize == 0 ? 0 : 1);
	}

	/**
	 * Retrieve file type and access rights.
	 * 
	 * @return file type and access rights
	 */
	public int getI_mode() {
		return mode;
	}

	/**
	 * Returns Owner identifier
	 * 
	 * @return Owner identifier
	 */
	public int getI_uid() {
		return uid;
	}

	/**
	 * Returns file length in bytes
	 * 
	 * @return
	 */
	public long getI_size() {
		return sizeB;
	}

	/**
	 * Returns Time of last file access
	 * 
	 * @return
	 */
	public Date getI_atime() {
		return atime;
	}

	/**
	 * Returns Time that inode last changed
	 * 
	 * @return
	 */
	public Date getI_ctime() {
		return ctime;
	}

	/**
	 * Returns Time that file contents last changed
	 * 
	 * @return
	 */
	public Date getI_mtime() {
		return mtime;
	}

	/**
	 * Returns Time of file deletion
	 * 
	 * @return
	 */
	public Date getI_dtime() {
		return dtime;
	}

	/**
	 * Returns Group identifier
	 * 
	 * @return
	 */
	public int getI_gid() {
		return gid;
	}

	/**
	 * Returns Hard links counter
	 * 
	 * @return
	 */
	public int getI_links_count() {
		return link_count;
	}

	/**
	 * Returns an array of all the block numbers of data blocks holding the contents
	 * of the inode's file
	 * 
	 * @return array of content block numbers
	 */
	public Integer[] getBlockPointers() {
		ArrayList<Integer> blockpointers = new ArrayList<>();

		blockGathering: {
			// get first 12 block
			int[] first12 = getI_block();

			// put them in into the blockpointers list
			for (int pointer : first12) {

				if (pointer != 0)
					blockpointers.add(pointer);

				if (--BlocksPointersTogo == 0)
					break blockGathering;
			}

			// get the block pointers from the indirect pointers
			for (int i = 1; i <= 3 && BlocksPointersTogo > 0; i++) {
				ArrayList<Integer> nums = indirectPointers(get_indirect(i), i);
				blockpointers.addAll(nums);
			}
		}

		Integer[] blockPointers = new Integer[blockpointers.size()];
		blockpointers.toArray(blockPointers);

		return blockPointers;
	}

	/**
	 * Returns an array of Pointers to data blocks
	 * 
	 * @return
	 */
	private int[] getI_block() {
		int[] blocks = new int[12];

		for (int i = 0; i < blocks.length; i++) {
			int value = this.volume.getIntAt(offset + i_block + i * 4);
			blocks[i] = value;
		}

		return blocks;
	}

	/**
	 * Returns the value of an indirect pointer
	 * 
	 * @param number
	 *          level of indirection
	 * @return the value of an indirect pointer
	 */
	private int get_indirect(int number) {
		return volume.getIntAt(offset + first_ind + (number - 1) * 4);
	}

	/**
	 * 
	 * @param block
	 *          the number of the block from with you should read
	 * @param level
	 * @return
	 */
	private ArrayList<Integer> indirectPointers(int block, int level) {

		if (level == 1) {
			block *= Volume.blockSize;
			// hold the block numbers that point to data block with the file's contents
			ArrayList<Integer> pointers = new ArrayList<>();

			// you start reading at block and you offset by 4 every time
			// because each pointer is 4 bytes
			for (int i = 0; i < Volume.blockSize; i += 4) {

				// get the block number
				int blockNumber = volume.getIntAt(block + i);

				if (blockNumber != 0)
					pointers.add(blockNumber);

				if (--BlocksPointersTogo == 0)
					break;
			}

			return pointers;
		}
		else {
			// get block numbers in this level
			// these point to the next blocks that hold other block numbers
			ArrayList<Integer> thisLevel = indirectPointers(block, 1);

			// this holds all the blocks numbers of the higher levels of indirections
			ArrayList<Integer> fromHigherLevel = new ArrayList<>();

			// you get the the block of pointers for each block number in thisLevel
			for (Iterator<Integer> i = thisLevel.iterator(); i.hasNext();) {
				int blockNumber = i.next();

				// get the pointers in the block whose number is blockNumber
				ArrayList<Integer> batch = indirectPointers(blockNumber, level - 1);

				// append the block number in batch to the pointers array
				fromHigherLevel.addAll(batch);
				if (BlocksPointersTogo == 0)
					break;
			}

			return fromHigherLevel;
		}
	}

	String fileInfo(String name) {
		StringBuilder builder = new StringBuilder();
		builder.append((mode & 0x4000) > 0 ? 'd' : '-');

		// checking the access permissions
		int access = this.getI_mode() & 0x1FF;
		char[] rwx = { 'r', 'w', 'x' };
		int flagchecker = 0x100;

		for (int i = 0; flagchecker > 0; i++) {
			builder.append((access & flagchecker) > 0 ? rwx[i % 3] : '-');
			flagchecker >>= 1;
		}

		builder.append(" " + getI_links_count());
		builder.append("" + getI_uid());
		builder.append(" " + getI_gid());

		builder.append(" " + getI_size());
		builder.append(" " + new SimpleDateFormat("MMM dd HH:mm").format(getI_atime()));
		builder.append(" " + name);

		return builder.toString();
	}
}
