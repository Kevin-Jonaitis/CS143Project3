import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class FileSystem {
	//Block 0 is for bitmap
	//Block 1 through 6 is for descriptors
	OFT oft;

	private IOSystem iosystem;
	BufferedWriter writer;
	
	
	//Constants used to clean up the code
	public static int DIRECTORY_ENTRY_SIZE_IN_BYTES = 4 * 2; //4 bytes per int, 2 ints per descriptor; filname name(4 characters), descriptor index(1 integer)
	public static int DESCRIPTOR_SIZE_IN_BYTES = 4 * 4; //4 integers  * 4 bytes; file length + 3 block #s 
	public static int NUMBER_OF_OFT_BUFFERS = 4;
	public static int SIZE_OF_BITMAP_IN_BYTES = 8;
	public static int SIZE_OF_BYTEMAP_IN_BYTES = 64;
	public static int NUMBER_OF_DESCRIPTOR_BLOCKS = 6;
	public static int NUMBER_OF_BYTEMAP_BLOCKS = 1;
	public static int SIZE_OF_INT_IN_BYTES = 4;
	public static int ZERO_INT = 0;
	public static int ONE_INT = 1;
	public static int NUMBER_OF_DECRIPTORS_PER_BLOCK = IOSystem.SIZE_OF_BLOCK_IN_BYTES/DESCRIPTOR_SIZE_IN_BYTES;
	public static int BLOCK_ONE_SIZE_IN_BYTES = 64;

	public FileSystem(BufferedWriter write) throws IOException {
		writer = write;
 
	}
	
	
//	public static void main(String[] args) throws Exception {
//		new FileSystem();
//	}
	
	
	FileSystem() throws Exception {
		FileWriter fw;
		File outputFile;
		outputFile = new File("output.txt");
		outputFile.createNewFile();
		fw = new FileWriter(outputFile.getAbsoluteFile());
		writer = new BufferedWriter(fw);
		
		init("");
		create("foo");
		open("foo");
		create("abc");
		open("abc");
		write(1,'x',60);
		write(1,'y',10);
		write(2,'t',300);
		read(1,10);
		lseek(1,66);
//		lseek(2,300);
		read(2,1);
		
		
		writer.close();
	}
	
	/**
	 * Creates a new file.
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public boolean create(String filename) throws Exception {
		
		//CHECK TO MAKE SURE FILE DOESN"T ALREADY EXIST
		if(checkIfFileExists(filename))
			throw new Exception("File already exists");
		
		int freeFileDescriptorIndex = findFreeFileDescriptor();
		int freeDirectoryEntry = findFreeDirectoryEntry();
		int freeBlock = findAndAllocateFreeBlock();
		
		//Fill in the directory entry 
		byte[] directoryEntry = new byte[DIRECTORY_ENTRY_SIZE_IN_BYTES];
		for(int i = 0; i < filename.length(); i++){
			directoryEntry[i] = (byte) filename.charAt(i);
		}
		pack(directoryEntry, freeFileDescriptorIndex, SIZE_OF_INT_IN_BYTES); // Start at position 4, because the first 4 are the filename
		oft.writeByteArray(0, freeDirectoryEntry, directoryEntry); //THIS CONTAINS THE ACTUAL DIRECTORY SIZE
		
		
		//Fill in the file descriptor index
		byte[] mem = IOSystem.readBlock(1 + (freeFileDescriptorIndex / NUMBER_OF_DECRIPTORS_PER_BLOCK)); // get the correct block #, offset by 1 block
		pack(mem, 0, (freeFileDescriptorIndex * DESCRIPTOR_SIZE_IN_BYTES) % IOSystem.SIZE_OF_BLOCK_IN_BYTES); //LENGTH
		pack(mem,freeBlock,(SIZE_OF_INT_IN_BYTES + (freeFileDescriptorIndex * DESCRIPTOR_SIZE_IN_BYTES)) % IOSystem.SIZE_OF_BLOCK_IN_BYTES); //INDEX BLOCK 0
		IOSystem.writeBlock(1 + (freeFileDescriptorIndex / NUMBER_OF_DECRIPTORS_PER_BLOCK), mem);
		
		writer.write(filename + " created");
		writer.newLine();
		
		return true;
		
	}
	
	/**
	 * Allocates another block for a file that is expanding
	 * @param fileDescriptorIndex
	 * @param blockPosition The block number(0,1, or 2)
	 * @param entry 
	 * @throws Exception 
	 * @return return's the new block number's allocation
	 */
	public int allocateNewBlockForFile(int fileDescriptorIndex, int blockPosition) throws Exception {
		int freeBlock = findAndAllocateFreeBlock();
		byte[] mem = IOSystem.readBlock(1 + (fileDescriptorIndex / NUMBER_OF_DECRIPTORS_PER_BLOCK)); // get the correct block #, offset by 1 block
		//pack(mem, 0, fileDescriptorIndex * DESCRIPTOR_SIZE_IN_BYTES); //LENGTH
		pack(mem, freeBlock, (SIZE_OF_INT_IN_BYTES + blockPosition * SIZE_OF_INT_IN_BYTES + (fileDescriptorIndex * DESCRIPTOR_SIZE_IN_BYTES)) % IOSystem.SIZE_OF_BLOCK_IN_BYTES); //INDEX BLOCK 0
		IOSystem.writeBlock(1 + (fileDescriptorIndex / NUMBER_OF_DECRIPTORS_PER_BLOCK), mem);

		return freeBlock;
	}
	
	
	/**
	 * Checks to see if a file being created already exists
	 * @param filename
	 * @return
	 */
	private boolean checkIfFileExists(String filename) {
		for(int i = 0; i < 3; i++){
			
			if(oft.readDiskToBuffer(0, i) == -1)
				return false;
			
			
			byte[] memory = oft.getBuffer(0);
			for(int j = 0; j < memory.length; j = j + DIRECTORY_ENTRY_SIZE_IN_BYTES){
				boolean foundMatch = true;
				int lengthCheck = 0;
				for(int k = 0; k < 4; k++){
					if (memory[j + k] != -1 && memory[j + k] != 0 )
						lengthCheck++;
				}
				if(lengthCheck != filename.length())
					continue;
				for(int k = 0; k < filename.length(); k++){
					if(((char) memory[j + k]) != (byte) filename.charAt(k))
						foundMatch = false;
				}
				if(foundMatch){
					return true;
				}
			}
		}
		 return false;
	}


	/**
	 * Searches through the file descriptors, looking for a free slot
	 * @return
	 * @throws Exception
	 */
	private int findFreeFileDescriptor() throws Exception {
		for(int i = 1; i < 7; i++){
			byte[] mem = IOSystem.readBlock(i);
			for(int j = 0; j < mem.length; j = j + DESCRIPTOR_SIZE_IN_BYTES) {
				if(mem[j] == -1) {
					int blockOffset = (i - 1) * NUMBER_OF_DECRIPTORS_PER_BLOCK;
					return j / DESCRIPTOR_SIZE_IN_BYTES + blockOffset;
				}
			}
		}
		throw new Exception("No free file descriptor");
	
	}

	/**
	 * Returns the actual index, from within the buffer, which has a free space to write a file descriptor.
	 * 
	 * @return THE ACTUAL J INDEX IN THE BUFFER FOR A FILE WRITE.
	 * @throws Exception 
	 */
	private int findFreeDirectoryEntry() throws Exception {
		//Check for free spaces first
		oft.readDiskToBuffer(0, 0);
		for(int i = 0; i < 3; i++){
			oft.readBlockDirectory(i);
			byte[] memory = oft.getBuffer(0);
			for(int j = 0; j < memory.length; j = j + DIRECTORY_ENTRY_SIZE_IN_BYTES){
				if(memory[j] == -1) {
					incrementAndAllocateDirectoryFileLength();
					return j;
				}	
			}
		}
		
		throw new Exception("Could not find a free directory entry");		
	}
	
	/**
	 * Increments the size of the directory file. If the file size has gone over the limit, we add another block.
	 * @throws Exception
	 */
	public void incrementAndAllocateDirectoryFileLength() throws Exception {
		byte[] block1 = IOSystem.readBlock(1);
		int length = unpack(block1,0);
		length = length + DIRECTORY_ENTRY_SIZE_IN_BYTES;
		if(length % IOSystem.SIZE_OF_BLOCK_IN_BYTES == 0) { //Allocate new block
			int newBlock = findAndAllocateFreeBlock();
			int offset = length / IOSystem.SIZE_OF_BLOCK_IN_BYTES;
			pack(block1, newBlock, BLOCK_ONE_SIZE_IN_BYTES + SIZE_OF_INT_IN_BYTES + offset * SIZE_OF_INT_IN_BYTES); //Offset by 1 block, place the new block based on the size of the file as of now.
		}
		pack(block1, length, 0);
		IOSystem.writeBlock(1, block1);
	}
	
	public void decrementDirectoryFileLength() {
		byte[] block1 = IOSystem.readBlock(1);
		int length = unpack(block1,0);
		length = length - DIRECTORY_ENTRY_SIZE_IN_BYTES;
		pack(block1, length, 0);
	}

	/** Search the bitmap for a free block
	 * 
	 * @return the int value corresponding to a free block
	 * @throws Exception 
	 */
	private int findAndAllocateFreeBlock() throws Exception {
		byte[] bitmap = IOSystem.readBlock(0);
		for(int i = 0; i < bitmap.length; i++) {
			if(bitmap[i] == 0){
				setBitOne(i);
				return i;
			}
		}
		
		throw new Exception("No free blocks available");
	}

	public void destroy(String filename) throws Exception {
		int fileDescriptorIndex = findIndexOfFileDescriptor(filename,true);
		for(int i = 0; i < oft.index.length; i++) {
			if (oft.index[i] == fileDescriptorIndex) {
				System.out.println("Closing before destroying: " + i);
				close(i);
			}
		}
				
		FileDescriptor fd = FileDescriptor.getFileDescriptorInfo(fileDescriptorIndex);
		
		//Clear bitmap
		for(int i = 0; i <fd.blocks.length; i++){
			if(fd.blocks[i] != -1){
				setBitZero(fd.blocks[i]);
			}
		}
		
		//Free file descriptor
		int byteIndex = 1 * IOSystem.SIZE_OF_BLOCK_IN_BYTES + (fileDescriptorIndex * FileSystem.DESCRIPTOR_SIZE_IN_BYTES);
		int blockNumber = (int) Math.floor(byteIndex/IOSystem.SIZE_OF_BLOCK_IN_BYTES); //Figure out which block this byte is in, and read in that block
		int memIndex = byteIndex % IOSystem.SIZE_OF_BLOCK_IN_BYTES; //Beacuse we are reading in one block, we must offset the aboslute byte location by the read-in block's location
		byte[] mem = IOSystem.readBlock(blockNumber);
		for(int i = memIndex; i < memIndex + 4 * FileSystem.SIZE_OF_INT_IN_BYTES; i++) {
			mem[i] = -1;
		}
		IOSystem.writeBlock(blockNumber, mem);
		
		writer.write(filename + " destroyed");
		writer.newLine();
	
	}
	
	/**
	 * Opens a new file
	 * @param filename
	 * @return OFT index
	 * @throws Exception 
	 */
	public int open(String filename) throws Exception {
		if(filename.equals("dir")) {
			int OFTIndex = oft.allocateEntry();
			
			oft.setFileDescritorIndex(OFTIndex, 0); //0 because we are looking at the first slot
			FileDescriptor fd = FileDescriptor.getFileDescriptorInfo(0);
			oft.setCurrentPosition(OFTIndex, 0);
			oft.setLength(OFTIndex,fd.length); //We are checking the length, because it could have been opened and closed before
			oft.setBlockNumber(OFTIndex, fd.blocks[0]); 

			oft.readDiskToBuffer(0, 0);
			return OFTIndex;
			
		} else {
			int fileDescriptorIndex = findIndexOfFileDescriptor(filename, false);
			int OFTIndex = oft.allocateEntry();
			
			oft.setFileDescritorIndex(OFTIndex, fileDescriptorIndex);
			FileDescriptor fd = FileDescriptor.getFileDescriptorInfo(fileDescriptorIndex);
			oft.setCurrentPosition(OFTIndex, 0);
			oft.setLength(OFTIndex,fd.length);
			oft.setBlockNumber(OFTIndex, fd.blocks[0]); //Initializing block number
			oft.readDiskToBuffer(OFTIndex, 0);

			writer.write(filename + " opened " + OFTIndex);
			writer.newLine();
			
			return OFTIndex;
			
		}
	}
	
	
	/**
	 * Searches the list of file descriptors for the specific filename. If the delete boolean is specified, it will also delete the file descriptor
	 * (delete is used in the detroy method)
	 * @param filename
	 * @param delete
	 * @return
	 * @throws Exception
	 */
	public int findIndexOfFileDescriptor(String filename, boolean delete) throws Exception {
			for(int i = 0; i < 3; i++){
				
				if(oft.readDiskToBuffer(0, i) == -1)
					throw new Exception("Block not assigned");
				
				byte[] memory = oft.getBuffer(0);
				for(int j = 0; j < memory.length; j = j + DIRECTORY_ENTRY_SIZE_IN_BYTES){
					boolean foundMatch = true;
					for(int k = 0; k < filename.length(); k++){
						if(((char) memory[j + k]) != (byte) filename.charAt(k))
							foundMatch = false;
					}
					if(foundMatch){
						int returnValue = unpack(memory, j + SIZE_OF_INT_IN_BYTES);
						if(delete) {
							for(int m = 0; m < 8; m++){
								memory[j + m] = -1;
								oft.writeByteArray(0, 0, memory);
							}
						}
						return returnValue; //Return the 4th element, or the file descriptor index
					}
				}
			}
			throw new Exception("Could not find file descriptor index for a file that SHOULD exist");
	}
	
	public void close(int index) throws IOException {
		oft.writeCurrentBufferToDisk(index); //writes buffer and length to disk
		oft.freeEntry(index);
		writer.write(index + " closed");
		writer.newLine();
	}
	
	/**
	 * Read method called from driver
	 * @param index
	 * @param memArea
	 * @param count
	 * @return bytes read
	 * @throws Exception 
	 */
	public char[] read(int index, int count) throws Exception {
		char[] returnValue;
		
		//Prevent overflow
		if(oft.getCurrentPosition(index) + count > oft.getLength(index))
			returnValue = new char[oft.getLength(index) - oft.getCurrentPosition(index)];
		else
			returnValue = new char[count];
		int counter = 0;

		while(counter < count) {
			if(oft.getCurrentPosition(index) >= oft.getLength(index)) {
				//end of file reached
				break;
			}
			
			if(oft.shouldReadNextBlock(index)) { //We've reached the beginning of the next block
				//writer.write("Moving to the next block");
				oft.readNextBlockToBuffer(index);
			}
			
			returnValue[counter] = oft.getChar(index);
			oft.setCurrentPosition(index, oft.getCurrentPosition(index) + 1); //increment current position
			counter++;
		}
		if(counter == 0)
			return returnValue;
		writer.write(returnValue);
		writer.newLine();
		return returnValue;
	}
	
	/**
	 * The write method called from driver
	 * @param index
	 * @param memArea
	 * @param count
	 * @return number of bytes written
	 * @throws Exception 
	 */
	public int write(int index, char c, int count) throws Exception {
		int counter = 0;
		while(counter < count) {
			
			//End of file reached
			if(oft.getCurrentPosition(index) >= IOSystem.SIZE_OF_BLOCK_IN_BYTES * 3) { //If the pointer is the size of 3 blocks, we're done.
				break;
			}
		
			if(oft.shouldReadNextBlock(index)) { //We've reached the beginning of the next block
				//writer.write("Moving to the next block");
				oft.readNextBlockToBuffer(index);
			}	
			
			oft.writeChar(index, c);
			int newPosition = oft.getCurrentPosition(index) + 1;
			oft.setCurrentPosition(index, newPosition); //increment current position
			if(newPosition > oft.getLength(index))
				oft.setLength(index, newPosition);
			counter++;	
		}
		writer.write(counter + " bytes written");
		writer.newLine();
		return counter;
	}
	
	
	public void lseek(int index, int position) throws Exception {
		if(position > oft.length[index] || position < 0)
			throw new Exception("Position out of rage");
		int block = (int) Math.floor(position / IOSystem.SIZE_OF_BLOCK_IN_BYTES);
		oft.readDiskToBuffer(index, block);
		oft.setCurrentPosition(index, position);
		writer.write("position is " + position);
		writer.newLine();
	}
	
	/**
	 * Should system.out a list of directories
	 * @throws IOException 
	 */
	public void listDirectory() throws IOException {
		String directory = "";
		
		for(int i = 0; i < 3; i++){ // 3 is to iterate through all the directories 
			
			if(oft.readDiskToBuffer(0, i) == -1)
				break;
			
			byte[] memory = oft.getBuffer(0);
			for(int j = 0; j < memory.length; j = j + DIRECTORY_ENTRY_SIZE_IN_BYTES){
				String name = "";
				for(int k = 0; k < 4; k++){ //4 = max length of word
					//writer.write(k);
					if(memory[j + k] != -1 && memory[j + k] != 0)
						name = name + (char) memory[j + k];
				}
				if(name != "")
					directory = directory + " " + name;

			}
		}
		
		if(directory.length() > 0)
			writer.write(directory.substring(1));
		else
			writer.write("");
		
		writer.newLine();
		
		
	}
	
	/**
	 * Restore ldisk from file.txt or create new (if no file)
	 * @param filename
	 * @throws Exception 
	 */
	public void init(String filename) throws Exception {
		try {
		iosystem = new IOSystem();
		oft = new OFT(this);		
		
		if(filename.equals("")) {
			initializeBitmap();
			initalizeDirectoryDescriptor();
			open("dir");

			writer.write("disk initialized");
			writer.newLine();
		} else {
			readInFile(filename);
			openExistingDirectory();
		}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
	}

	
	/**
	 * Used to reopen a directory from a file that is being re-initalized
	 */
	private void openExistingDirectory() {
		int OFTIndex = 0;
		byte[] test = IOSystem.readBlock(7);
		oft.setFileDescritorIndex(OFTIndex, 0); //0 because we are looking at the first slot
		FileDescriptor fd = FileDescriptor.getFileDescriptorInfo(0);
		int testing = fd.blocks[0];
		oft.setCurrentPosition(OFTIndex, 0);
		oft.setLength(OFTIndex,fd.length); //We are checking the length, because it could have been opened and closed before
		oft.setBlockNumber(OFTIndex, fd.blocks[0]); 

		byte[] mem = IOSystem.readBlock(fd.blocks[0]);
		oft.setBuffer(0,mem);
		oft.setBlockNumber(0,fd.blocks[0]);
		
	}

	/**
	 * Save ldisk to file.txt
	 * @param filename
	 * @throws IOException 
	 */
	public void save(String filename) throws IOException {
		oft.writeCurrentBufferToDisk(0);
		
		File file = new File(filename);
		file.createNewFile();
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);

		for(int i = 0; i < IOSystem.NUMBER_OF_BLOCKS; i++){
			byte[] block = IOSystem.readBlock(i);
			String s = new String(block);
			bw.write(s);
		}
		bw.close();
		
		writer.write("disk saved");
		writer.newLine();
		//WRITE OUT ALL CURRENT THINGS IN THE BUFFER
		
	}
	
	
	private void readInFile(String filename) throws IOException {
		File file = new File(filename);
		if (!file.exists()) {
			file.createNewFile();
			writer.write("disk initialized");
			writer.newLine();
		} else {
			FileReader fr = new FileReader(file.getAbsoluteFile());
			BufferedReader br = new BufferedReader(fr);
			char[] block = new char[IOSystem.SIZE_OF_BLOCK_IN_BYTES];
			
			for(int i = 0; i < IOSystem.NUMBER_OF_BLOCKS; i++){
				br.read(block);
				byte[] block_bytes = new String(block).getBytes();
				IOSystem.writeBlock(i, block_bytes);
			}
			writer.write("disk restored");
			writer.newLine();
		}
		
	}
	

	private void initializeBitmap() throws Exception {
		//Set the first 7 bits of the bitmap(block 0 for bitmap, blocks 1-6 for file descriptors)
		for(int i = 0; i < NUMBER_OF_BYTEMAP_BLOCKS + NUMBER_OF_DESCRIPTOR_BLOCKS; i++)
			setBitOne(i);
		
	}
	


	private void initalizeDirectoryDescriptor() throws Exception {
		int zero = 0;
		byte[] block1 = IOSystem.readBlock(1);
		pack(block1,0,0);
		
		int freeBlock = findAndAllocateFreeBlock();
		//writer.write("Free block for directory found at block " + freeBlock);
		pack(block1,freeBlock,0 + SIZE_OF_INT_IN_BYTES);
		IOSystem.writeBlock(1, block1);
		
	}
	
	private void setBitOne(int i) throws Exception {
		if(i > 64 || i < 0)
			throw new Exception("OUT OF BOUNDS FOR ZERO BIT SET");
		byte[] bitmap = IOSystem.readBlock(0);
		bitmap[i] = 1;
		IOSystem.writeBlock(0, bitmap);
	}
	private void setBitZero(int i) throws Exception {
		if(i > 64 || i < 0)
			throw new Exception("OUT OF BOUNDS FOR ZERO BIT SET");
		byte[] bitmap = IOSystem.readBlock(0);
		bitmap[i] = 0;
		IOSystem.writeBlock(0, bitmap);
	}

	private int serachForZeroBit() {
		byte[] bitmap = IOSystem.readBlock(0);
		for (int i = 0; i < bitmap.length; i++) {
			if (bitmap[i] == 0)
				return i;
		}
		return -1; // Could not find a 0
	}

	// Pack the 4-byte integer val into the four bytes mem[loc]...mem[loc+3].
	// The most significant porion of the integer is stored in mem[loc].
	// Bytes are masked out of the integer and stored in the array, working
	// from right(least significant) to left (most significant).
	public static void pack(byte[] mem, int val, int loc) {
		final int MASK = 0xff;
		for (int i = 3; i >= 0; i--) {
			mem[loc + i] = (byte) (val & MASK);
			val = val >> 8;
		}
	}

	// Unpack the four bytes mem[loc]...mem[loc+3] into a 4-byte integer,
	// and return the resulting integer value.
	// The most significant porion of the integer is stored in mem[loc].
	// Bytes are 'OR'ed into the integer, working from left (most significant)
	// to right (least significant)
	public static int unpack(byte[] mem, int loc) {
		final int MASK = 0xff;
		int v = (int) mem[loc] & MASK;
		for (int i = 1; i < 4; i++) {
			v = v << 8;
			v = v | ((int) mem[loc + i] & MASK);
		}
		return v;
	}
}