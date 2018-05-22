import java.io.IOException;
import java.util.regex.*;
import fs2.Ext2File;
import fs2.Volume;
import util.Utils;
import java.nio.file.*;

public class Driver {


	public static void main(String[] args) {
		try {
			Volume vol = new Volume("ext2fs");
			Ext2File dirs = new Ext2File(vol, "/files/dir-e");
			
			int size = (int) dirs.size();
			int chunk = 11;
			
			dirs.seek(size - chunk);
			System.out.print(new String(dirs.read(chunk)));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}