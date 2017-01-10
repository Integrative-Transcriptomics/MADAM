package tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.ParseException;

import datastructures.FastAEntry;
import io.FastAReader;
import main.Madam;
import pipelines.APipeline;
import pipelines.Inputs;
import tools.filter.FilterContigsMapped;
import utilities.FileType;
import utilities.Tools;
import utilities.Utilities;

/**
 * @author Alexander Seitz
 *
 */
public class Filter extends ATool {

	private static final String CLASS_NAME = Madam.CLASS_NAME + " filter [options]";

	private Integer threads = 1;
	private String prefix = "mapped";
	//	private String outFolder = "";
	private String filteredName = "contigs_filtered.fasta";
	private String finalBam = this.prefix + ".filtered.sorted.bam";
	private String[] input = new String[0];
	private String[] inputNames = new String[0];
	private Integer minMapped = 1;
	private Boolean filterFastQ = false;
	private Boolean filterLength = false;
	private Integer length = 0;
	String[] fastqs = new String[0];
	private String tmpContigFile = "";
	private boolean deleteTmpContigFile = false;

	private String pipelineFolderName = "Filter";

	public Filter(boolean filterLength, boolean filterFastQ){
		super(Tools.filter);
		this.filterLength = filterLength;
		this.filterFastQ = filterFastQ;
	}

	@SuppressWarnings("static-access")
	public Filter(String[] args){
		super(Tools.filter);
		// create command line options
		//		Options helpOptions = new Options();
		helpOptions.addOption("h", "help", false, "show this help page");
		//		Options options = new Options();
		options.addOption("h", "help", false, "show this help page");
		options.addOption("t", "threads", true, "number of threads to use ["+this.threads+"]");
		options.addOption("p", "prefix", true, "the prefix for the mapping files ["+this.prefix+"]");
		options.addOption("f", "filter", true, "the name of the filtered contigs\n\t\t["+this.filteredName+"]");
		options.addOption("m", "minFilter", true, "the minimum number of reads that have to map against a contig to keep ["+this.minMapped+"]");
		options.addOption("l", "length", true, "the minimum length of a contig to keep ["+this.length+"]");
		options.addOption(OptionBuilder.withLongOpt("output")
				.withArgName("OUTPUT")
				.withDescription("the output Directory")
				.isRequired()
				.hasArg()
				.create("o"));
		options.addOption(OptionBuilder.withLongOpt("input")
				.withArgName("INPUT")
				.withDescription("the input contig File to filter")
				.isRequired()
				.hasArg()
				.create("i"));
		options.addOption(OptionBuilder.withLongOpt("fastq")
				.withArgName("FASTQ")
				.withDescription("the input fastQ File(s) to filter")
				.hasArgs(2)
				.create("q"));

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

			this.input = cmd.getOptionValues("i");
			this.workingDir = cmd.getOptionValue("o");
			this.workingDir = new File(this.workingDir).getAbsolutePath();
//			this.filteredName = this.workingDir + "/" + "contigs_filtered.fasta";
			this.finalBam = this.workingDir + "/" + this.prefix + ".filtered.sorted.bam";
			if(cmd.hasOption("q")){
				this.fastqs = cmd.getOptionValues("q");
				this.filterFastQ = true;
				if(this.fastqs.length > 2){
					System.err.println("Please provide at most 2 fastq files");
					helpformatter.printHelp(CLASS_NAME, options);
					System.exit(0);
				}
			}
			if(cmd.hasOption("l")){
				String l = cmd.getOptionValue("l");
				if(Utilities.isInteger(l)){
					this.length = Integer.parseInt(l);
					this.filterLength = true;
				}else{
					System.err.println("Provided length not an Integer: "+l);
					helpformatter.printHelp(CLASS_NAME, options);
					System.exit(0);
				}
			}
			if(!(filterFastQ || filterLength)){
				System.err.println("Please give at least one filtering method (length or fastq)");
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
			if(cmd.hasOption("m")){
				String m = cmd.getOptionValue("m");
				if(Utilities.isInteger(m)){
					this.minMapped = Integer.parseInt(m);
				}else{
					System.err.println("Provided threads not an Integer: "+m);
					helpformatter.printHelp(CLASS_NAME, options);
					System.exit(0);
				}
			}
			if(cmd.hasOption("p")){
				this.prefix = cmd.getOptionValue("p");
				finalBam = this.workingDir + "/" + this.prefix + ".filtered.sorted.bam";
			}
			if(cmd.hasOption("f")){
				this.filteredName = cmd.getOptionValue("f");
//				this.filteredName = this.workingDir + "/" + this.filteredName;
			}
		} catch (ParseException e) {
			helpformatter.printHelp(CLASS_NAME, options);
			System.err.println(e.getMessage());
			System.exit(0);
		}
		run();
	}

