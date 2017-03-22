/**
 * 
 */
package tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import main.Madam;
import pipelines.APipeline;
import pipelines.Inputs;
import tools.statistics.ContigStatistics;
import tools.statistics.StatisticWriter;
import utilities.Tools;
import utilities.Utilities;

/**
 * @author Alexander Seitz
 *
 */
public class Statistics extends ATool {

	private static final String CLASS_NAME = Madam.CLASS_NAME + " statistics [options]";

	private Integer refLength = 0;
	private Integer threads = 1;
	private StatisticWriter sw;
	private String[] input;
	private String[] names;
	private Boolean hasRefLength = false;
	private OutputStream outstream = System.out;
	
	private String pipelineFolderName = "Statistics";
	
	public Statistics(){
		super(Tools.statistics);
	}

	@SuppressWarnings("static-access")
	public Statistics(String[] args){
		super(Tools.statistics);
		// create command line options
		Options helpOptions = new Options();
		helpOptions.addOption("h", "help", false, "show this help page");
		Options options = new Options();
		options.addOption("h", "help", false, "show this help page");
		options.addOption("o", "output", true, "the output file [stdout]");
		options.addOption("l", "length", true, "length of reference");
		options.addOption("t", "threads", true, "number of threads to use [1]");
		options.addOption(OptionBuilder.withLongOpt("input")
				.withArgName("INPUT")
				.withDescription("the list of input Files")
				.isRequired()
				.hasArg()
				.hasOptionalArgs()
				.create("i"));
		options.addOption(OptionBuilder.withLongOpt("names")
				.withArgName("NAMES")
				.withDescription("The names of the samples")
				.isRequired()
				.hasArg()
				.hasOptionalArgs()
				.create("n"));

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
		// initialize variables
		String outFile = "";
		Boolean hasOutFile = false;

		try {
			CommandLine cmd = parser.parse(options, args);
			cmd = parser.parse(options, args);

			input = cmd.getOptionValues("i");
			for(Integer i=0; i<this.input.length; i++){
				this.input[i] = new File(input[i]).getAbsolutePath();
			}
			names = cmd.getOptionValues("n");

			if(cmd.hasOption("o")){
				outFile = cmd.getOptionValue("o");
				outFile = new File(outFile).getAbsolutePath();
				hasOutFile = true;
			}
			if(cmd.hasOption("r")){
				String length = cmd.getOptionValue("r");
				if(Utilities.isInteger(length)){
					this.refLength = Integer.parseInt(length);
					hasRefLength = true;
				}else{
					System.err.println("Provided length not an Integer: "+length);
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
		} catch (ParseException e) {
			helpformatter.printHelp(CLASS_NAME, options);
			System.err.println(e.getMessage());
			System.exit(0);
		}
		if(input.length != names.length){
			System.err.println("the number of input files and names does not match");
			System.err.println("input: "+this.input.length);
			System.err.println("names: "+this.names.length);
			System.err.println(Arrays.asList(this.input));
			System.err.println(Arrays.asList(this.names));
			helpformatter.printHelp(CLASS_NAME, options);
			System.exit(0);
		}
		// initialize variables
		this.outstream = System.out;
		if(hasOutFile){
			try {
				outstream = new FileOutputStream(outFile, false);
			} catch (FileNotFoundException e) {
			}
		}
		run();
	}

	public void run() {
		writeExecutionLog(new String[]{"calculating statistics"});
		this.sw = new StatisticWriter(this.outstream, hasRefLength);
		List<ContigStatistics> todo = new LinkedList<ContigStatistics>();
		for(int i=0; i<input.length; i++){
			String inFile = input[i];
			String inName = names[i];
			if(hasRefLength){
				todo.add(new ContigStatistics(inFile, inName, this.refLength));
			}else{
				todo.add(new ContigStatistics(inFile, inName, 0));
			}
		}
		ExecutorService es = Executors.newFixedThreadPool(this.threads);
		try {
			@SuppressWarnings("unused")
			List<Future<Object>> answers = es.invokeAll(todo);
			es.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for(ContigStatistics cs: todo){
			this.sw.writeStatistic(cs);
		}
	}

	@Override
	public Tools getName() {
		return Tools.statistics;
	}

	@Override
	public List<Inputs> needsInputs() {
		List<Inputs> inputs = new LinkedList<Inputs>();
		inputs.add(Inputs.contigFiles);
		return inputs;
	}

	@Override
	public List<Inputs> providesInputs() {
		List<Inputs> inputs = new LinkedList<Inputs>();
		return inputs;
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
		return;
	}

	/* (non-Javadoc)
	 * @see tools.ATool#setVariables(pipelines.APipeline)
	 */
	@Override
	protected void setVariables(APipeline pipeline) {
		this.workingDir = pipeline.getOutputFolder() + "/" + pipeline.getCurrPipelineNumber() + "_" + this.pipelineFolderName;
		Utilities.createOutFolder(this.workingDir);
		String outFile = this.workingDir + "/statistics.csv";
		try {
			this.outstream = new FileOutputStream(outFile, false);
		} catch (FileNotFoundException e) {
		}
		this.sw = new StatisticWriter(this.outstream, this.hasRefLength);
		this.threads = pipeline.getThreads();
		List<String> inputs = pipeline.getAllContigFiles();
		List<String> name = pipeline.getAllContigNames();
		this.input = inputs.toArray(new String[inputs.size()]);
		this.names = name.toArray(new String[name.size()]);
		this.setExecutionLog(pipeline.getExecutionLog());
	}

}
