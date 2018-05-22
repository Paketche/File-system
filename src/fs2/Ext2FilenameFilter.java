package fs2;

/**
 * Used to filter files
 * @author Georgi Valchanov
 *
 */
@FunctionalInterface
public interface Ext2FilenameFilter {

	/**
	 * Tests if a specified file should be included in a file list.
	 * @param str the name of the file.
	 * @return true if and only if the name should be included in the file list; false otherwise.
	 */
	boolean accept(String str);
}
