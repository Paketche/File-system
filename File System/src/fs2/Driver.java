package fs2;

import java.io.IOException;

//import util.Utils;

public class Driver {

	public static void main(String[] args) {
		try {
			Volume vol = new Volume("ext2fs");
			Volume.traverse(vol.root, 0);
			//Utils.dumpHexRepresnetation(vol.root.fullyRead());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}