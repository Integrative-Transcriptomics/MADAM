package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import main.Madam;
import pipelines.APipeline;
import pipelines.Inputs;
import tools.assembly.Assemblers;
import tools.assembly.Megahit;
import tools.assembly.SoapDenovo2;
import utilities.FileType;
import utilities.Tools;
import utilities.Utilities;
public class Assembly extends ATool {

	private static final String CLASS_NAME = Madam.CLASS_NAME + " assembly <command> [options]";
	private Assembly tool;
	protected List<String> contigFiles = new LinkedList<String>();
	protected List<String> contigFileNames = new LinkedList<String>();
	
	protected String inputFile = "";
	private Integer maxReadLength = -1;
	protected String inputFile2 = "";
	protected Boolean hasInputFile2 = false;
	protected String readLengthFile = this.workingDir+"/maxReadLength.txt";

	protected List<Integer> ks = new LinkedList<Integer>();
	
	public Assembly() {
		super(Tools.assembly);
		this.readLengthFile = this.workingDir+"/maxReadLength.txt";
	}

	public Assembly(String[] args){
		super(Tools.assembly);
		this.readLengthFile = this.workingDir+"/maxReadLength.txt";
		if(!(args.length > 0)){
			printHelp();
			System.exit(0);
		}
		String command = args[0];
		String[] newCommands = Arrays.copyOfRange(args, 1, args.length);
		Assemblers assembler = Assemblers.SOAP;
		try{
			assembler = Assemblers.valueOf(command.toUpperCase());
		}catch (Exception e){
			System.err.println("Command not recognized: "+command);
			printHelp();
			System.exit(0);
		}
		switch(assembler){
		//TODO tools
		case SOAP:
//			tool = new SoapOld(newCommands);
			tool = new SoapDenovo2(newCommands);
			break;
		case MEGAHIT:
			tool = new Megahit(newCommands);
//		case VELVET: System.out.println("VELVET");//TODO
//			break;
//		case SGA: System.out.println("SGA");//TODO
//			break;
		default: printHelp();
			System.exit(0);
		}
		this.contigFiles = tool.getContigFiles();
		this.contigFileNames = tool.getContigFileNames();
	}

	public Tools getName(){
		return Tools.assembly;
	}

	private void printHelp(){
		StringBuffer result = new StringBuffer();
		result.append("Usage:\t");
		result.append(CLASS_NAME);
		result.append(" <command> [options]\n\n");
		result.append("Commands:\n");
		for(Assemblers a: Assemblers.values()){
			result.append(" ");
			result.append(a.name());
			result.append("\t");
			result.append(a.toString());
			result.append("\n");
		}
		System.err.println(result.toString());
	}
	
	public List<String> getContigFiles(){
		return this.contigFiles;
	}
	public List<String> getContigFileNames(){
		return this.contigFileNames;
	}

	@Override
	public void runInPipeline(APipeline pipeline) {
		switch(pipeline.getAssembler()){
		case SOAP:
			this.tool = new SoapDenovo2();
			break;
		case MEGAHIT:
			this.tool = new Megahit();
			break;
		default:
			this.tool = new SoapDenovo2();
			break;
		}
		this.tool.setVariables(pipeline);
		this.tool.runSeparately();
		this.tool.setNewPipelineVariables(pipeline);
//		tool.runInPipeline(pipeline);
	}

	@Override
	public List<Inputs> needsInputs() {
		List<Inputs> inputs = new LinkedList<Inputs>();
		inputs.add(Inputs.fastQFiles);
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
		for(String contigFile: this.contigFiles){
			String errorMessage = Utilities.checkFile(contigFile, FileType.fasta);
			if(errorMessage.length()>0){
				writeExecutionLog(new String[]{errorMessage});
				errorInFiles.add(contigFile);
//				return false;
			}
		}
		this.contigFiles.removeAll(errorInFiles);
		return !contigFiles.isEmpty();
	}

	/* (non-Javadoc)
	 * @see tools.ATool#run()
	 */
	@Override
	protected void run() {
		this.tool.run();
	}

	/* (non-Javadoc)
	 * @see tools.ATool#setNewPipelineVariables(pipelines.APipeline)
	 */
	@Override
	protected void setNewPipelineVariables(APipeline pipeline) {
		this.tool.setNewPipelineVariables(pipeline);
	}

	/* (non-Javadoc)
	 * @see tools.ATool#setVariables(pipelines.APipeline)
	 */
	@Override
	protected void setVariables(APipeline pipeline) {
		this.tool.setVariables(pipeline);
	}
	
	
	/*
	 * get the maximum read length from the input fastq files
	 */
	protected Integer getMaxReadLenght() {
		if(this.maxReadLength >=0){
			return this.maxReadLength;
		}
		if(new File(this.readLengthFile).exists()){
			Integer readLength = readReadLengthFromFile();
			if(readLength>0){
				this.maxReadLength = readLength;
				return this.maxReadLength;
			}
		}
		Integer result = 0;
		List<String> files = new LinkedList<String>();
		files.add(this.inputFile);
		if(this.hasInputFile2){
			files.add(this.inputFile2);
		}
		for(String inputFastQ: files){
			try {
				BufferedReader br = Utilities.getReader(inputFastQ);
				String currLine = "";
				Double i = 0.0;
				while((currLine = br.readLine()) != null){
					// get the maximal read length
					if(i%4==1){
						int len = currLine.length();
						if(len > result){
							result = len;
						}
					}
					i++;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Utilities.writeToFile(""+result, new File(this.readLengthFile));
		return result;
	}
	
	/**
	 * @return
	 */
	private Integer readReadLengthFromFile() {
		try {
			@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(new FileReader(this.readLengthFile));
			String currLine = "";
			while((currLine = br.readLine()) != null){
				if(Utilities.isInteger(currLine.trim())){
					return Integer.parseInt(currLine.trim());
				}else{
					return -1;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	protected void checkAndRemoveTooLargeInputKs(){
		Integer maxReadLength = getMaxReadLenght();
		List<Integer> toRemove = new LinkedList<Integer>();
		for(int k: this.ks){
			if(k>maxReadLength){
				toRemove.add(k);
			}
		}
		this.ks.removeAll(toRemove);
	}

}
