package util;

public class Utils {

	/**
	 * 
	 * @param hex
	 * @return
	 */
	public static byte[] HexStringTobyteArray(String hex) {
		byte[] byteArray = new byte[hex.length() - 2];

		for (int i = 2; i < hex.length(); i++) {
			byteArray[i - 2] = HexCharTobyte(hex.charAt(i));
		}

		return byteArray;
	}

	public static String byteArrayToHexString(byte[] num) {
		String hex = "";
		for (int i = 0; i < num.length; i++) {
			hex = hex + byteToHexChar(num[i]);
		}
		return hex;
	}

	public static String byteArrayToASCIIString(byte[] num) {
		String string = "";

		for (int i = 0; i < num.length; i++) {
			string += (char) (num[i] & 0xff);
		}

		return string;
	}

	public static byte HexCharTobyte(char hex) {

		if (hex >= '0' && hex <= '9') {
			return (byte) (hex - '0');
		}
		else if (hex >= 'A' && hex <= 'F') {
			return (byte) (hex - 'A' + 10);
		}
		else {
			return -1;
		}
	}

	public static char byteToHexChar(byte _byte) {
		if (_byte >= 0 && _byte <= 9) {
			return (char) ('0' + _byte);
		}
		else if (_byte >= 10 && _byte <= 15) {
			return (char) ('A' + _byte - 10);
		}
		return ' ';
	}

	public static int byteArrayToInt(byte[] num) {
		int number = num[0] & 0xff;
		for (int i = 1; i < num.length; i++) {
			number += (int) ((num[i] & 0xff) * Math.pow(2, 4 * (i + 1)));
		}
		return number;
	}

	public static byte[] intToByteArray(int num) {
		String hex = "0x" + Integer.toHexString(num);
		return HexStringTobyteArray(hex);
	}

	public static int ComparebyteArrays(byte[] num, byte[] num2) {
		byte[] bigger = (Math.max(num.length, num2.length) == num.length) ? num.clone() : num2.clone();
		byte[] smaller = (Math.max(num.length, num2.length) == num.length) ? num2.clone() : num.clone();

		for (int big = bigger.length - 1, small = smaller.length - 1; small >= 0; big--, small--) {
			byte temp = (byte) (bigger[big] - smaller[small]);
			if (temp != 0) {
				return temp / Math.abs(temp);
			}
		}

		return 0;
	}

	public static byte[] RigthLogicalShift(byte[] num, int shifts) {
		if (shifts == 0)
			return num;
		else {
			byte shifted = 0;
			for (int i = 0; i < num.length; i++) {
				byte toBeshifted = (byte) (num[i] & 0x1);
				num[i] = (byte) ((num[i] >> 1) + shifted * 8);
				shifted = toBeshifted;
			}

			return RigthLogicalShift(num, shifts - 1);
		}

	}

	public static byte[] LeftLogicalShift(byte[] num, int shifts) {
		if (shifts == 0)
			return num;
		else {
			byte shifted = 0;
			for (int i = num.length - 1; i >= 0; i--) {
				byte toBeshifted = (byte) (num[i] & 0x8);
				num[i] = (byte) ((num[i] << 1) + shifted);
				shifted = toBeshifted;
			}
			return LeftLogicalShift(num, shifts - 1);
		}
	}

	public static byte[] LogicalAND(byte[] num, byte[] num2) {
		byte[] bigger = (Math.max(num.length, num2.length) == num.length) ? num.clone() : num2.clone();
		byte[] smaller = (Math.max(num.length, num2.length) == num.length) ? num2.clone() : num.clone();

		for (int big = bigger.length - 1, small = smaller.length - 1; small >= 0; big--, small--) {
			bigger[big] = (byte) (bigger[big] & smaller[small]);
		}
		return bigger;
	}

	public static byte[] LogicalOR(byte[] num, byte[] num2) {
		byte[] bigger = (Math.max(num.length, num2.length) == num.length) ? num.clone() : num2.clone();
		byte[] smaller = (Math.max(num.length, num2.length) == num.length) ? num2.clone() : num.clone();

		for (int big = bigger.length - 1, small = smaller.length - 1; small >= 0; big--, small--) {
			bigger[big] = (byte) (bigger[big] | smaller[small]);
		}
		return bigger;
	}

	public static boolean isPowerOf(int a, int b) {
		while (a % b == 0) {
			a /= b;
		}
		return a == 1;
	}

	/**
	 * Prints out a byte array in a readable hexadecimal format
	 * @param data to be printed
	 */
	public static void dumpHexRepresnetation(byte[] data) {

		for (int i = 0; i < data.length; i += 16) {

			int j;
			for (j = 0; j < 16 && i + j < data.length; j++) {
				System.out.format("%02x %s", data[j + i], (j + 1) % 8 == 0 ? "| " : " ");
			}

			while (j < 16) {
				System.out.print("XX " + ((j + 1) % 8 == 0 ? "| " : " "));
				j++;
			}

			for (j = 0; j < 16 && i + j < data.length; j++) {
				if (data[j + i] > 32 && data[j + i] < 127)
					System.out.format("%c%s", (char) data[j + i], (j + 1) % 8 == 0 ? "| " : "");
				else
					System.out.print("_" + ((j + 1) % 8 == 0 ? "| " : ""));
			}
			
			while (j < 16) {
				System.out.print("X"+((j + 1) % 8 == 0 ? "| " : ""));
				j++;
			}


			System.out.println();
		}
		System.out.println("---------------------------------------------------");
	}

	public static void main(String[] args) {
		System.out.println(Integer.toHexString(1000));
		System.out.println(RigthLogicalShift(HexStringTobyteArray("0x1F"), 1));
	}
}
