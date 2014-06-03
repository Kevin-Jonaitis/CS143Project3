
public class IOSystem {

	
	//THERE ARE 4 bytes in an int
	//There is 1 byte in a char
	private static byte[] ldisk;
	public static final int NUMBER_OF_BLOCKS= 64;
	public static final int SIZE_OF_BLOCK_IN_BYTES = 64;
	public static final int NUMBER_OF_OFT_ENTRIES = 4;
	public static final int SIZE_OF_DESCRIPTOR_IN_BYTES = 4 * 4; //4 integers * 4 bytes per integer
	
	
	public IOSystem() {
		ldisk = new byte[NUMBER_OF_BLOCKS * SIZE_OF_BLOCK_IN_BYTES];
		for(int i = 1 * SIZE_OF_BLOCK_IN_BYTES; i < ldisk.length; i++) //Set everything to -1 except the first block
			ldisk[i] = -1;
	}
	
	/**
	 * Read entire block i
	 * @param i block number
	 * @return array of the block you are reading
	 */
	public static byte[] readBlock(int blockNumber) {
		byte[] toReturn = new byte[SIZE_OF_BLOCK_IN_BYTES];
		System.arraycopy(ldisk, SIZE_OF_BLOCK_IN_BYTES * blockNumber, toReturn, 0, SIZE_OF_BLOCK_IN_BYTES);
		return toReturn;
	}
	
	/**
	 * Write an entire block i
	 * @param i block number
	 * @param block array of characters you are writing
	 */
	public static void writeBlock(int blockNumber, byte[] block) {
		System.arraycopy(block, 0, ldisk, SIZE_OF_BLOCK_IN_BYTES * blockNumber, SIZE_OF_BLOCK_IN_BYTES);
	}

}
