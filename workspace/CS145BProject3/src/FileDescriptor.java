
public class FileDescriptor {
	int length;
	int blocks[];
	byte[] block;
	
	
	FileDescriptor(int length) {
		this.length = length;
		blocks = new int[3];
		block = new byte[IOSystem.SIZE_OF_BLOCK_IN_BYTES];
		for(int i = 0; i < blocks.length; i++)
			blocks[i] = -1;
	}
	
	/**
	 * Get a file descriptor based on a fileDescriptorNumber(this starts at 0, and increases by 1 for eacn 
	 * new file descriptor; and all the information associated with it
	 * @param fileDescriptorNumber
	 * @return
	 */
	public static FileDescriptor getFileDescriptorInfo(int fileDescriptorNumber) {
		FileDescriptor result;
		//Starting at block 1, find the block that contains the file descriptor
		int byteIndex = 1 * IOSystem.SIZE_OF_BLOCK_IN_BYTES + (fileDescriptorNumber * FileSystem.DESCRIPTOR_SIZE_IN_BYTES);
		int blockNumber = (int) Math.floor(byteIndex/IOSystem.SIZE_OF_BLOCK_IN_BYTES); //Figure out which block this byte is in, and read in that block
		int memIndex = byteIndex % IOSystem.SIZE_OF_BLOCK_IN_BYTES; //Beacuse we are reading in one block, we must offset the aboslute byte location by the read-in block's location
		byte[] mem = IOSystem.readBlock(blockNumber);
		int length = FileSystem.unpack(mem, memIndex);
		result = new FileDescriptor(length);
		int counter = 0;
	
		//Goes through the 3 integers representing the blocks in which this file are located
		for(int i = memIndex + FileSystem.SIZE_OF_INT_IN_BYTES; i <= memIndex + 3 * FileSystem.SIZE_OF_INT_IN_BYTES; i = i + FileSystem.SIZE_OF_INT_IN_BYTES) {
			result.blocks[counter] = FileSystem.unpack(mem, i);
			counter++;
		}
		return result;
	}
}
