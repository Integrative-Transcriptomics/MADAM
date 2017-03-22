package tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

public class ClipAndMerge extends ATool {

	private static final String CLASS_NAME = Madam.CLASS_NAME + " cm [optional parameters] forward reverse output";

	private String pipelineFolderName = "ClipAndMerge";
	private String inputF = "";
	private String inputR = "";
	private String output = "";
	private String outF = "";
	private String outR = "";
	private String logFile = "";
	private String adapterF = "AGATCGGAAGAGCACACGTCTGAACTCCAGTCAC";
	private Boolean hasAdapterF = false;
	private String adapterR = "AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGTA";
	private Boolean hasAdapterR = false;
	private Integer quality = 20;
	private Boolean merge = true;
	private Boolean pairedAssembly = false; 

	public ClipAndMerge(){
		super(Tools.cm);
	}

	@SuppressWarnings("static-access")
	public ClipAndMerge(String[] args){
		super(Tools.cm);
		parseHelpOptions(args, CLASS_NAME);

		// create tool specific command line options
		this.options.addOption("f", "forward", true, "the forward adapter\n\t\t["+this.adapterF+"]");
		this.options.addOption("r", "reverse", true, "the reverse adapter\n\t\t["+this.adapterR+"]");
		this.options.addOption("q", "quality", true, "Minimum base quality for quality trimming ["+this.quality+"]");
		this.options.addOption("l", "log", true, "File for Log output [stdout]");
		this.options.addOption("n", "nomerge", false, "don't merge, just clip and trim");
		this.options.addOption("s", "single", false, "don't merge and combine for single end assembly (implies -n)");
		options.addOption(OptionBuilder.withLongOpt("input1")
				.withArgName("INPUT1")
				.withDescription("Forward reads input file(s) in fastq(.gz) file format")
				.hasArg()
				.isRequired()
//				.hasOptionalArgs()
				.create("in1"));
		options.addOption(OptionBuilder.withLongOpt("input2")
				.withArgName("INPUT2")
				.withDescription("Reverse reads input file(s) in fastq(.gz) file format")
				.hasArg()
//				.hasOptionalArgs()
				.create("in2"));
		options.addOption(OptionBuilder.withLongOpt("output")
				.withArgName("OUTPUT")
				.withDescription("the output Directory")
				.isRequired()
				.hasArg()
				.create("o"));
		HelpFormatter helpformatter = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
		//initialize variables
		try {
			CommandLine cmd = parser.parse(options, args);
			this.inputF = cmd.getOptionValue("in1");
			this.output = cmd.getOptionValue("o");
			if(cmd.hasOption("in2")){
				this.inputR = cmd.getOptionValue("in2");
//				if(this.inputF.length != this.inputR.length){
//					System.err.println("number of files for forward and reverse inputs different");
//					System.exit(1);
//				}
			}
			if(cmd.hasOption("q")){
				String qual = cmd.getOptionValue("q");
				if(Utilities.isInteger(qual)){
					quality = Integer.parseInt(qual);
				}else{
					System.err.println("Provided quality not an Integer: "+qual);
					helpformatter.printHelp(CLASS_NAME, options);
					System.exit(0);
				}
			}
			if(cmd.hasOption("f")){
				adapterF = cmd.getOptionValue("f");
				hasAdapterF = true;
			}
			if(cmd.hasOption("r")){
				adapterR = cmd.getOptionValue("r");
				hasAdapterR = true;
			}
			if(cmd.hasOption("l")){
				String logFile = cmd.getOptionValue("l");
				this.logFile = logFile;
			}
			if(cmd.hasOption("n")){
				this.merge = false;
				this.pairedAssembly = true;
			}
			if(cmd.hasOption("s")){
				this.merge = false;
				this.pairedAssembly = false;
			}
			if(this.logFile.length()==0){
				setLogFile();
			}
		} catch (ParseException e) {
			helpformatter.printHelp(CLASS_NAME, options);
			System.err.println(e.getMessage());
			System.exit(0);
		}
		runSeparately();
	}
	
	private void setLogFile() {
		File f = new File(this.output);
		this.logFile = f.getParent()+"/CMlog.log";
	}

