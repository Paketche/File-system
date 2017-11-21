package util;

public class __u8 extends UnsingedByteVariable {

	public __u8(byte i) {
		super(i & 0xFF);
	}

	public __u8() {
		super();
	}

	@Override
	void add(UnsingedByteVariable num) {
		setValue(value() + (num.value() & 0xFF));
	}

	@Override
	void subtract(UnsingedByteVariable num) {
		setValue(value() + num.value() & 0xFF);
	}

}
