package fs2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import util.Utils;

public class LittleEndianBuffer {
	private ByteBuffer byteBuffer;
	// private byte[] byteArray;
	private int currentByte;
	private int blocksize;

	public LittleEndianBuffer(byte[] stream) throws IOException {

		byteBuffer = ByteBuffer.wrap(stream);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		this.blocksize = Volume.blockSize;
		currentByte = 0;
	}

	public LittleEndianBuffer(int capacity) {
		byteBuffer = ByteBuffer.allocate(capacity);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		this.blocksize = Volume.blockSize;
		currentByte = 0;
	}

	public void write(byte[] data) {
		byteBuffer.put(data);
	}

	public void flip() {
		byteBuffer.flip();
	}

	public void reset() {
		byteBuffer.reset();
		currentByte = 0;
	}

	public byte[] read(int offset, int length) {
		byte[] temp = new byte[length];
		int lastpos = byteBuffer.position();

		byteBuffer.position(offset);
		byteBuffer.get(temp);
		byteBuffer.position(lastpos);

		return temp;
	}

	public byte[] readBlock(int offset) {
		return read(offset, blocksize);
	}

	public byte[] readBlock() {
		byte[] temp = readBlock(currentByte);
		currentByte += blocksize;
		return temp;
	}

	public byte readByte() {
		if (currentByte == byteBuffer.capacity())
			return -1;
		return byteBuffer.get(currentByte++);
	}

	public byte readByte(int offset) {
		if (offset > byteBuffer.capacity()) {
			throw new IndexOutOfBoundsException();
		}
		return byteBuffer.get(offset);
	}

	public String readString(int offset, int length) {
		byte[] str = read(offset, length);
		String string = "";

		for (int i = 0; i < str.length; i++) {
			string += (char) (str[i] & 0xff);
		}

		return string;
	}

	public int readInt(int offset, int size) {
		return Utils.byteArrayToInt(read(offset, size));
	}

	public int capacity() {
		return byteBuffer.capacity();
	}

	public static void dumpHexRepresnetation(byte[] data) {
		
		for (int i = 0; i < data.length; i += 16) {

			for (int j = 0; j < 16 && i + j < data.length; j++) {
				System.out.format("%02x %s", data[j + i], (j + 1) % 8 == 0 ? "| " : " ");
			}

			for (int j = 0; j < 16 && i + j < data.length; j++) {
				if (data[j + i] > 32 && data[j + i] < 127)
					System.out.format("%c%s", (char) data[j + i], (j + 1) % 8 == 0 ? "| " : "");
				else
					System.out.print("_" + ((j + 1) % 8 == 0 ? "| " : ""));
			}

			System.out.println();
		}
		System.out.println("---------------------------------------------------");
	}
}
