package fs2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
	 * The volume in which this inode is located
	 */
	private Volume volume;
	/**
	 * holds the offset from which the data of this inode is located in the volume
	 */
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
	 * @return file length in bytes
	 */
	public long getI_size() {
		return sizeB;
	}

	/**
	 * Returns Time of last file access
	 * 
	 * @return Time of last file access
	 */
	public Date getI_atime() {
		return atime;
	}

	/**
	 * Returns Time that inode last changed
	 * 
	 * @return Time that inode last changed
	 */
	public Date getI_ctime() {
		return ctime;
	}

	/**
	 * Returns Time that file contents last changed
	 * 
	 * @return Time that file contents last changed
	 */
	public Date getI_mtime() {
		return mtime;
	}

	/**
	 * Returns Time of file deletion
	 * 
	 * @return Time of file deletion
	 */
	public Date getI_dtime() {
		return dtime;
	}

	/**
	 * Returns Group identifier
	 * 
	 * @return Group identifier
	 */
	public int getI_gid() {
		return gid;
	}

	/**
	 * Returns Hard links count
	 * 
	 * @return number of Hard links
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
		// will hold all the numbers of block that hold the contents of a file
		ArrayList<Integer> blockpointers = new ArrayList<>();
		// how many blocks the file is made up of
		long BlocksPointersTogo = getI_size() / Volume.blockSize + (getI_size() % Volume.blockSize == 0 ? 0 : 1);

		// get first 12 block
		for (int i = 0; i < 12; i++) {

			blockpointers.add(volume.getIntAt(offset + i_block + i * 4));

			if (--BlocksPointersTogo <= 0)
				break;
		}

		// get the block pointers from the indirect pointers
		for (int i = 1; i <= 3 && BlocksPointersTogo > 0; i++) {
			// calculate how many pointers to get from this indirect pointer(all of the
			// pointers or the rest of needed)
			int toget = (int) Math.pow(256, i);
			toget = (int) Math.min(toget, BlocksPointersTogo);

			ArrayList<Integer> nums = indirectPointers(get_indirect(i), i, toget);
			blockpointers.addAll(nums);

			BlocksPointersTogo -= toget;
		}

		// Convert to array
		Integer[] blockPointers = new Integer[blockpointers.size()];
		blockpointers.toArray(blockPointers);

		return blockPointers;
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
	 * Returns data block numbers from an indirect pointer.
	 * 
	 * @param block
	 *          number(a value held by an indirect pointer)
	 * @param level
	 *          of in direction of the pointer
	 * @param blockstoget
	 *          number of blocks the method needs to get before stopping
	 * @return Returns data block numbers
	 */
	private ArrayList<Integer> indirectPointers(int block, int level, int blockstoget) {
		// if the pointer points to a hole just give it zeros to avoid the recursion
		if (block == 0) {

			Integer[] stuff = new Integer[blockstoget];
			Arrays.fill(stuff, 0);

			return new ArrayList<>(Arrays.asList(stuff));
		}

		if (level == 1) {
			block *= Volume.blockSize;
			// hold the block numbers that point to data block with the file's contents
			ArrayList<Integer> pointers = new ArrayList<>();

			// you start reading at block and you offset by 4 every time
			// because each pointer is 4 bytes
			for (int i = 0; i < Volume.blockSize; i += 4) {

				// get the block number
				int blockNumber = volume.getIntAt(block + i);

				pointers.add(blockNumber);

				if ((--blockstoget) <= 0)
					break;
			}

			return pointers;
		}
		else {
			// get block numbers in this level
			ArrayList<Integer> thisLevel = indirectPointers(block, 1, 256);

			// this holds all the blocks numbers of the higher levels of indirections
			ArrayList<Integer> fromHigherLevel = new ArrayList<>();

			// you get the the block of pointers for each block number in thisLevel
			for (Iterator<Integer> i = thisLevel.iterator(); i.hasNext();) {
				int blockNumber = i.next();

				// calculate how many pointers to get from this indirect pointer
				int toget = (int) Math.pow(256, level - 1);
				toget = (int) Math.min(blockstoget, toget);

				// get the pointers in the block whose number is blockNumber
				ArrayList<Integer> bunch = indirectPointers(blockNumber, level - 1, toget);
				fromHigherLevel.addAll(bunch);

				blockstoget -= toget;
				if (blockstoget <= 0)
					break;
			}

			return fromHigherLevel;
		}
	}

	/**
	 * Returns a string of the inode's information in Unix like format
	 * 
	 * @param name
	 *          name of the file
	 * @return a string of the inode's information in Unix like format
	 */
	public String fileInfo(String name) {
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
		builder.append(" " + getI_uid());
		builder.append(" " + getI_gid());

		builder.append(" " + getI_size());
		builder.append(" " + new SimpleDateFormat("MMM dd HH:mm").format(getI_atime()));
		builder.append(" " + name);

		return builder.toString();
	}
	

	/**
	 * for demonstrating purposes. Gets the contents of the inode in bytes 
	 * @return
	 */
	byte[] getBytes() {
		return volume.getBytes(offset, Volume.blockSize);
	}
}
