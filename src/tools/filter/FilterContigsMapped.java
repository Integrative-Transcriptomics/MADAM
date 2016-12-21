/**
 * 
 */
package tools.filter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;

import datastructures.FastAEntry;
import io.FastAReader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;

/**
 * @author Alexander Seitz
 *
 */
public class FilterContigsMapped {
	private HashMap<String, Integer> headers;


	public FilterContigsMapped(String bamFile, String contigFile, String outputFile, int min){
		this.headers = new HashMap<String, Integer>();
		getHeadersInSamFile(bamFile);
		removeContigsBelowMin(contigFile, outputFile, min);
	}

	private void removeContigsBelowMin(String contigFile, String outputFile, int min) {
		FastAReader fr = new FastAReader(contigFile);
		FastAEntry fe = fr.getOneFastAEntry();
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, false)));
			while(fe != null){
				String[] splitted = fe.getHeader().split(" ");
				String name = splitted[0];
				if(this.headers.containsKey(name)){
					if(this.headers.get(name)>=min){
						out.println(fe.toString());
					}
				}
				fe = fr.getOneFastAEntry();
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getHeadersInSamFile(String samFile) {
		@SuppressWarnings("resource")
		SAMFileReader inputSam = new SAMFileReader(new File(samFile));
		inputSam.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
		@SuppressWarnings("rawtypes")
		Iterator it = inputSam.iterator();
		while(it.hasNext()){
			SAMRecord curr = (SAMRecord) it.next();
			if(((curr.getFlags()&4) == 4)){
				continue;
			}
			if(curr.getMappingQuality() == 0){
				String attribute = curr.getStringAttribute("XA");
				if(attribute != null){
					String[] splitted = attribute.split(";");
					for(String at:splitted){
						String[] split = at.split(",");
						String name = split[0];
						putReferenceName(name);
					}
				}
			}
			String name = curr.getReferenceName();
			putReferenceName(name);

		}
	}

	/**
	 * see if the given name is alreads in the HashMap.
	 * if it is increment value
	 * if not put it there
	 * @param name
	 */
	private void putReferenceName(String name) {
		if(this.headers.containsKey(name)){
			this.headers.put(name, this.headers.get(name) + 1);
		}else{
			this.headers.put(name, 1);
		}
	}
	
	public static void main(String[] args){
		String bamFile = "/homes/seitza/assembly/Jorgen625/newAnalysis2/2_Assembly/K127/filtered/mapped.filtered.sorted.bam";
		String contigFile = "/homes/seitza/assembly/Jorgen625/newAnalysis2/2_Assembly/K127/genome.contig";
		String outputFile = "test.fasta";
		int min = 1;
		new FilterContigsMapped(bamFile, contigFile, outputFile, min);
	}

}
