/**
 * 
 */
package pipelines;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import main.Madam;
import tools.ITool;
import tools.assembly.Assemblers;
import utilities.Utilities;

/**
 * @author Alexander Seitz
 *
 */
public abstract class APipeline {
	
	protected String CLASS_NAME = Madam.CLASS_NAME + " pipeline [options]";
	
	protected Assemblers assembler = Assemblers.SOAP;
	protected Boolean hasAdapterF = false;
	protected Boolean hasAdapterR = false;
	protected Boolean merge = true;
	protected Boolean pairedAssembly = false;
	protected Integer filterMinMapped = 1;
	protected Integer filterLength = 1000;
	protected Integer insertSize = 20;
	protected Integer referenceLength;
	protected Integer threads = 1;
	protected Integer trimQual = 20;
	protected List<Integer> ks = new LinkedList<Integer>(Arrays.asList(37,47,57,67,77,87,97,107,117,127));
	protected List<String> allContigFiles = new LinkedList<String>();
	protected List<String> allContigNames = new LinkedList<String>();
	protected List<String> currContigFiles = new LinkedList<String>();
	protected List<String> currContigNames = new LinkedList<String>();
	protected List<String> fastQFiles;
//	protected Set<Integer> algos = new HashSet<Integer>(Arrays.asList(1));
	protected String adapterF = "AGATCGGAAGAGCACACGTCTGAACTCCAGTCAC";
	protected String adapterR = "AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGTA";
	protected String logFile = "log.log";
	protected String outputFolder;
	protected String reference;
	protected String executionLog;
	
	protected HashMap<Inputs, Boolean> inputsMap;
	protected List<ITool> pipeline = new LinkedList<ITool>();
	protected Integer currPipelineNumber = 1;
	
	@SuppressWarnings("static-access")
	public APipeline(String className, String[] args){
		this.CLASS_NAME = className;
		initInputsMap();
		// create command line options
		Options helpOptions = new Options();
		helpOptions.addOption("h", "help", false, "show this help page");
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("assembly")
				.withArgName("ASSEMBLY")
				.withDescription("The assembly algorithm of the first Layer: SOAP, MEGAHIT ["+ this.assembler.name() +"]") //SGA, 2: VELVET "+this.algos.toString())
				.hasArg()
				.create("a"));
		options.addOption("f", "forward", true, "the forward adapter\n\t\t["+this.adapterF+"]");
		options.addOption("h", "help", false, "show this help page");
		options.addOption("l", "filterlength", true, "the minimum length of a contig to keep ["+this.filterLength+"]");
		options.addOption("m", "minFilter", true, "the minimum number of reads that have to map against a contig to keep ["+filterMinMapped+"]");
		options.addOption("q", "quality", true, "Minimum base quality for quality trimming ["+this.trimQual+"]");
		options.addOption("r", "reverse", true, "the reverse adapter\n\t\t["+this.adapterR+"]");
		options.addOption("s", "insertSize", true, "the insert size of the input["+this.insertSize+"]");
		options.addOption("t", "threads", true, "number of threads to use ["+this.threads+"]");
		options.addOption("n", "no_merge", false, "Do not merge the reads");
		options.addOption("S", "single", false, "don't merge and combine for single end assembly (implies -n)");
		options.addOption(OptionBuilder.withLongOpt("input1")
				.withArgName("INPUT1")
				.withDescription("the forward and reverse fastq files")
				.isRequired()
				.hasArg()
				.hasOptionalArgs()
				.create("in1"));
		options.addOption(OptionBuilder.withLongOpt("input2")
				.withArgName("INPUT2")
				.withDescription("the forward and reverse fastq files")
				.isRequired()
				.hasArg()
				.hasOptionalArgs()
				.create("in2"));
		options.addOption(OptionBuilder.withLongOpt("kmer")
				.withArgName("KMER")
				.withDescription("the Kmers to use [37,47,...,127]")
				.hasArg()
				.hasOptionalArgs()
				.create("k"));
		options.addOption(OptionBuilder.withLongOpt("output")
				.withArgName("OUTPUT")
				.withDescription("the output Directory")
				.isRequired()
				.hasArg()
				.create("o"));
		options.addOption(OptionBuilder.withLongOpt("reference")
				.withArgName("REFERENCE")
				.withDescription("the reference genome file")
				.isRequired()
				.hasArg()
				.create("R"));

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
		
