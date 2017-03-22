/**
 * 
 */
package tools.assembly;

import java.io.File;
import java.io.IOException;
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
public class Megahit extends Assembly {
	
	private static final String CLASS_NAME = Madam.CLASS_NAME+" assembly MEGAHIT [options]";
	
	private Integer threads = 1;
	
//	private String inputFile = "";
//	private String inputFile2 = "";
//	private Boolean hasInputFile2 = false;
	
	private String pipelineFolderName = "Assembly";

	/**
	 * 
	 */
	public Megahit() {
		super();
	}
	
	@SuppressWarnings("static-access")
	public Megahit(String[] args){
		super();
		parseHelpOptions(args, CLASS_NAME);
		Options options = new Options();options.addOption("t", "threads", true, "number of threads to use ["+this.threads+"]");
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
		writeExecutionLog(new String[]{"# running Assembly using multiple k-mers with MEGAHIT: "});
		this.workingDir = Utilities.removeTrailingSlashFromFolder(this.workingDir);
		Utilities.createOutFolder(this.workingDir); 
		runAssembly();
	}
	
	private void runAssembly() {
		for(Integer k: this.ks){
			String currOutFolder = this.workingDir + "/" + "K" + k;
			String contigFile = currOutFolder + "/final.contigs.fa";
			this.contigFiles.add(contigFile);
			this.contigFileNames.add("MEGAHIT_K"+k);
			if(new File(contigFile).exists()){
				continue;
			}
//			String[] runAssembly = new String[]{"megahit" -r $in --k-list 37,47,57,67,77,87,97,107,127 -o megalist -t 4}
			String[] runAssembly = new String[0];
			if(this.hasInputFile2){
				runAssembly = new String[]{"/share/home/seitza/software/megahit/megahit", "-1", this.inputFile, "-2", this.inputFile2, "--k-list", ""+k, "-o", currOutFolder, "-t", ""+this.threads};
			}else{
				runAssembly = new String[]{"/share/home/seitza/software/megahit/megahit", "-r", this.inputFile, "--k-list", ""+k, "-o", currOutFolder, "-t", ""+this.threads};
			}
			Process process;
			try {
				writeExecutionLog(runAssembly);
				process = new ProcessBuilder(runAssembly).start();
				process.waitFor();
			} catch (IOException | InterruptedException e) {
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
				String contigFile = currOutFolder + "/final.contigs.fa";
				this.contigFiles.add(contigFile);
				this.contigFileNames.add("MEGAHIT_K"+k);
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
		this.ks = pipeline.getKs();
		checkAndRemoveTooLargeInputKs();
	}

}
