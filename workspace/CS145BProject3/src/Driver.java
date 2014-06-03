import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class Driver {

	FileSystem fs;
	FileWriter fw;
	BufferedWriter writer;
	File outputFile;
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		new Driver(args);
	}
	
	
	/**
	 * The main driver of the program. The driver calls methods from the FileSystem. It also reads in inputs from a file.
	 * Any error caught during the running of a FileSystem method will propogate up to here, and will be caught, resulting in an "error" to be printed out. 
	 * @param args
	 * @throws IOException
	 */
	public Driver(String[] args) throws IOException {
		
		outputFile = new File(args[1]);
		outputFile.createNewFile();
		fw = new FileWriter(outputFile.getAbsoluteFile());
		writer = new BufferedWriter(fw);

		fs = new FileSystem(writer);
		
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		String line = reader.readLine();
		while(line != null) {
			String[] split = line.split(" ");
			if(split[0].equals("cr")) {
				try {
					fs.create(split[1]);
				} catch(Exception e) {
					writer.write("error");
					writer.newLine();
				}
			} else if (split[0].equals("de")){
				try {
					fs.destroy(split[1]);
				} catch(Exception e) {
					writer.write("error");
					writer.newLine();
					}
			} else if (split[0].equals("op")){
				try {
					fs.open(split[1]);
				} catch(Exception e) {
					writer.write("error");
					writer.newLine();
					}
			}  else if (split[0].equals("cl")){
				try {
					fs.close(Integer.parseInt(split[1]));
				} catch(Exception e) {
					writer.write("error");
					writer.newLine();
					}
			} else if (split[0].equals("rd")){
				try {
					fs.read(Integer.parseInt(split[1]),Integer.parseInt(split[2]));
				} catch(Exception e) {
					writer.write("error");
					writer.newLine();
					}
			} else if (split[0].equals("wr")){
				try {
					fs.write(Integer.parseInt(split[1]), split[2].charAt(0), Integer.parseInt(split[3]));
				} catch(Exception e) {
					writer.write("error");
					writer.newLine();
					}
			}  else if (split[0].equals("sk")){
				try {
					fs.lseek(Integer.parseInt(split[1]),Integer.parseInt(split[2]));
				} catch(Exception e) {
					writer.write("error");
					writer.newLine();
					}
			} else if (split[0].equals("dr")){
				try {
					fs.listDirectory();
				} catch(Exception e) {
					writer.write("error");
					writer.newLine();
					}
			} else if (split[0].equals("in")){
				try {
					if(split.length == 1)
						fs.init("");
					else
						fs.init(split[1]);
				} catch(Exception e) {
					writer.write("error");
					writer.newLine();
					}
			} else if (split[0].equals("sv")){
				try {
					fs.save(split[1]);
				} catch(Exception e) {
					writer.write("error");
					writer.newLine();
					}
			} 
			line = reader.readLine();
		}
		writer.close();
	}
}
