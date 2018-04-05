/**
 * 
 */
package tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import pipelines.APipeline;
import utilities.Tools;

/**
 * @author Alexander Seitz
 *
 */
public abstract class ATool implements ITool {

	private Tools name;
	protected String workingDir;
	private String doneFilename;
	private String executionLog;
	private boolean doExecutionLog = false;
	
	protected Options helpOptions = new Options();
	protected Options options = new Options();

	public ATool(Tools name){
		this.name = name;
		this.doneFilename="DONE."+this.name.name();
		
		this.helpOptions.addOption("h", "help", false, "show this help page");
		
		options.addOption("h", "help", false, "show this help page");
	}

	public ATool(Tools name, String executionLog){
		this(name);
		this.doExecutionLog = true;
		this.executionLog = executionLog;
	}

	protected abstract boolean checkRunSuccessful();

	protected abstract void run();

	protected abstract void setNewPipelineVariables(APipeline pipeline);

	protected abstract void setVariables(APipeline pipeline);
	
	protected void parseHelpOptions(String[] args, String CLASS_NAME){
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
	}

	@Override
	public void runInPipeline(APipeline pipeline) {
		setVariables(pipeline);
		runSeparately();
		setNewPipelineVariables(pipeline);
	}

	protected void runSeparately(){
		if(!alreadyRun()){
			long start = System.currentTimeMillis();
			run();
			long end = System.currentTimeMillis();
			writeExecutionLog(new String[]{"# time:  "+ ((end-start)/1000) + "s"});
			writeExecutionLog(new String[]{""});
			if(checkRunSuccessful()){
				runSuccessful();
			}else{
				System.err.println("Error in Tool "+this.name.name());
				System.exit(1);
			}
		}
	}

	private void runSuccessful() {
		File done = new File(this.workingDir+"/"+this.doneFilename);
		try {
			done.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean alreadyRun() {
		File done = new File(this.workingDir+"/"+this.doneFilename);
		return done.exists();
	}

	@Override
	public Tools getName() {
		return this.name;
	}
	
	private String getCommand(String[] command){
		StringBuffer cmd = new StringBuffer();
		for(int i=0; i<command.length-1; i++){
			cmd.append(command[i]);
			cmd.append(" ");
		}
		cmd.append(command[command.length-1]);
		return cmd.toString();
	}

	protected synchronized void writeExecutionLog(String[] command){
		String cmd = getCommand(command);
		DateFormat dateFormat = new SimpleDateFormat("yyy/MM/dd HH:mm:ss");
		String currDate = "# "+dateFormat.format(new Date());
		if(this.doExecutionLog){
			try {
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(this.executionLog, true)));
				out.println(currDate);
				out.println(cmd);
				out.flush();
				out.close();
			} catch (IOException e) {
				System.err.println("error writing command to file");
				e.printStackTrace();
			}
		}else{
			System.out.println(cmd);
		}
	}
	
	protected void setExecutionLog(String file){
		this.doExecutionLog = true;
		this.executionLog = file;
	}

}