		try {
			CommandLine cmd = parser.parse(options, args);
			this.outputFolder = cmd.getOptionValue("o");
			this.outputFolder = new File(this.outputFolder).getAbsolutePath();
			this.outputFolder = Utilities.removeTrailingSlashFromFolder(this.outputFolder);
			Utilities.createOutFolder(this.outputFolder);
			this.inputsMap.put(Inputs.outputFolder, true);
			this.logFile = this.outputFolder + "/log.log";
			this.executionLog = this.outputFolder + "/executionLog.log";
			String forwardFastQFile = cmd.getOptionValue("in1");
			String reverseFastQFile = cmd.getOptionValue("in2");
			/*if(forwardFastQFiles.length != reverseFastQFiles.length){ //TODO multiple runs
				System.err.println("number of files for forward and reverse files not equal");
				helpformatter.printHelp(CLASS_NAME, options);
				System.exit(1);
			}else{*/
				this.fastQFiles = new LinkedList<String>();
				this.fastQFiles.add(forwardFastQFile);
				this.fastQFiles.add(reverseFastQFile);
				this.inputsMap.put(Inputs.fastQFiles, true);
//			}
			this.reference = cmd.getOptionValue("R");
			this.inputsMap.put(Inputs.reference, true);
			if(cmd.hasOption("f")){
				this.adapterF = cmd.getOptionValue("f");
				this.hasAdapterF = true;
			}
			if(cmd.hasOption("r")){
				this.adapterR = cmd.getOptionValue("r");
				this.hasAdapterR = true;
			}if(cmd.hasOption("q")){
				String qual = cmd.getOptionValue("q");
				if(Utilities.isInteger(qual)){
					this.trimQual = Integer.parseInt(qual);
				}else{
					System.err.println("Provided quality not an Integer: "+qual);
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
			if(cmd.hasOption("k")){
				String[] tmpKs = cmd.getOptionValues("k");
				this.ks.clear();
				for(String k: tmpKs){
					if(Utilities.isInteger(k)){
						this.ks.add(Integer.parseInt(k));
					}else{
						System.err.println("No integer value for K: "+k);
					}
				}				
			}
			if(!(this.ks.size()>0)){
				System.err.println("No viable Ks found");
				helpformatter.printHelp(CLASS_NAME, options);
				System.exit(0);
			}
			if(cmd.hasOption("m")){
				String m = cmd.getOptionValue("m");
				if(Utilities.isInteger(m)){
					this.filterMinMapped = Integer.parseInt(m);
				}else{
					System.err.println("Provided threads not an Integer: "+m);
					helpformatter.printHelp(CLASS_NAME, options);
					System.exit(0);
				}
			}
			if(cmd.hasOption("n")){
				this.merge = false;
				this.pairedAssembly = true;
			}
			if(cmd.hasOption("S")){
				this.merge = false;
				this.pairedAssembly = false;
			}
			if(cmd.hasOption("l")){
				String l = cmd.getOptionValue("l");
				if(Utilities.isInteger(l)){
					this.filterLength = Integer.parseInt(l);
				}else{
					System.err.println("Provided length not an Integer: "+l);
					helpformatter.printHelp(CLASS_NAME, options);
					System.exit(0);
				}
			}
			if(cmd.hasOption("a")){
				String a = cmd.getOptionValue("a");
				try{
					this.assembler = Assemblers.valueOf(a.toUpperCase());
				}catch (Exception e){
					System.err.println("Command not recognized: "+a);
					helpformatter.printHelp(CLASS_NAME, options);
					System.exit(0);
				}
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
			}
		} catch (ParseException e) {
			helpformatter.printHelp(CLASS_NAME, options);
			System.err.println(e.getMessage());
			System.exit(0);
		}
		StringBuffer runCommand = new StringBuffer();
		runCommand.append("pipeline");
		for(String s: args){
			runCommand.append(" ");
			runCommand.append(s);
		}
		File rerunFile = new File(this.outputFolder+"/rerun.txt");
		Utilities.writeToFile(runCommand.toString(), rerunFile);
	}
	
	protected void runPipeline(){
		if(!testPipelineIntegrity()){
			System.err.println("pipeline Integrity not satisfied");//TODO
			System.exit(0);
		}
		for(ITool tool: this.pipeline){
			tool.runInPipeline(this);
			this.currPipelineNumber++;
		}
	}
	
	protected Boolean testPipelineIntegrity() {
		for(ITool tool: this.pipeline){
			for(Inputs input: tool.needsInputs()){
				if(!this.inputsMap.get(input)){
					System.out.println(tool.getName() + ": " + input.name());
					return false;
				}
			}
			for(Inputs output: tool.providesInputs()){
				this.inputsMap.put(output, true);
			}
		}
		return true;
	}

	private void initInputsMap() {
		this.inputsMap = new HashMap<Inputs, Boolean>();
		for(Inputs input: Inputs.values()){
			this.inputsMap.put(input, false);
		}
	}
	
	/**
	 * Add the newly generated contigs to the list of contigs.
	 * The current contigs are saved but are not used in the main analysis steps
	 * @param contigs
	 * @param names
	 */
	public void addNewRelevantContigFiles(List<String> contigs, List<String> names){
		this.allContigFiles.addAll(contigs);
		this.allContigNames.addAll(names);
		setCurrContigFiles(contigs);
		setCurrContigNames(names);
	}
	
	/**
	 * Add the generated contig files to all contigs.
	 * However, they do not replace the current contig files for the main analysis steps
	 * @param contigs
	 * @param names
	 */
	public void addAdditionalContigFilesNotForMainAnalysis(List<String> contigs, List<String> names){
		this.allContigFiles.addAll(contigs);
		this.allContigNames.addAll(names);
	}

	/**
	 * @return the allContigFiles
	 */
	public List<String> getAllContigFiles() {
		return allContigFiles;
	}
	
	/**
	 * @param allContigFiles the allContigFiles to set
	 */
	public void setAllContigFiles(List<String> allContigFiles) {
		this.allContigFiles = allContigFiles;
	}
	/**
	 * @return the allContigNames
	 */
	public List<String> getAllContigNames() {
		return allContigNames;
	}
	/**
	 * @param allContigNames the allContigNames to set
	 */
	public void setAllContigNames(List<String> allContigNames) {
		this.allContigNames = allContigNames;
	}
	/**
	 * @return the currContigFiles
	 */
	public List<String> getCurrContigFiles() {
		return currContigFiles;
	}
	/**
	 * @param currContigFiles the currContigFiles to set
	 */
	public void setCurrContigFiles(List<String> currContigFiles) {
		this.currContigFiles = currContigFiles;
	}
	/**
	 * @return the currContigNames
	 */
	public List<String> getCurrContigNames() {
		return currContigNames;
	}
	/**
	 * @param currContigNames the currContigNames to set
	 */
	public void setCurrContigNames(List<String> currContigNames) {
		this.currContigNames = currContigNames;
	}
	/**
	 * @return the fastQFiles
	 */
	public List<String> getFastQFiles() {
		return fastQFiles;
	}
	/**
	 * @param fastQFiles the fastQFiles to set
	 */
	public void setFastQFiles(List<String> fastQFiles) {
		this.fastQFiles = fastQFiles;
	}
	/**
	 * @return the assembler
	 */
	public Assemblers getAssembler() {
		return assembler;
	}
	/**
	 * @return the currPipelineNumber
	 */
	public Integer getCurrPipelineNumber() {
		return currPipelineNumber;
	}
	/**
	 * @return the filterMinMapped
	 */
	public Integer getFilterMinMapped() {
		return filterMinMapped;
	}
	/**
	 * @return the filterLength
	 */
	public Integer getFilterLength() {
		return filterLength;
	}
	/**
	 * @return the insertSize
	 */
	public Integer getInsertSize() {
		return insertSize;
	}
	/**
	 * @return the referenceLength
	 */
	public Integer getReferenceLength() {
		return referenceLength;
	}
	/**
	 * @return the threads
	 */
	public Integer getThreads() {
		return threads;
	}
	/**
	 * @return the trimQual
	 */
	public Integer getTrimQual() {
		return trimQual;
	}
	/**
	 * @return the ks
	 */
	public List<Integer> getKs() {
		return ks;
	}
	/**
	 * @return the adapterF
	 */
	public String getAdapterF() {
		return adapterF;
	}
	/**
	 * @return the adapterR
	 */
	public String getAdapterR() {
		return adapterR;
	}
	/**
	 * @return the logFile
	 */
	public String getLogFile() {
		return logFile;
	}
	/**
	 * @return the outputFolder
	 */
	public String getOutputFolder() {
		return outputFolder;
	}

	/**
	 * @return the hasAdapterF
	 */
	public Boolean getHasAdapterF() {
		return hasAdapterF;
	}

	/**
	 * @return the hasAdapterR
	 */
	public Boolean getHasAdapterR() {
		return hasAdapterR;
	}
	
	/**
	 * @return the merge
	 */
	public Boolean getMerge(){
		return this.merge;
	}
	
	/**
	 * @return the pairedAssembly
	 */
	public Boolean getPairedAssembly(){
		return this.pairedAssembly;
	}

	/**
	 * @return the reference
	 */
	public String getReference() {
		return reference;
	}

	/**
	 * @return the executionLog
	 */
	public String getExecutionLog() {
		return executionLog;
	}

}