	/* (non-Javadoc)
	 * @see tools.ATool#run()
	 */
	@Override
	protected void run() {
		writeExecutionLog(new String[]{"# filtering contig files"});
		if(this.filterLength && this.filterFastQ){
			writeExecutionLog(new String[]{"# by read concurrency and length ("+this.length+")"});
		}else if (this.filterLength){
			writeExecutionLog(new String[]{"# by length ("+this.length+")"});
		}else{
			writeExecutionLog(new String[]{"# by read concurrency"});
		}
		this.workingDir = Utilities.removeTrailingSlashFromFolder(this.workingDir);
		File f = new File("");
		try {
			f = new File(Filter.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		String tmpWorkingDir = this.workingDir;
		String tmpFilteredName = this.filteredName;
		for(int i=0; i<this.input.length; i++){
			if(this.input.length>1){
				this.workingDir = tmpWorkingDir+"/"+this.inputNames[i];
			}
			this.filteredName = this.workingDir + "/" + tmpFilteredName;
			Utilities.createOutFolder(this.workingDir);
			String input = this.input[i];
			List<String> command = new LinkedList<String>();
			command.add("java");
			command.add("-jar");
			command.add(f.getAbsolutePath());
			command.add("filter");
			command.add("-i");
			command.add(input);
			command.add("-o");
			command.add(this.workingDir);
			command.add("-t");
			command.add(""+this.threads);
			command.add("-p");
			command.add(this.prefix);
			if(this.filterLength){
				if(!Utilities.fileExists(this.workingDir+"/"+this.filteredName)){
//					Utilities.createOutFolder(this.workingDir);
					command.add("-l");
					command.add(""+this.length);
					filterContigsLength(input);
					if(this.filterFastQ){
						this.tmpContigFile = this.workingDir + "/tmpContigFile.fasta";
						Utilities.moveFile(filteredName, tmpContigFile);
						input = this.tmpContigFile;
						this.deleteTmpContigFile = true;
					}
				}
			}
			if(this.filterFastQ){
				command.add("-q");
				if(this.fastqs.length==1){
					command.add(this.fastqs[0]);					
				}else if (this.fastqs.length==2){
					command.add(this.fastqs[0]);
					command.add(this.fastqs[1]);
				}
				command.add("-m");
				command.add(""+this.minMapped);
				if(!Utilities.fileExists(this.filteredName)){
//					Utilities.createOutFolder(this.workingDir);
					String sam = mapAgainstContigs(input);
					filterForMappedContigs(sam, input);
					createFilteredBamFile(sam);
				}
			}
			if(this.deleteTmpContigFile){
				Utilities.removeFile(this.tmpContigFile);
			}
			this.workingDir = tmpWorkingDir;
			this.filteredName = tmpFilteredName;
			writeExecutionLog(command.toArray(new String[command.size()]));
		}
	}

	private void filterContigsLength(String input) {
		FastAReader fa = new FastAReader(input);
		FastAEntry fe = fa.getOneFastAEntry();
		try {
			@SuppressWarnings("resource")
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(this.filteredName, false)));
			while(fe != null){
				Integer len = fe.getSequenceLength();
				if(len >= this.length){
					out.println(fe.toString());
					out.flush();
				}
				fe = fa.getOneFastAEntry();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createFilteredBamFile(String sam) {
		String bamFiltered = this.workingDir + "/" + this.prefix + ".filtered.bam";
		String[] runBam = {"samtools", "view", "-@", ""+this.threads, "-bT", this.filteredName, sam};
		String[] sortBam = {"samtools", "sort", "-@", ""+this.threads, bamFiltered, this.finalBam.replace(".bam", "")};
		String[] indexBam = {"samtools", "index", this.finalBam};

		try {
			Process process = new ProcessBuilder(runBam).redirectOutput(new File(bamFiltered)).start();
			process.waitFor();
			process = new ProcessBuilder(sortBam).start();
			process.waitFor();
			Utilities.removeFile(sam);
			process = new ProcessBuilder(indexBam).start();
			process.waitFor();
			Utilities.removeFile(bamFiltered);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void filterForMappedContigs(String tmpBam, String input) {
		new FilterContigsMapped(tmpBam, input, this.filteredName, this.minMapped);
	}

	private String mapAgainstContigs(String input) {
		// initialize variables
		String sam = this.workingDir + "/tmp.sam";
		String sam2 = this.workingDir + "/tmp2.sam";
		// commands to run
		String[] indexReference = {"bwa", "index", input};
		String[] mapContigs = new String[0];
		if(this.fastqs.length==1){
			mapContigs = new String[]{"bwa", "mem", "-t", this.threads.toString(), input, this.fastqs[0]};
		}else if(fastqs.length == 2){
			mapContigs = new String[]{"bwa", "mem", "-t", this.threads.toString(), input, this.fastqs[0], this.fastqs[1]};
		}else{
			System.err.println("Error: "+this.fastqs.length+" number of fastqs found, only 1 or 2 supported");
			System.exit(0);
		}
		String[] removeHeader = {"samtools", "view", "-S", sam}; 
		try {
			if(!Utilities.fileExists(sam)){
				Process process = new ProcessBuilder(indexReference).start();
				process.waitFor();
				process = new ProcessBuilder(mapContigs).redirectOutput(new File(sam)).redirectError(new File(this.workingDir+"/log.txt")).start();
				process.waitFor();
				// delete the bwa index
				Utilities.removeFile(this.input+".*");
			}
			if(!Utilities.fileExists(sam2)){
				Process process = new ProcessBuilder(removeHeader).redirectOutput(new File(sam2)).start();
				process.waitFor();
			}
			Utilities.removeFile(sam);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return sam2;
	}

	@Override
	public Tools getName() {
		return Tools.filter;
	}

	public String getFilteredFile(){
		return this.filteredName;
	}

	@Override
	public List<Inputs> needsInputs() {
		List<Inputs> inputs = new LinkedList<Inputs>();
		inputs.add(Inputs.outputFolder);
		inputs.add(Inputs.contigFiles);
		if(this.filterFastQ){
			inputs.add(Inputs.fastQFiles);
		}
		if(this.filterLength){
			inputs.add(Inputs.filterLength);
		}
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
		List<String> errorInFiles = new LinkedList<String>();
		for(int i=0; i<this.input.length; i++){
			String outFile = "";
			if(this.input.length>1){
				outFile = this.workingDir+"/"+this.inputNames[i]+"/"+this.filteredName;				
			}else{
				outFile = this.workingDir+"/"+this.filteredName;
			}
			String errorMessage = Utilities.checkFile(outFile, FileType.fasta);
			if(errorMessage.length()>0){
				writeExecutionLog(new String[]{errorMessage});
				errorInFiles.add(outFile);
//				return false;
			}
		}
		return this.input.length > errorInFiles.size();
	}
	
//	protected boolean checkRunSuccessful() {
//		List<String> errorInFiles = new LinkedList<String>();
//		for(String contigFile: this.contigFiles){
//			String errorMessage = Utilities.checkFile(contigFile, FileType.fasta);
//			if(errorMessage.length()>0){
//				writeExecutionLog(new String[]{errorMessage});
//				errorInFiles.add(contigFile);
////				return false;
//			}
//		}
//		this.contigFiles.removeAll(errorInFiles);
//		return !contigFiles.isEmpty();
//	}

	/* (non-Javadoc)
	 * @see tools.ATool#setNewPipelineVariables(pipelines.APipeline)
	 */
	@Override
	protected void setNewPipelineVariables(APipeline pipeline) {
		List<String> newContigFiles = new LinkedList<String>();
		List<String> newContigNames = new LinkedList<String>();
		if(this.input.length==1){
			newContigFiles.add(this.workingDir+"/"+this.filteredName);
			newContigNames.add(this.inputNames[0]+"filtered");
		}else{
			for(int i=0; i<this.input.length; i++){
				newContigFiles.add(this.workingDir+"/"+this.inputNames[i]+"/"+this.filteredName);
				newContigNames.add(this.inputNames[i]+"filtered");
			}
		}
		pipeline.addNewRelevantContigFiles(newContigFiles, newContigNames);
	}

	/* (non-Javadoc)
	 * @see tools.ATool#setVariables(pipelines.APipeline)
	 */
	@Override
	protected void setVariables(APipeline pipeline) {
		this.workingDir = pipeline.getOutputFolder() + "/" + pipeline.getCurrPipelineNumber() + "_" + this.pipelineFolderName;
		Utilities.createOutFolder(this.workingDir);
		this.threads = pipeline.getThreads();
		this.minMapped = pipeline.getFilterMinMapped();
		this.input = pipeline.getCurrContigFiles().toArray(new String[pipeline.getCurrContigFiles().size()]);
		this.inputNames = pipeline.getCurrContigNames().toArray(new String[pipeline.getCurrContigNames().size()]);
		if(this.filterFastQ){
			this.fastqs = pipeline.getFastQFiles().toArray(new String[pipeline.getFastQFiles().size()]);
		}
		if(this.filterLength){
			this.length = pipeline.getFilterLength();
		}
		this.setExecutionLog(pipeline.getExecutionLog());
	}

}
