package fylesys;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LittleEndianBuffer {
	private ByteBuffer byteBuffer;
	// private byte[] byteArray;
	private int currentByte;
	private int blocksize;

	public LittleEndianBuffer(byte[] stream, int blockSize) throws IOException {

		byteBuffer = ByteBuffer.wrap(stream);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		this.blocksize = blockSize;
		currentByte = 0;
	}

	public byte[] read(int offest, int length) {
		byte[] temp = new byte[length];
		int lastpos = byteBuffer.position();

		byteBuffer.position(offest);
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

	public void reset() {
		currentByte = 0;
	}

	public void dumpHexRepresnetation(byte[] data) {
		
		for (int i = 0; i < data.length; i++) {
			byte[] word = new byte[16];
			for (int j = 0; j < 16 && i < data.length; j++,i++) {
				word[j] = data[i];
				System.out.format("%d %s", word[j], (j + 1) % 8 == 0 ? "| " : "");
			}
			for (int j = 0; j < word.length; j++) {
				System.out.format("%c %s", (char) word[j], (j + 1) % 8 == 0 ? "| " : "");

			}
			System.out.println();
		}
		System.out.println("---------------------------------------------------");	
	}
}
