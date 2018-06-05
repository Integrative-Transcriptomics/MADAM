package tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import datastructures.FastAEntry;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import io.FastAReader;
import main.Madam;
import pipelines.APipeline;
import pipelines.Inputs;
import utilities.Tools;
import utilities.Utilities;

public class Mapping extends ATool {

	private static final String CLASS_NAME = Madam.CLASS_NAME + " mapping [options]";

	private String reference;
	private String[] input;
	private String[] inputNames;
	private String[] output;
	private String[] outputNames;
	private String gapFilename = "gaps.tsv";
	private String prefix = "mapped";
	private String finalBam = this.prefix + ".sorted.bam";
	private String filteredFasta = "mappedContigs.fasta";
	private StringBuffer gaps = new StringBuffer();
	private Integer threads = 1;
	private Set<String> mappedContigs = new HashSet<String>();

	private String pipelineFolderName = "Mapping";



	public Mapping() {
		super(Tools.mapping);
	}

	@SuppressWarnings("static-access")
	public Mapping(String[] args){
		super(Tools.mapping);
		// create command line options
		Options helpOptions = new Options();
		helpOptions.addOption("h", "help", false, "show this help page");
		Options options = new Options();
		options.addOption("h", "help", false, "show this help page");
		options.addOption("p", "prefix", true, "the prefix of the bam file\n\t\t["+this.prefix+"]");
		options.addOption("t", "threads", true, "number of threads to use ["+this.threads+"]");
		options.addOption(OptionBuilder.withLongOpt("input")
				.withArgName("INPUT")
				.withDescription("the input contig File to filter")
				.isRequired()
				.hasArg()
				.create("i"));
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

		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
			this.input = cmd.getOptionValues("i");
			if(this.input.length>1){
				System.err.println("pleas specify only one input fasta file");
				System.exit(1);
			}
			this.inputNames = new String[0];
			this.workingDir = cmd.getOptionValue("o");
			this.workingDir = Utilities.removeTrailingSlashFromFolder(this.workingDir);
			this.workingDir = new File(this.workingDir).getAbsolutePath();
			this.finalBam = this.workingDir + "/" + this.prefix + ".sorted.bam";
			this.reference = cmd.getOptionValue("R");
			if(cmd.hasOption("p")){
				this.prefix = cmd.getOptionValue("p");
				this.finalBam = this.workingDir + "/" + this.prefix + ".sorted.bam";
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
		run();
	}

	@Override
	protected void run(){
		writeExecutionLog(new String[]{"# mapping the contigs against reference"});
		String tmpWorkingDir = this.workingDir;
		if(this.input.length>1 && this.input.length != this.inputNames.length){
			System.err.println("Number of inputs not equal to number of input names");
			System.exit(1);
		}
//		this.output = new String[this.input.length];
//		this.outputNames = new String[this.input.length];
		for(int i=0; i<this.input.length; i++){
			//TODO
			this.mappedContigs = new HashSet<String>();
			String inFile = this.input[i];
			if(this.inputNames.length>0){
				this.workingDir = tmpWorkingDir+"/"+i+"_"+inputNames[i];
				this.finalBam = this.workingDir + "/"+this.prefix+".sorted.bam";
			}
			if(alreadyRun()) {
				continue;
			}
			String filteredFastaFile = this.workingDir + "/" + this.filteredFasta;
//			this.output[i] = filteredFastaFile;
//			this.outputNames[i] = this.inputNames[i]+"_mapped";
			if(new File(this.finalBam).exists()){
				return;
			}
			Utilities.createOutFolder(this.workingDir);
			String sam = this.workingDir + "/tmp.sam";
			String bam = this.workingDir + "/tmp.bam";
			String[] indexReference = {"bwa", "index", this.reference};
			String[] mapContigs = new String[]{"bwa", "mem", "-t", this.threads.toString(), this.reference, inFile};
			this.writeExecutionLog(mapContigs);
			String[] convertToBam = {"samtools", "view", "-@", ""+this.threads, "-Sb", sam};
			String[] sortBam = {"samtools", "sort", "-@", ""+this.threads, bam, "-o", this.finalBam};
			String[] indexBam = {"samtools", "index", this.finalBam};
			String[] qualimap = {"qualimap", "bamqc", "-bam", this.finalBam, "-outdir", this.workingDir+"/qualimap", "-nt", this.threads.toString()};
			try {
				Process process = new ProcessBuilder(indexReference).start();
				process.waitFor();
				process = new ProcessBuilder(mapContigs).redirectOutput(new File(sam)).start();
				process.waitFor();
				process = new ProcessBuilder(convertToBam).redirectOutput(new File(bam)).start();
				process.waitFor();
				Utilities.removeFile(sam);
				process = new ProcessBuilder(sortBam).start();
				process.waitFor();
				Utilities.removeFile(bam);
				process = new ProcessBuilder(indexBam).start();
				process.waitFor();
				process = new ProcessBuilder(qualimap).start();
				process.waitFor();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
			getHeadersInBamFile(this.finalBam);
			createFilteredFasta(inFile, filteredFastaFile);
			createGapFile();
			runSuccessful();
			this.workingDir = tmpWorkingDir;
		}
	}

	private void createGapFile() {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(this.workingDir + "/" + this.gapFilename, false)));
			out.print(this.gaps.toString());
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createFilteredFasta(String inFile, String outFile){
		FastAReader fr = new FastAReader(inFile);
		FastAEntry fe;
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outFile, false)));
			while((fe = fr.getOneFastAEntry()) != null){
				String name = fe.getHeader().trim().split(" ")[0];
				if(this.mappedContigs.contains(name)){
					out.println(fe.toString());
				}
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getHeadersInBamFile(String samFile) {
		this.gaps = new StringBuffer();
		this.gaps.append("gap_start");
		this.gaps.append("\t");
		this.gaps.append("gap_stop");
		this.gaps.append("\t");
		this.gaps.append("gap_length");
		this.gaps.append("\n");
//		@SuppressWarnings("resource")
//		SamReader inputSam = new SamReader(new File(samFile));
		SamReader inputSam = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT).open(new File(samFile));
//		inputSam.setValidationStringency(SAMReader.ValidationStringency.SILENT);
		@SuppressWarnings("rawtypes")
		Iterator it = inputSam.iterator();
		int currPos = 1;
		while(it.hasNext()){
			SAMRecord curr = (SAMRecord) it.next();
			if(curr.getReadUnmappedFlag()){
				continue;
			}
			this.mappedContigs.add(curr.getReadName());
			int start = curr.getAlignmentStart();
			int end = curr.getAlignmentEnd();
			if(start > currPos+1){
				int len = start-currPos-1;
				this.gaps.append(currPos+1);
				this.gaps.append("\t");
				this.gaps.append(start-1);
				this.gaps.append("\t");
				this.gaps.append(len);
				this.gaps.append("\n");
			}
			if(end > currPos){
				currPos = end;
			}
		}
	}

	@Override
	public Tools getName() {
		return Tools.mapping;
	}

	@Override
	public List<Inputs> needsInputs() {
		List<Inputs> inputs = new LinkedList<Inputs>();
		inputs.add(Inputs.contigFiles);
		inputs.add(Inputs.reference);
		return inputs;
	}

	@Override
	public List<Inputs> providesInputs() {
		return new LinkedList<Inputs>();
	}

	/* (non-Javadoc)
	 * @see tools.ATool#checkRunSuccessful()
	 */
	@Override
	protected boolean checkRunSuccessful() {
		// TODO Auto-generated method stub
		return true;
	}

	/* (non-Javadoc)
	 * @see tools.ATool#setNewPipelineVariables(pipelines.APipeline)
	 */
	@Override
	protected void setNewPipelineVariables(APipeline pipeline) {
		//TODO pipeline.addAdditionalContigFilesNotForMainAnalysis(contigs, names);
		this.output = new String[this.input.length];
		this.outputNames = new String[this.input.length];
		for(int i=0; i<this.input.length; i++){
			String currWorkingDir = this.workingDir +"/"+i+"_"+inputNames[i];
			String filteredFastaFile = currWorkingDir + "/" + this.filteredFasta;
			this.output[i] = filteredFastaFile;
			this.outputNames[i] = this.inputNames[i]+"_mapped";
		}
		pipeline.addAdditionalContigFilesNotForMainAnalysis(Arrays.asList(this.output), Arrays.asList(this.outputNames));
		return;
	}

	/* (non-Javadoc)
	 * @see tools.ATool#setVariables(pipelines.APipeline)
	 */
	@Override
	protected void setVariables(APipeline pipeline) {
		this.workingDir = pipeline.getOutputFolder() + "/" + pipeline.getCurrPipelineNumber() + "_" + this.pipelineFolderName;
		Utilities.createOutFolder(this.workingDir);
		this.threads = pipeline.getThreads();
		this.reference = pipeline.getReference();
//		this.input = pipeline.getCurrContigFiles().toArray(new String[pipeline.getCurrContigFiles().size()]);
//		this.inputNames = pipeline.getCurrContigNames().toArray(new String[pipeline.getCurrContigNames().size()]);
		this.input = pipeline.getAllContigFiles().toArray(new String[pipeline.getAllContigFiles().size()]);
		this.inputNames = pipeline.getAllContigNames().toArray(new String[pipeline.getAllContigNames().size()]);
		setExecutionLog(pipeline.getExecutionLog());
	}

}