	@Override
	protected void run() {
		writeExecutionLog(new String[]{"# running Clip&Merge: "});
		LinkedList<String> tmpCommand = new LinkedList<String>();
		tmpCommand.add("ClipAndMerge");
		tmpCommand.add("-in1");
		tmpCommand.add(this.inputF);
		if(this.inputR.length()>0){
			tmpCommand.add("-in2");
			tmpCommand.add(this.inputR);
		}
		tmpCommand.add("-o");
		tmpCommand.add(this.output);
		tmpCommand.add("-q");
		tmpCommand.add(""+this.quality);
		if(this.hasAdapterF){
			tmpCommand.add("-f");
			tmpCommand.add(this.adapterF);
		}
		if(this.hasAdapterR){
			tmpCommand.add("-r");
			tmpCommand.add(this.adapterR);
		}
		if(!this.merge){
			tmpCommand.add("-no_merging");
			File f = new File(output);
			this.outF = f.getParent()+"/F_" + f.getName();
			this.outR = f.getParent()+"/R_" + f.getName();
			tmpCommand.add("-u");
			tmpCommand.add(this.outF);
			tmpCommand.add(this.outR);
			if(this.pairedAssembly){
				tmpCommand.add("-rm_no_partner");
			}
		}
		tmpCommand.add("-log");
		tmpCommand.add(this.logFile);
		String[] command = tmpCommand.toArray(new String[tmpCommand.size()]);
		Process process;
		try {
			writeExecutionLog(command);
			process = new ProcessBuilder(command).start();
			process.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		if(!this.merge && !this.pairedAssembly){
			combineFastQs();
		}
	}

	private void combineFastQs() {
		mergeFiles(this.outF, false, "F_");
		mergeFiles(this.outR, true, "R_");
	}
	
	private void mergeFiles(String file, boolean append, String prefix){
		try {
			BufferedReader bfr;
			if(file.endsWith("gz")){
				bfr = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
			}else{
				bfr = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			}
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(this.output, append))));
			String currLine = "";
			int i=0;
			while((currLine = bfr.readLine()) != null){
				if(i%4==0){
					currLine="@"+prefix+currLine.substring(1);
				}
				writer.write(currLine+"\n");
				i++;
			}
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<Inputs> needsInputs() {
		List<Inputs> inputs = new LinkedList<Inputs>();
		inputs.add(Inputs.outputFolder);
		inputs.add(Inputs.fastQFiles);
		return inputs;
	}

	@Override
	public List<Inputs> providesInputs() {
		List<Inputs> inputs = new LinkedList<Inputs>();
		inputs.add(Inputs.fastQFiles);
		return inputs;
	}

	@Override
	protected boolean checkRunSuccessful() {
//		return false;
		if(this.merge){
			String errorMessage = Utilities.checkFile(this.output, FileType.fastq);
			if(errorMessage.length()>0){
				writeExecutionLog(new String[]{errorMessage});
				return false;
			}
		}else{
			String errorMessage = Utilities.checkFile(this.outF, FileType.fastq);
			if(errorMessage.length()>0){
				writeExecutionLog(new String[]{errorMessage});
				return false;
			}
			errorMessage = Utilities.checkFile(this.outR, FileType.fastq);
			if(errorMessage.length()>0){
				writeExecutionLog(new String[]{errorMessage});
				return false;
			}
		}
		return true;
	}

	@Override
	protected void setNewPipelineVariables(APipeline pipeline) {
		List<String> newFastQFiles = new LinkedList<String>();
		if(this.pairedAssembly){
			newFastQFiles.add(this.outF);
			newFastQFiles.add(this.outR);
		}else{
			newFastQFiles.add(this.output);
		}
		pipeline.setFastQFiles(newFastQFiles);
	}

	@Override
	protected void setVariables(APipeline pipeline) {
		List<String> inputs = pipeline.getFastQFiles();
		this.inputF = inputs.get(0);
		this.inputR = inputs.get(1);
		this.workingDir = pipeline.getOutputFolder() + "/" + pipeline.getCurrPipelineNumber() + "_" + this.pipelineFolderName;
		Utilities.createOutFolder(this.workingDir);
		this.output = this.workingDir + "/reads.fastq.gz";
		File f = new File(output);
		this.outF = f.getParent()+"/F_" + f.getName();
		this.outR = f.getParent()+"/R_" + f.getName();
		this.quality = pipeline.getTrimQual();
		if(pipeline.getHasAdapterF()){
			this.hasAdapterF = true;
			this.adapterF = pipeline.getAdapterF();
		}
		if(pipeline.getHasAdapterR()){
			this.adapterR = pipeline.getAdapterR();
		}
		this.merge = pipeline.getMerge();
		this.pairedAssembly = pipeline.getPairedAssembly();
		this.setExecutionLog(pipeline.getExecutionLog());
		setLogFile();
	}

}
