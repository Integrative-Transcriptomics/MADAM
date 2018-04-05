/**
 * 
 */
package utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * @author Alexander Seitz
 *
 */
public class Utilities {
	
	public static BufferedReader getReader(String file){
		BufferedReader br = null;
		try {
			if(file.endsWith(".gz")){
				InputStream fileStream = new FileInputStream(file);
				InputStream gzipStream = new GZIPInputStream(fileStream);
				Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
				br = new BufferedReader(decoder);
			}else{
				br = new BufferedReader(new FileReader(new File(file)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return br;
	}
	
	public static void writeToFile(String s, File f){
		try {
			PrintWriter outMerged = new PrintWriter(new BufferedWriter(new FileWriter(f, false)));
			outMerged.println(s);
			outMerged.flush();
			outMerged.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void checkForInstalledPrograms(String[] programs){
		List<String> programsNotWorking = new LinkedList<String>();
		for(String prog: programs){
			String[] testProg=new String[]{prog};
			try{
				Process process = new ProcessBuilder(testProg).start();
				process.waitFor();
			}catch(IOException | InterruptedException e){
				programsNotWorking.add(prog);
				continue;
			}
		}
		if(programsNotWorking.size()>0){
			System.err.println("There are some programs that are not working");
			System.err.println("Please check the following programs:");
			for(String prog: programsNotWorking){
				System.err.println(prog);
			}
		}
		System.exit(1);
	}
	
	public static Boolean isInteger(String s){
		try{
			Integer.parseInt(s);
			return true;
		}catch(Exception e){
			return false;
		}
	}
	
	public static Boolean isDouble(String s){
		try{
			Double.parseDouble(s);
			return true;
		}catch(Exception e){
			return false;
		}
	}
	
	public static String removeTrailingSlashFromFolder(String name) {
		if (name.endsWith("/")) {
			name = name.substring(0, name.length() - 1);
		}
		return name;
	}
	
	public static void createOutFolder(String folder) {
		String[] createOutputFolder = { "mkdir", "-p", folder };
		try {
			Process process = new ProcessBuilder(createOutputFolder).start();
			process.waitFor();
		} catch (IOException | InterruptedException e) {
		}
	}
	
	public static void removeFile(String file) {
		String[] removeFile = {"sh", "-c", "rm "+file};
		try {
			new ProcessBuilder(removeFile).start();
		} catch (IOException e) {
		}
	}
	
	public static void removeFolder(String file) {
		String[] createOutputFolder = { "rm", "-r", file };
		try {
			Process process = new ProcessBuilder(createOutputFolder).start();
			process.waitFor();
		} catch (IOException | InterruptedException e) {
		}
	}
	
	public static void copyFile(String source, String dest) {
		String[] createOutputFolder = { "cp", source, dest };
		try {
			Process process = new ProcessBuilder(createOutputFolder).start();
			process.waitFor();
		} catch (IOException | InterruptedException e) {
		}
	}
	
	public static void moveFile(String src, String dest){
		String[] moveFile = { "mv", src, dest };
		try {
			Process process = new ProcessBuilder(moveFile).start();
			process.waitFor();
		} catch (IOException | InterruptedException e) {
		}
	}
	
	public static void zipFile(String source){
		String[] zipFile = {"gzip", source};
		try {
			Process process = new ProcessBuilder(zipFile).start();
			process.waitFor();
		} catch (IOException | InterruptedException e) {
		}
	}
	
	public static String checkFile(String file, FileType filetype){
		if(!Utilities.fileExists(file)){
			return "# ERROR: File does not exist:\n# "+file;
		}
		if(!Utilities.fileNotEmpty(file)){
			return "# Error: File is Empty:\n# "+file;
		}
		switch(filetype){
		case fasta:
			if(!Utilities.checkFastA(file)){
				return "# ERROR: Not a valid fasta file:\n# "+file;
			}
			break;
		case fastq:
			if(!Utilities.checkFastQ(file)){
				return "# ERROR: Not a valid fastq file:\n# "+file;
			}
			break;
		default:
			return "# ERROR: unknown filetype:\n# "+filetype;
		}
		return "";
	}
	
	public static boolean fileExists(String f){
		return new File(f).exists();
	}
	
	public static boolean fileNotEmpty(String f){
		return new File(f).length()>0;
	}
	
	
	public static boolean checkFastA(String file){
		boolean fileCorrect=true;
		try {
			BufferedReader bfr;
			bfr = getReader(file);
//			bfr = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String currLine = "";
			double line = 0.0;
			while(fileCorrect && (currLine=bfr.readLine()) != null){
				line++;
				if(currLine.startsWith(">")){
					continue;
				}else if(currLine.length()>0 && !currLine.matches("[ACGTN]+")){
					System.err.println("Error in fasta file: "+file);
					System.err.println("in line: "+line+": "+currLine);
					fileCorrect=false;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fileCorrect;
	}
	
	public static boolean checkFastQ(String file){
		try {
			BufferedReader bfr = getReader(file);
//			if(file.endsWith("gz")){
//				bfr = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
//			}else{
//				bfr = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
//			}
			String currLine = "";
			int i=0;
			String seq="";
			String qual="";
			boolean fileCorrect=true;
			while(fileCorrect && (currLine = bfr.readLine()) != null){
				switch(i%4){
				case 0: break;
				case 1: seq=currLine; break;
				case 2: break;
				case 3: qual=currLine;
						fileCorrect = fileCorrect && (qual.length() == seq.length());
				default: break;
				}
				i++;
			}
			return fileCorrect && (i%4==0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

}
