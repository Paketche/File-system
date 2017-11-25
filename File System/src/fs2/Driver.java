package fs2;

import java.io.IOException;

public class Driver {

	public static void main(String[] args) {
		try {
			Volume.initVolume("ext2fs");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
