package fylesys;

public class DataBlock {
	
	private static long idCounter = 0;
	private long id;
	private byte[] data;
	
	public DataBlock(byte[] data) {
		id = idCounter++;
		this.data = data;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public long id() {
		return id;
	}
	
	public byte[] getPortion(int offset, int length) {
		byte[] temp = new byte[length];
		for (int i = 0; i < temp.length; i++) {
			temp[i] = getData()[offset + i];
		}
		return temp;
	}
}
