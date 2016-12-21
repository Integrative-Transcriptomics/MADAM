package tools.merge;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import utilities.Pair;
import utilities.Utilities;

public class MergeCleanUp {
	
	private String folder;
	private String inputFileName;
	private String outputFileName;
	private String selfMapped;
	private Integer threads;
	private List<Pair<Set<String>, Set<SAMRecord>>> toResolve;
	
	public MergeCleanUp(String folder, String inputName, String outputName, Integer threads){
		this.folder = folder;
		this.inputFileName = inputName;
		this.outputFileName = outputName;
		this.threads = threads;
		this.selfMapped = this.folder + "/mappedSelf.bam";
		this.toResolve = new LinkedList<Pair<Set<String>, Set<SAMRecord>>>();
		map();
		parseBam();
	}

	private void parseBam() {
		@SuppressWarnings("resource")
		SAMFileReader inputSam = new SAMFileReader(new File(this.selfMapped));
		inputSam.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
		@SuppressWarnings("rawtypes")
		Iterator it = inputSam.iterator();
		while(it.hasNext()){
			SAMRecord curr = (SAMRecord) it.next();
			if(((curr.getFlags()&4) == 4)){
				continue;
			}
			if(curr.getReadName().equals(curr.getReferenceName())){
				continue;
			}
			String ref = curr.getReferenceName();
			String name = curr.getReadName();
			boolean found = false;
			for(Pair<Set<String>, Set<SAMRecord>> p: this.toResolve){
				if(p.getFirst().contains(ref) || p.getFirst().contains(name)){
					found = true;
					p.getFirst().add(ref);
					p.getFirst().add(name);
					p.getSecond().add(curr);
					break;
				}
			}
			if(!found){
				HashSet<String> h1 = new HashSet<String>();
				HashSet<SAMRecord> h2 = new HashSet<SAMRecord>();
				h1.add(ref);
				h1.add(name);
				h2.add(curr);
				this.toResolve.add(new Pair<Set<String>, Set<SAMRecord>>(h1, h2));
			}
//			System.out.println(curr.getReadName() + "\t" + curr.getReferenceName());
		}
		Collections.sort(this.toResolve, new Comparator<Pair<Set<String>, Set<SAMRecord>>>() {
			public int compare(Pair<Set<String>, Set<SAMRecord>> p1, Pair<Set<String>, Set<SAMRecord>> p2){
				return Integer.compare(p1.getFirst().size(), p2.getFirst().size());
			}
		});
		for(Pair<Set<String>, Set<SAMRecord>> p: this.toResolve){
			System.out.println(p.getFirst().size());
		}
		
		System.out.println(toResolve.size());
		//TODO
	}

	private void map() {
		String file = this.folder + "/" + this.inputFileName;
		String sam = this.folder + "/tmp.sam";
		String bam = this.folder + "/tmp.bam";
		String[] indexReference = {"bwa", "index", file};
		String[] mapContigs = new String[]{"bwa", "mem", "-a", "-t", this.threads.toString(), file, file};
		String[] convertToBam = {"samtools", "view", "-@", ""+this.threads, "-Sb", sam};
		String[] sortBam = {"samtools", "sort", "-@", ""+this.threads, bam, this.selfMapped.replace(".bam", "")};
		String[] indexBam = {"samtools", "index", this.selfMapped};
		try {
			Process process = new ProcessBuilder(indexReference).start();
			process.waitFor();
			process = new ProcessBuilder(mapContigs).redirectOutput(new File(sam)).start();
			process.waitFor();
			// delete the bwa index
			Utilities.removeFile(file+".*");
			process = new ProcessBuilder(convertToBam).redirectOutput(new File(bam)).start();
			process.waitFor();
			Utilities.removeFile(sam);
			process = new ProcessBuilder(sortBam).start();
			process.waitFor();
			Utilities.removeFile(bam);
			process = new ProcessBuilder(indexBam).start();
			process.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		if(args.length != 3){
			System.err.println("Usage: folder input output");
			System.exit(0);
		}
		String folder = args[0];
		String input = args[1];
		String output = args[2];
		new MergeCleanUp(folder, input, output, 4);
	}

}
