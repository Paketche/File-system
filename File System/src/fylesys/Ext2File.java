package fylesys;

import java.io.IOException;
import java.io.RandomAccessFile;

public class Ext2File {

	private RandomAccessFile file;
	private long possition;
	private LittleEndianBuffer buffer;

	public Ext2File(String name) throws IOException {
		file = new RandomAccessFile(name, "r");
		byte[] full = new byte[(int) file.length()];
		buffer = new LittleEndianBuffer(full, 1024);
		possition = 0;
	}

	public byte[] read(long startByte, long length) throws IOException {
		return buffer.read((int) startByte, (int) length);
	}

	public byte[] read(long length) throws IOException {
		byte[] temp = read(possition, length);
		possition += length;
		return temp;
	}

	public void seek(long possition) {
		this.possition = possition;
	}

	public long size() throws IOException {
		return file.length();
	}

	public byte[] fullyRead() throws IOException {
		return read(0, file.length());
	}

}
