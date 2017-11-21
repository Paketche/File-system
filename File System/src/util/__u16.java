package util;

public class __u16 extends UnsingedByteVariable {

	public __u16(short bite) {
		super(bite & 0xFFFF);
	}

	public __u16() {
		super();
	}

	@Override
	public void add(UnsingedByteVariable num) {
		setValue(value() + num.value() & 0xFFFF);
	}

	@Override
	public void subtract(UnsingedByteVariable num) {
		setValue(value() + num.value() & 0xFFFF);

	}
}
