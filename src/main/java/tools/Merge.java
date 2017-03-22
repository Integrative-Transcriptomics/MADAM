package tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.ParseException;

import main.Madam;
import pipelines.APipeline;
import pipelines.Inputs;
import utilities.FileType;
import utilities.Tools;
import utilities.Utilities;

public class Merge extends ATool {

	private static final String CLASS_NAME = Madam.CLASS_NAME + " merge [options]";

	private Integer threads = 1;
	//	private String outFolder = "";
	private String[] inputContigs = new String[0];
	private String mergedFile = "merged.fasta";
//	private Set<Integer> algos = new HashSet<Integer>(Arrays.asList(1));
	private List<String> contigFiles = new LinkedList<String>();
	private List<String> contigFileNames = new LinkedList<String>();
	private String sgaFile = "";

	private String pipelineFolderName = "Merge";

	public Merge(){
		super(Tools.merge);
	}

	@SuppressWarnings("static-access")
	public Merge(String[] args){
		super(Tools.merge);
		// create command line options
		//		Options helpOptions = new Options();
		//		helpOptions.addOption("h", "help", false, "show this help page");
		//		Options options = new Options();
		options.addOption("h", "help", false, "show this help page");
		options.addOption("t", "threads", true, "number of threads to use ["+this.threads+"]");
		options.addOption("f", "file", true, "name of the merged output file ["+this.mergedFile+"]");
//		options.addOption(OptionBuilder.withLongOpt("algos")
//				.withArgName("ALGO")
//				.withDescription("The merge algorithm(s) to use: 1: SGA, 2: VELVET "+this.algos.toString())
//				.hasArg()
//				.hasOptionalArgs()
//				.create("a"));
		options.addOption(OptionBuilder.withLongOpt("input")
				.withArgName("INPUT")
				.withDescription("the input File(s)")
				.isRequired()
				.hasArg()
				.hasOptionalArgs()
				.create("i"));
		options.addOption(OptionBuilder.withLongOpt("output")
				.withArgName("OUTPUT")
				.withDescription("the output Directory")
				.isRequired()
				.hasArg()
				.create("o"));

		HelpFormatter helpformatter = new HelpFormatter();

		CommandLineParser parser = new BasicParser();

		try {
			CommandLine cmd = parser.parse(helpOptions, args);
			if (cmd.hasOption('h')) {
				helpformatter.printHelp(CLASS_NAME, options);
				System.exit(0);
			}
		} catch (ParseException e1) {
		}

		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
			cmd = parser.parse(options, args);

			inputContigs = cmd.getOptionValues("i");
			this.workingDir = cmd.getOptionValue("o");
			this.workingDir = new File(this.workingDir).getAbsolutePath();
			this.workingDir = Utilities.removeTrailingSlashFromFolder(this.workingDir);
			this.sgaFile = this.workingDir + "/SGA_"+this.mergedFile;
			if(cmd.hasOption("t")){
				String t = cmd.getOptionValue("t");
				if(Utilities.isInteger(t)){
					this.threads = Integer.parseInt(t);
				}else{
					System.err.println("Provided threads not an Integer: "+t);
					helpformatter.printHelp(CLASS_NAME, options);
					System.exit(0);
				}
			}
//			if(cmd.hasOption("a")){
//				String[] a = cmd.getOptionValues("a");
//				this.algos.clear();
//				for(String alg: a){
//					if(Utilities.isInteger(alg)){
//						Integer algo = Integer.parseInt(alg);
//						if(!(algo == 1 || algo == 2)){
//							System.err.println("Provided merge Algorithm not recognized: "+algo);
//							helpformatter.printHelp(CLASS_NAME, options);
//							System.exit(0);
//						}else{
//							algos.add(algo);
//						}
//					}
//				}
//				if(this.algos.size() < 1){
//					System.err.println("Please give at least one viable merge algorithm");
//					helpformatter.printHelp(CLASS_NAME, options);
//					System.exit(0);
//				}
//			}
			if(cmd.hasOption("f")){
				this.mergedFile = cmd.getOptionValue("f");
			}
		} catch (ParseException e) {
			helpformatter.printHelp(CLASS_NAME, options);
			System.err.println(e.getMessage());
			System.exit(0);
		}
		run();
	}

	public void run() {
		writeExecutionLog(new String[]{"# Merging the contig files"});
		this.workingDir = Utilities.removeTrailingSlashFromFolder(this.workingDir);
		File f = new File(this.workingDir);
		this.workingDir = f.getAbsolutePath();
		Utilities.createOutFolder(this.workingDir);
		String tmpFolder = this.workingDir + "/" + "tmp";
		String pref = "tmp_mer";
		//		String velvetFile = this.workingDir + "/VELVET_"+this.mergedFile;
		String mergedContigs = this.workingDir+"/merged_contigs.fasta"; 
		mergeInputContigs(mergedContigs);
		Utilities.zipFile(mergedContigs);
		mergedContigs += ".gz";
		writeExecutionLog(new String[]{"mkdir", tmpFolder});
		Utilities.createOutFolder(tmpFolder);
		if(!new File(sgaFile).exists()){
			writeExecutionLog(new String[]{"# WARNING: making sure the merged contig file exists:"});
			writeExecutionLog(new String[]{"if [ ! -f "+mergedContigs+" ]; then"});
			writeExecutionLog(new String[]{"  echo 'ERROR: merged File does not exist'"});
			writeExecutionLog(new String[]{"  exit"});///TODO call to program
			writeExecutionLog(new String[]{"else"});
			mergeSGA(tmpFolder, mergedContigs, pref);
			writeExecutionLog(new String[]{"fi"});
			Utilities.copyFile(tmpFolder+"/"+pref+"-contigs.fa", sgaFile);
		}
		writeExecutionLog(new String[]{"rm", "-r", tmpFolder});
		Utilities.removeFolder(tmpFolder);
		//		Utilities.zipFile(mergedContigs);
	}

	private void mergeSGA(String mergeFolder, String mergedContigs, String prefix) {
		String index = "index";
		String rmdup = "reads.rmdup.fa";
		String fmMerge = "reads.rmdup.fm.fa";
		String[] runIndex = {"sga", "index", "-t", ""+this.threads, "-p", index, mergedContigs};
		String[] runRmdup = {"sga", "rmdup", "-t", ""+this.threads, "-p", index, "-o", rmdup, mergedContigs};
		String[] runFmMerge = {"sga", "fm-merge", "-t", ""+this.threads, "-o", fmMerge, rmdup};
		String[] runFMIndex = {"sga", "index", "-t", ""+this.threads, fmMerge};
		String[] runOverlap = {"sga", "overlap", "-t", ""+this.threads, fmMerge};
		String[] runAssemble = {"sga", "assemble", "-o", prefix, fmMerge.replace(".fa", ".asqg.gz")};
		Process process;
		try {
			String currentDir = System.getProperty("user.dir");
			writeExecutionLog(new String[]{"cd", mergeFolder});
			writeExecutionLog(runIndex);
			process = new ProcessBuilder(runIndex).directory(new File(mergeFolder)).redirectError(new File(this.workingDir+"/sgaLog.log")).start();
			process.waitFor();
			writeExecutionLog(runRmdup);
			process = new ProcessBuilder(runRmdup).directory(new File(mergeFolder)).redirectError(new File(this.workingDir+"/sgaLog.log")).start();
			process.waitFor();
			writeExecutionLog(runFmMerge);
			process = new ProcessBuilder(runFmMerge).directory(new File(mergeFolder)).redirectError(new File(this.workingDir+"/sgaLog.log")).start();
			process.waitFor();
			writeExecutionLog(runFMIndex);
			process = new ProcessBuilder(runFMIndex).directory(new File(mergeFolder)).redirectError(new File(this.workingDir+"/sgaLog.log")).start();
			process.waitFor();
			writeExecutionLog(runOverlap);
			process = new ProcessBuilder(runOverlap).directory(new File(mergeFolder)).redirectError(new File(this.workingDir+"/sgaLog.log")).start();
			process.waitFor();
			writeExecutionLog(runAssemble);
			process = new ProcessBuilder(runAssemble).directory(new File(mergeFolder)).redirectError(new File(this.workingDir+"/sgaLog.log")).start();
			process.waitFor();
			writeExecutionLog(new String[]{"cd", currentDir});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	//	private void mergeVelvet(String folder, String mergedContigs) {
	//		String[] hashAssemblies = {"sh", "-c", "velveth "+ folder+ " 23 -long "+ mergedContigs};
	//		String[] mergeAssemblies = {"velvetg", folder, "-read_trkg", "yes", "-conserveLong", "yes", "-very_clean", "yes"};
	//		Process process;
	//		try {
	//			ProcessBuilder pbHash = new ProcessBuilder(hashAssemblies);
	//			pbHash.environment().put("OMP_NUM_THREADS", this.threads.toString());
	//			process = pbHash.start();
	//			process.waitFor();
	//			ProcessBuilder pbAssemble = new ProcessBuilder(mergeAssemblies);
	//			pbAssemble.environment().put("OMP_NUM_THREADS", this.threads.toString());
	//			process = pbAssemble.start();
	//			process.waitFor();
	//		} catch (Exception e) {
	//			e.printStackTrace();
	//		}
	//	}

	private void mergeInputContigs(String mergedContigs) {
		if(new File(mergedContigs).exists()){
			return;
		}
		try {
			PrintWriter outMerged = new PrintWriter(new BufferedWriter(new FileWriter(mergedContigs, false)));
			Integer number = 1;
			for(String file: this.inputContigs){
				@SuppressWarnings("resource")
				BufferedReader br= new BufferedReader(new FileReader(file));
				String currLine = "";
				while((currLine = br.readLine()) != null){
					if(currLine.startsWith(">")){
						outMerged.flush();
						String changedHeader = currLine.replaceFirst(">", ">"+number+"_");
						outMerged.println(changedHeader);
					}else{
						outMerged.println(currLine);
					}
				}
				outMerged.flush();
				number++;
			}
			outMerged.flush();
			outMerged.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<String> getContigFiles(){
		return this.contigFiles;
	}

	public List<String> getContigFileNames(){
		return this.contigFileNames;
	}

	@Override
	public Tools getName() {
		return Tools.merge;
	}

	@Override
	public List<Inputs> needsInputs() {
		List<Inputs> inputs = new LinkedList<Inputs>();
		inputs.add(Inputs.outputFolder);
		inputs.add(Inputs.contigFiles);
		return inputs;
	}

	@Override
	public List<Inputs> providesInputs() {
		List<Inputs> inputs = new LinkedList<Inputs>();
		inputs.add(Inputs.contigFiles);
		return inputs;
	}

	/* (non-Javadoc)
	 * @see tools.ATool#checkRunSuccessful()
	 */
	@Override
	protected boolean checkRunSuccessful() {
		for(String contigFile: this.contigFiles){
			String errorMessage = Utilities.checkFile(contigFile, FileType.fasta); 
			if(errorMessage.length()>0){
				writeExecutionLog(new String[]{errorMessage});
				return false;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see tools.ATool#setNewPipelineVariables(pipelines.APipeline)
	 */
	@Override
	protected void setNewPipelineVariables(APipeline pipeline) {
		this.contigFiles.add(sgaFile);
		this.contigFileNames.add("merged_SGA");			
		pipeline.addNewRelevantContigFiles(this.contigFiles, this.contigFileNames);
	}

	/* (non-Javadoc)
	 * @see tools.ATool#setVariables(pipelines.APipeline)
	 */
	@Override
	protected void setVariables(APipeline pipeline) {
		this.workingDir = pipeline.getOutputFolder() + "/" + pipeline.getCurrPipelineNumber() + "_" + this.pipelineFolderName;
		this.sgaFile = this.workingDir + "/SGA_"+this.mergedFile;
		this.threads = pipeline.getThreads();
		this.inputContigs = pipeline.getCurrContigFiles().toArray(new String[pipeline.getCurrContigFiles().size()]);
		this.setExecutionLog(pipeline.getExecutionLog());
	}

}
