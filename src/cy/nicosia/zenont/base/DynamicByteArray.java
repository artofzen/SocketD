package cy.nicosia.zenont.base;

public class DynamicByteArray {

	private byte[] _array;
	private int _dataLength;

	public DynamicByteArray() {
		this(0);
	}

	public DynamicByteArray(int length) {
		_array = new byte[length];
		_dataLength = length;
	}

	public int find(byte[] chars) {
		return ByteArrayUtils.findBytes(_array, _dataLength, chars);
	}

	public void concatenate(final byte[] secondary, int secondaryDataLength) {

		assert (secondary != null);

		if (secondaryDataLength <= 0)
			return;
		
		if (_array.length >= _dataLength + secondaryDataLength) {
			for(int i = 0; i < secondaryDataLength; i++)
				_array[_dataLength + i] = secondary[i];
			_dataLength += secondaryDataLength;
		}
		else {
			_array = ByteArrayUtils.concatenateByteArrays(_array, _dataLength, secondary, secondaryDataLength);
			_dataLength = _array.length;
		}

	}

	public void shift(int index) {
		_dataLength -= index;

		for (int i = 0, j = index; i < _dataLength; i++, j++)
			_array[i] = _array[j];

	}

	public byte[] getArray() {
		return _array;
	}

	public int length() {
		return _dataLength;
	}

	public static abstract class ByteArrayUtils {

		public static int findBytes(final byte[] buffer, int dataLength, byte[] chars) {
			int bufferByte = 0;
			int charByte = 0;
			while ((bufferByte + (chars.length - 1) - charByte) < dataLength) {
				//byte in buffer matches byte in char sequence
				if (buffer[bufferByte] == chars[charByte]) {
					//Are we at end of character sequence
					if (charByte < chars.length - 1)
						//No, so increment both buffers and continue
						charByte++;
					else {
						//Yes so return our findings
						return bufferByte + 1;
					}
				} else {
					//Reset to beginning
					charByte = 0;
				}

				bufferByte++;
			}
			return -1;
		}

		public static byte[] concatenateByteArrays(final byte[] first, int firstDataLength, 
				final byte[] second, int secondDataLength) {

			assert (first != null || second != null);

			byte[] tempBuffer = new byte[ firstDataLength + secondDataLength];

			for (int i = 0; i < firstDataLength; i++)
				tempBuffer[i] = first[i];

			for (int i = 0; i < secondDataLength; i++)
				tempBuffer[firstDataLength + i] = second[i];

			return tempBuffer;
		}

		public static byte[] extractBytesFromByteArray(final byte[] buffer, int indexStart, int dataLength) {
			byte[] tempBuffer = new byte[dataLength - indexStart]; 
			for (int i = 0, j = indexStart; i < dataLength - indexStart; i++, j++)
				tempBuffer[i] = buffer[j];

			return tempBuffer;
		}
	}
}
