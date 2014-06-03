
/**
 * This class deal with the OFT data structure. It keeps tracks of the buffer, along with the various data associated with the buffer.
 * @author Kevin
 *
 */
public class OFT {

	FileSystem fs;
	public static byte[][] OFT;
	public static int[] currentPosition; //Should be the absolute position with respect to length
	public static int[] index; // File descriptor index
	public static int[] length; //The length of the file 
	public static int[] blockNumber; //The current block number
	public static boolean[] firstWrite;
	
	public OFT(FileSystem fs) {
		this.fs = fs;
		OFT = new byte[FileSystem.NUMBER_OF_OFT_BUFFERS][IOSystem.SIZE_OF_BLOCK_IN_BYTES];
		currentPosition = new int[4];
		index = new int[4];
		length = new int[4];
		blockNumber = new int[4];
		firstWrite = new boolean[4];
		
		// Initialize all the values to -1
		for(int i = 0; i < currentPosition.length; i++){
			currentPosition[i] = -1;
			index[i] = -1;
			length[i] = -1;
			blockNumber[i] = -1;
			firstWrite[i] = true;
			for(int j = 0; j < OFT[1].length; j++){
				OFT[i][j] = -1;
			}
		}
	}
	
	/**
	 * Returns the index of a free OFT buffer
	 * @return the index of a free buffer
	 */
	public int allocateEntry() {
		for(int i = 0; i < index.length; i++){
			if(index[i] == -1) {
				return i;
			}
		}
		return -1; //No index was found
	}
	
	public void freeEntry(int i) {
		index[i] = -1;
		length[i] = -1;
		blockNumber[i] = -1;
		currentPosition[i] = -1;
		firstWrite[i] = true;
	}
	
	public void setCurrentPosition(int entry, int position) {
		currentPosition[entry] = position;
	}
	
	public int getCurrentPosition(int entry) {
		return currentPosition[entry];
	}
	
	
	public void setFileDescritorIndex(int entry, int fileDescritorIndex) {
		index[entry] = fileDescritorIndex;
	}
	
	public void setBuffer(int entry, byte[] mem) {
		OFT[entry] = mem;
	}

	public byte[] getBuffer(int entry) {
		return OFT[entry];
	}
	
	public int getLength(int entry) {
		return length[entry];
	}
	
	public void setLength(int entry, int size) {
		length[entry] = size;
	}
	
	public void setBlockNumber(int entry, int bn) {
		blockNumber[entry] = bn;
	}
	
	public char getChar(int entry) {
		return (char) OFT[entry][currentPosition[entry] % IOSystem.SIZE_OF_BLOCK_IN_BYTES];
	}
	
	public void writeChar(int entry, char c) {
		OFT[entry][currentPosition[entry] % IOSystem.SIZE_OF_BLOCK_IN_BYTES] = (byte) c; 
	}
	
	public void writeByteArray(int entry, int position, byte[] array) {
		for (int i = position; i < position + array.length; i++){
			OFT[entry][i] = array[i - position];
		}
	}
	
	/**
	 * 
	 * @param entry
	 * @return returns false if it's already at maximum blocks, otherwise returns true
	 * @throws Exception
	 */
	public boolean readNextBlockToBuffer(int entry) throws Exception{
		if(getActualBlockIndex(entry) == 3) {
			throw new Exception("Tried to read to block that doesn't exist");
		} else {
			FileDescriptor fd = FileDescriptor.getFileDescriptorInfo(index[entry]);
			int nextBlockIndex = getActualBlockIndex(entry); //We DONT offset by 1, because it's already on the next one, as we checked that it SHOULD read the next block
			int block = fd.blocks[nextBlockIndex];
			if(block == -1) {
				//System.out.println("Allocating for block number(0,1,2): " + nextBlockIndex);
				int newBlock = fs.allocateNewBlockForFile(index[entry], nextBlockIndex);
				//System.out.println("While reading new block, we allocated a new block at " + newBlock);
				readDiskToBuffer(entry,nextBlockIndex);
			} else { //This is a normal read, read in the next block
				readDiskToBuffer(entry,nextBlockIndex);
			}
				
		}
		return true;
	}
	
