/**
 * 
 */
package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import utilities.MyException;
import utilities.Tools;

/**
 * @author Alexander Seitz
 *
 */
public class Madam { 

	public static final String CLASS_NAME = "Madam";
	private static final String VERSION = "1.0";
	private static final String DESC = "Tools for Assembly";
	private static final String DEFAULTCASE = "defaultCase";

	public Madam(String[] args) throws IOException{
		if(!(args.length > 0)){
			printHelp();
			System.exit(1);
		}else if(args.length == 1 && new File(args[0]).exists() && new File(args[0]).isFile()){
			File configFile = new File(args[0]); 
			if(new File(args[0]).exists()){
				runAssemblytoolsFromFile(configFile);
			}else{
				try {
					runAssemblytools(args);
				} catch (MyException e) {
					checkException(e);
				}
			}
		}else{
			try {
				runAssemblytools(args);
			} catch (MyException e) {
				checkException(e);
			}
		}
		System.out.println("Finished.");
	}

	private void checkException(MyException e){
		if(e.getMessage().equals(Madam.DEFAULTCASE)){
			printHelp();
			System.exit(1);
		}else{
			System.err.println(e.getMessage());
			printHelp();
			System.exit(1);
		}
	}


	/**
	 * @param configFile
	 */
	private void runAssemblytoolsFromFile(File configFile) throws IOException {
		@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader(new FileReader(configFile));
		String line = "";
		List<String[]> runs = new LinkedList<String[]>();
		while((line = br.readLine()) != null){
			if(line.trim().length()>0){
				String[] command = line.split(" ");
				runs.add(command);
			}
		}
		if(runs.size()==0){
			System.err.println("config file did not contain any run commands");
			printHelp();
			System.exit(1);
		}
		List<String[]> errors = new LinkedList<String[]>();
		List<MyException> exceptions = new LinkedList<MyException>();
		for(String[] command: runs){
			try {
				runAssemblytools(command);
			} catch (MyException e) {
				if(!e.getMessage().equals(Madam.DEFAULTCASE)){
					errors.add(command);
					exceptions.add(e);
				}else{
					// TODO
				}
			}
		}
		if(errors.size()>0){
			int i=0;
			for(String[] command: errors){
				System.err.println("command in config file failed with exception:\n");
				System.err.println(exceptions.get(i));
				System.err.println(Arrays.asList(command));
			}
			System.exit(1);
		}
	}


	/**
	 * @param args
	 * @throws Exception 
	 */
	private void runAssemblytools(String[] args) throws MyException {
		String command = args[0];
		String[] newCommands = Arrays.copyOfRange(args, 1, args.length);
		Tools toolName = Tools.statistics;
		try{
			toolName = Tools.valueOf(command);
		}catch (Exception e){
			throw new MyException("Command not recognized: \n"+command);
		}
		// instantiate the tool
		try {
			Class<?> cl = Class.forName(toolName.getConstructorName());
			Constructor<?> cons = cl.getConstructor(String[].class);
			cons.newInstance(new Object[]{newCommands});
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.exit(0);
//		switch(toolName){
//		case statistics: new Statistics(newCommands);
//		break;
//		case assembly: new Assembly(newCommands);
//		break;
//		case cm: new ClipAndMerge(newCommands);
//		break;
//		case filter: new Filter(newCommands);
//		break;
//		case merge: new Merge(newCommands);
//		break;
//		case pipeline: new AncientBacterialPipeline(newCommands);//new AncientBacteriaPipeline(newCommands);
//		break;
//		case mapping: new Mapping(newCommands);
//		break;
//		default:
//			throw new MyException(Assemblytools.DEFAULTCASE);
//		}
	}


	private void printHelp(){
		StringBuffer result = new StringBuffer();
		result.append("\n");
		result.append("Program: ");
		result.append(CLASS_NAME);
		result.append(" (");
		result.append(DESC);
		result.append(")");
		result.append("\n");
		result.append("Version: ");
		result.append(VERSION);
		result.append("\n\n");
		result.append("Usage:\t");
		result.append(CLASS_NAME);
		result.append(" <command> [options]\n\n");
		result.append("Commands:\n");
		for(Tools t: Tools.values()){
			result.append(" ");
			result.append(t.name());
			result.append("\t");
			result.append(t.toString());
			result.append("\n");
		}
		System.err.println(result.toString());
	}


	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		new Madam(args);
	}

}