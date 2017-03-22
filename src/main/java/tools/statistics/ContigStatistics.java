/**
 * 
 */
package tools.statistics;

import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import datastructures.FastAEntry;
import io.FastAReader;

/**
 * @author seitz
 *
 */
public class ContigStatistics implements Callable<Object> {
	
	private String inputFile;
	private String name;
	private Integer N50;
	private Integer N90;
	private Integer NG50;
	private Integer moreThanThousand;
	private Integer moreThanTenThousand;
	private Integer moreThanHundredThousand;
	private Integer longestContig;
	private Integer numContigs;
	private Double averageContigLength;
	
	private Integer refLength;

	/**
	 * 
	 */
	public ContigStatistics(String file, String name, Integer refLength) {
		this.name = name;
		this.inputFile = file;
		this.refLength = refLength;
		this.N50 = 0;
		this.NG50 = 0;
		this.moreThanThousand = 0;
		this.moreThanTenThousand = 0;
		this.moreThanHundredThousand = 0;
		this.longestContig = 0;
		this.numContigs = 0;
		this.averageContigLength = 0.0;
	}

	private void calculateStatistics() {
		LinkedList<Integer> lengths = new LinkedList<Integer>();
		Double completeLength = 0.0;
		FastAReader fa = new FastAReader(this.inputFile);
		FastAEntry fe = fa.getOneFastAEntry();
		while(fe != null){
			this.numContigs++;
			Integer length = fe.getSequenceLength();
			if(length > this.longestContig){
				this.longestContig = length;
			}
			completeLength += length;
			lengths.add(length);
			if(length>900 && length <= 1000){
			}
			if(length >= 1000){
				this.moreThanThousand++;
				if(length >= 10000){
					this.moreThanTenThousand++;
					if(length >= 100000){
						this.moreThanHundredThousand++;
					}
				}
			}
			fe = fa.getOneFastAEntry();
		}
		this.averageContigLength = completeLength / (double) lengths.size();
		Collections.sort(lengths);
		Collections.reverse(lengths);
		Double currSum = 0.0;
		Double halfLength = completeLength / 2.0;
		for(Integer l: lengths){
			currSum += l;
			if(currSum >= halfLength){
				this.N50 = l;
				break;
			}
		}
		for(Integer l: lengths){
			currSum += l;
			if(currSum >= completeLength*0.9){
				this.N90 = l;
				break;
			}
		}
		if(this.refLength > 0){
			currSum = 0.0;
			halfLength = this.refLength / 2.0;
			for(Integer l: lengths){
				currSum += l;
				if(currSum >= halfLength){
					this.NG50 = l;
					break;
				}
			}
		}
	}

	/**
	 * @return the N50
	 */
	public Integer getN50() {
		return N50;
	}

	/**
	 * @return the N90
	 */
	public Integer getN90() {
		return N90;
	}

	/**
	 * @return the NG50
	 */
	public Object getNg50() {
		return this.NG50;
	}

	/**
	 * @return the moreThanThousand
	 */
	public Integer getMoreThanThousand() {
		return moreThanThousand;
	}

	/**
	 * @return the moreThanTenThousand
	 */
	public Integer getMoreThanTenThousand() {
		return moreThanTenThousand;
	}

	/**
	 * @return the moreThanHundredThousand
	 */
	public Integer getMoreThanHundredThousand() {
		return moreThanHundredThousand;
	}

	/**
	 * @return the k
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the longestContig
	 */
	public Integer getLongestContig() {
		return longestContig;
	}

	/**
	 * @return the numContigs
	 */
	public Integer getNumContigs() {
		return numContigs;
	}

	/**
	 * @return the averageContigLength
	 */
	public Double getAverageContigLength() {
		return averageContigLength;
	}

	/**
	 * @return the inputFile
	 */
	public String getInputFile() {
		return inputFile;
	}

	@Override
	public Object call() throws Exception {
		calculateStatistics();
		return this;
	}

}