	/**
	 * 
	 * @param blockNumber 0, 1, or 2, corresponding to which one you want to grab
	 * @throws Exception 
	 */
	public void readBlockDirectory(int blockNumber) throws Exception {
		if(getActualBlockIndex(0) == 3) {
			throw new Exception("Tried to read to block that doesn't exist");
		} else {
			FileDescriptor fd = FileDescriptor.getFileDescriptorInfo(index[0]);
			int block = fd.blocks[blockNumber];
			if(block == -1) {
				//System.out.println("Allocating for block number(0,1,2): " + blockNumber);
				int newBlock = fs.allocateNewBlockForFile(index[0], blockNumber);
				//System.out.println("While reading new block, we allocated a new block at " + newBlock);
				readDiskToBuffer(0,blockNumber);
			} else { //This is a normal read, read in the next block
				readDiskToBuffer(0,blockNumber);
			}
		}
		
	}
	
	public boolean shouldReadNextBlock(int entry){
		FileDescriptor fd = FileDescriptor.getFileDescriptorInfo(index[entry]);
		int currentIndex = getActualBlockIndex(entry);
		int actualBlockIndex = fd.blocks[currentIndex];
		if(actualBlockIndex == blockNumber[entry])
			return false;
		else{
			return true;
		}
	}
	
	
	/**
	 * Returns the INDEX of the current block(0, 1, or 2)
	 * @param entry
	 * @return
	 */
	public int getActualBlockIndex(int entry){
		return currentPosition[entry] / IOSystem.SIZE_OF_BLOCK_IN_BYTES;
	}
	
	/**
	 * Block number is 0 through 2, aka the index of the block number in the FILE descriptor
	 * Writes the current block to disk, and then reads in the new block to memory
	 * The first "read" into disk should not produce a write, ever, because there's nothing in there 
	 * @param entry
	 * @param blockNumber
	 */
	public int readDiskToBuffer(int entry, int bn) {
		if(firstWrite[entry]) {
			firstWrite[entry] = false;
		} else{
			IOSystem.writeBlock(blockNumber[entry], OFT[entry]);
			writeLengthToDisk(entry,index[entry], length[entry]); //Updates the length field on disk
		}

		
		FileDescriptor fd = FileDescriptor.getFileDescriptorInfo(index[entry]);
		if(fd.blocks[bn] == -1) {
			return -1; //Block isn't assigned yet
		}
		byte[] mem = IOSystem.readBlock(fd.blocks[bn]);
		setBuffer(entry,mem);
		setBlockNumber(entry,fd.blocks[bn]);
		return 0;
	}
	
	private void writeLengthToDisk(int entry, int fileDescriptorNumber, int length ){
		int byteIndex = 1 * IOSystem.SIZE_OF_BLOCK_IN_BYTES + (fileDescriptorNumber * FileSystem.DESCRIPTOR_SIZE_IN_BYTES);
		int blockNumber = (int) Math.floor(byteIndex/IOSystem.SIZE_OF_BLOCK_IN_BYTES); //Figure out which block this byte is in, and read in that block
		int memIndex = byteIndex % IOSystem.SIZE_OF_BLOCK_IN_BYTES; //Beacuse we are reading in one block, we must offset the aboslute byte location by the read-in block's location
		byte[] mem = IOSystem.readBlock(blockNumber);
		FileSystem.pack(mem, length, memIndex);
		IOSystem.writeBlock(blockNumber, mem);
	}
	
	
	public void writeCurrentBufferToDisk(int entry) {
		IOSystem.writeBlock(blockNumber[entry], OFT[entry]);
		writeLengthToDisk(entry,index[entry], length[entry]); //Updates the length field on disk

	}
	/**
	 * Block number is 1 through 3, aka the index of the block number in the FILE descriptor
	 * @param entry
	 * @param blockNumber
	 */
	public void writeBufferToDisk(int entry, int blockNumber) {
		FileDescriptor fd = FileDescriptor.getFileDescriptorInfo(index[entry]);
		IOSystem.writeBlock(fd.blocks[blockNumber], OFT[entry]);
		byte[] mem = IOSystem.readBlock(fd.blocks[blockNumber]);

		setBuffer(entry,mem);
	}
}
