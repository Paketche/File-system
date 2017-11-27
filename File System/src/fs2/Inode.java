package fs2;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class Inode {

	public static final int size = 128;

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

	private Integer[] blockPointers;
	private int BlocksPointersTogo;

	private Volume volume;
	int offset;

	public Inode(Volume vol, int offset) {
		volume = vol;
		this.offset = offset;

		BlocksPointersTogo = getI_size() / Volume.blockSize + (getI_size() % Volume.blockSize == 0 ? 0 : 1);
	}

	/**
	 * Retrieve file type and access rights.
	 * 
	 * @return file type and access rights
	 */
	public int getI_mode() {
		return volume.getIntAt(offset + i_mode, 2);
	}

	/**
	 * Returns Owner identifier
	 * 
	 * @return Owner identifier
	 */
	public int getI_uid() {
		return volume.getIntAt(offset + i_uid, 2);
	}

	/**
	 * Returns file length in bytes
	 * 
	 * @return
	 */
	public int getI_size() {
		return volume.getIntAt(offset + i_size_u, 4) * (int) Math.pow(2, 16) + volume.getIntAt(offset + i_size_l, 4);
	}

	/**
	 * Returns Time of last file access
	 * 
	 * @return
	 */
	public Date getI_atime() {
		long date = volume.getIntAt(offset + i_atime, 4) * 1000;
		return new Date(date);
	}

	/**
	 * Returns Time that inode last changed
	 * 
	 * @return
	 */
	public Date getI_ctime() {
		long date = volume.getIntAt(offset + i_ctime, 4) * 1000;
		return new Date(date);
	}

	/**
	 * Returns Time that file contents last changed
	 * 
	 * @return
	 */
	public Date getI_mtime() {
		long date = volume.getIntAt(offset + i_mtime, 4) * 1000;
		return new Date(date);
	}

	/**
	 * Returns Time of file deletion
	 * 
	 * @return
	 */
	public Date getI_dtime() {
		long date = volume.getIntAt(offset + i_dtime, 4) * 1000;
		return new Date(date);
	}

	/**
	 * Returns Group identifier
	 * 
	 * @return
	 */
	public int getI_gid() {
		return volume.getIntAt(offset + i_gid, 2);
	}

	/**
	 * Returns Hard links counter
	 * 
	 * @return
	 */
	public int getI_links_count() {
		return volume.getIntAt(offset + i_links_count, 2);
	}

	/**
	 * Returns an array of Pointers to data blocks
	 * 
	 * @return
	 */
	public int[] getI_block() {
		int[] blocks = new int[12];

		for (int i = 0; i < blocks.length; i++) {
			int offseti = offset + i_block + i * 4;
			int value = this.volume.getIntAt(offseti, 4);
			blocks[i] = value;
		}

		return blocks;
	}

	/**
	 * 
	 * @param number
	 * @return
	 */
	public int get_indirect(int number) {
		return volume.getIntAt(offset + first_ind + (number - 1) * 4, 4);
	}

	/**
	 * 
	 * @return
	 */
	public Integer[] getBlockPointers() {
		if (this.blockPointers == null) {
			ArrayList<Integer> blockpointers = new ArrayList<>();

			blockGathering: {
				// get first 12 block
				int[] first12 = getI_block();

				for (int pointer : first12) {
					// the pointer is a zero all of the file blocks are read
					if (pointer != 0) {
						blockpointers.add(pointer);
					}

					if (--BlocksPointersTogo == 0)
						break blockGathering;
				}

				// get the block pointers from the indirect pointers
				for (int i = 1; i < 3 && BlocksPointersTogo > 0; i++) {
					ArrayList<Integer> nums = indirectPointers(get_indirect(i), i);
					blockpointers.addAll(nums);
				}
			}

			blockPointers = new Integer[blockpointers.size()];
			blockpointers.toArray(blockPointers);
		}
		return blockPointers;
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
				int blockNumber = volume.getIntAt(block + i, 4);
				// no need to read in this case

				if (blockNumber != 0)
					pointers.add(blockNumber);

				if (BlocksPointersTogo == 0)
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
}
