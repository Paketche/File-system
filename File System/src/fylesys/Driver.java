package fylesys;

import java.io.IOException;

public class Driver {

	public static void main(String[] args) {
		try {
			new Volume("ext2fs");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
