/**
 * 
 */
package tools.assembly;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import main.Madam;
import pipelines.APipeline;
import tools.Assembly;
import utilities.Utilities;

/**
 * @author Alexander Seitz
 *
 */
public class SoapDenovo2 extends Assembly {
	private static final String CLASS_NAME = Madam.CLASS_NAME+" assembly SOAP [options]";
	
	private Integer threads = 1;
	private String prefix = "genome";
	private Integer insertSize = 20;
	
	String configFile;
//	private List<Integer> ks = new LinkedList<Integer>();
//	private String outFolder = "";
//	List<String> contigFiles = new LinkedList<String>();
//	List<String> contigFileNames = new LinkedList<String>();
	
	private String pipelineFolderName = "Assembly";
	
	/**
	 * 
	 */
	public SoapDenovo2() {
		super();
	}
	
	@SuppressWarnings("static-access")
	public SoapDenovo2(String[] args) {
		super();
		parseHelpOptions(args, CLASS_NAME);
		Options options = new Options();
		options.addOption("t", "threads", true, "number of threads to use ["+this.threads+"]");
		options.addOption("p", "prefix", true, "the prefix for the output files ["+this.prefix+"]");
		options.addOption("s", "insertSize", true, "the insert size of the input["+this.insertSize+"]");
		options.addOption(OptionBuilder.withLongOpt("input")
				.withArgName("INPUT")
				.withDescription("the input File(s)")
				.isRequired()
				.hasArg()
				.hasOptionalArg()
				.create("i"));
		options.addOption(OptionBuilder.withLongOpt("output")
				.withArgName("OUTPUT")
				.withDescription("the output Directory")
				.isRequired()
				.hasArg()
				.create("o"));
		options.addOption(OptionBuilder.withLongOpt("kmer")
				.withArgName("KMER")
				.withDescription("the Kmers to use")
				.isRequired()
				.hasArg()
				.hasOptionalArgs()
				.create("k"));

		HelpFormatter helpformatter = new HelpFormatter();

		CommandLineParser parser = new BasicParser();
		try {
			CommandLine cmd = parser.parse(options, args);
			cmd = parser.parse(options, args);

			String[] input = cmd.getOptionValues("i");
			this.workingDir = cmd.getOptionValue("o");
			if(input.length == 1){
				this.inputFile = input[0];
			}else if(input.length == 2){
				this.inputFile = input[0];
				this.inputFile2 = input[1];
				this.hasInputFile2 = true;
			}else{
				System.err.println("Please provide at most two input files");
				helpformatter.printHelp(CLASS_NAME, options);
				System.exit(0);
			}
			String[] tmpKs = cmd.getOptionValues("k");
			for(String k: tmpKs){
				if(Utilities.isInteger(k)){
					this.ks.add(Integer.parseInt(k));
				}else{
					System.err.println("No integer value for K: "+k);
				}
			}
			if(!(this.ks.size()>0)){
				System.err.println("No viable Ks found");
				helpformatter.printHelp(CLASS_NAME, options);
				System.exit(0);
			}
			workingDir = cmd.getOptionValue("o");
			if(cmd.hasOption("s")){
				String insSize = cmd.getOptionValue("s");
				if(Utilities.isInteger(insSize)){
					this.insertSize = Integer.parseInt(insSize);
				}else{
					System.err.println("Provided insert size not an Integer: "+insSize);
					helpformatter.printHelp(CLASS_NAME, options);
					System.exit(0);
				}
			}
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
			if(cmd.hasOption("p")){
				this.prefix = cmd.getOptionValue("p");
			}
		} catch (ParseException e) {
			helpformatter.printHelp(CLASS_NAME, options);
			System.err.println(e.getMessage());
			System.exit(0);
		}
		checkAndRemoveTooLargeInputKs();
		run();
	}

