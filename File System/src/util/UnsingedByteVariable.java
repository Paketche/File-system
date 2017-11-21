package util;

public abstract class UnsingedByteVariable {

	private long representation;

	protected UnsingedByteVariable(int i) {
		representation = i & 0xFFFFFFFFL;
	}

	protected UnsingedByteVariable() {
		representation = 0;
	}

	abstract void add(UnsingedByteVariable num);

	abstract void subtract(UnsingedByteVariable num);

	/**
	 * All implementations of this methos must return an __u32 value
	 * 
	 * @return
	 */
	public long value() {
		return representation;
	}

	protected void setValue(long num) {
		representation = num;
	}

	public void logicalRightShift(__u8 shifts) {
		setValue(value() >> shifts.value());
	}
}