	/* (non-Javadoc)
	 * @see tools.ATool#run()
	 */
	@Override
	protected void run() {
		writeExecutionLog(new String[]{"# running Assembly using multiple k-mers with SOAPdenovo2: "});
		this.workingDir = Utilities.removeTrailingSlashFromFolder(this.workingDir);
		Utilities.createOutFolder(this.workingDir);
		this.configFile = this.workingDir + "/" + "soap.conf";
		generateConfigFile();
		runAssembly();
	}
	

	
	private void runAssembly() {
		String tmpWorkingDir = this.workingDir;
		for(Integer k: this.ks){
			if(alreadyRun()) {
				continue;
			}
			this.workingDir = tmpWorkingDir + "/" + "K" + k;
//			String currOutFolder = this.workingDir + "/" + "K" + k;
			
			String contigFile = this.workingDir + "/" + this.prefix + ".contig";
			this.contigFiles.add(contigFile);
			this.contigFileNames.add("SOAP_K"+k);
			if(new File(contigFile).exists()){
				continue;
			}
			Utilities.createOutFolder(this.workingDir);
			String[] runAssembly = new String[0];
			if(k<=63){
				runAssembly = new String[]{ "SOAPdenovo-63mer", "all", "-K",
						k.toString(), "-p", this.threads.toString(), "-s",
						this.configFile, "-o", this.workingDir+"/"+this.prefix };
			}else{
				runAssembly = new String[]{ "SOAPdenovo-127mer", "all", "-K",
						k.toString(), "-p", this.threads.toString(), "-s",
						this.configFile, "-o", this.workingDir+"/"+this.prefix };
			}
			Process process;
			try {
				writeExecutionLog(runAssembly);
				process = new ProcessBuilder(runAssembly).start();
				process.waitFor();
			} catch (IOException | InterruptedException e) {
			}
			runSuccessful();
			this.workingDir = tmpWorkingDir;
		}
	}

	private void generateConfigFile() {
		if(!new File(this.configFile).exists()){
			Integer maxReadLength = getMaxReadLenght();
			try {
				@SuppressWarnings("resource")
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(this.configFile, false)));
				out.println("#maximal read length");
				out.println("max_rd_len="+maxReadLength);
				out.println("[LIB]");
				out.println("#average insert size");
				out.println("avg_ins="+this.insertSize);
				out.println("#if sequence needs to be reversed");
				out.println("reverse_seq=0");
				out.println("#in which part(s) the reads are used");
				out.println("asm_flags=3");
				out.println("#use only first n bps of each read");
				out.println("rd_len_cutoff="+maxReadLength);
				out.println("#in which order the reads are used while scaffolding");
				out.println("rank=1");
				out.println("# cutoff of pair number for a reliable connection (at least 3 for short insert size)");
				out.println("pair_num_cutoff=3");
				out.println("#minimum aligned length to contigs for a reliable read location (at least 32 for short insert size)");
				out.println("map_len=32");
				out.println("# fastq file for single reads");
				if(this.hasInputFile2){
					out.println("q1="+this.inputFile);
					out.println("q2="+this.inputFile2);
				}else{
					out.println("q="+this.inputFile);
				}
				out.flush();
			} catch (IOException e) {
			}
		}
	}

	/* (non-Javadoc)
	 * @see tools.ATool#setNewPipelineVariables(pipelines.APipeline)
	 */
	@Override
	protected void setNewPipelineVariables(APipeline pipeline) {
		if((this.contigFileNames.size()==0 || this.contigFiles.size() == 0) || this.contigFiles.size() != this.contigFileNames.size()){
			this.contigFileNames.clear();
			this.contigFiles.clear();
			for(Integer k: this.ks){
				String currOutFolder = this.workingDir + "/" + "K" + k;
				String contigFile = currOutFolder + "/" + this.prefix + ".contig";
				this.contigFiles.add(contigFile);
				this.contigFileNames.add("SOAP_K"+k);
			}
		}
		pipeline.addNewRelevantContigFiles(this.contigFiles, this.contigFileNames);
	}

	/* (non-Javadoc)
	 * @see tools.ATool#setVariables(pipelines.APipeline)
	 */
	@Override
	protected void setVariables(APipeline pipeline) {
		this.workingDir = pipeline.getOutputFolder() + "/" + pipeline.getCurrPipelineNumber() + "_" + this.pipelineFolderName;
		if(!new File(this.workingDir).exists()) {
			Utilities.createOutFolder(this.workingDir);
		}
		this.threads = pipeline.getThreads();
		List<String> fastQFiles = pipeline.getFastQFiles();
		this.setExecutionLog(pipeline.getExecutionLog());
		if(fastQFiles.size() == 1){
			//TODO extend for multiple fastq files
			this.inputFile = fastQFiles.get(0);
		}else if(fastQFiles.size() == 2){
			this.inputFile = fastQFiles.get(0);
			this.inputFile2 = fastQFiles.get(1);
			this.hasInputFile2 = true;
		}
		this.insertSize = pipeline.getInsertSize();
		this.ks = pipeline.getKs();
		this.readLengthFile = this.workingDir+"/maxReadLength.txt";
		checkAndRemoveTooLargeInputKs();
	}
}
