package tools.statistics;

import java.io.OutputStream;
import java.io.PrintStream;

public class StatisticWriter {

	private PrintStream out;
	private Boolean ng50;

	public StatisticWriter(OutputStream out, Boolean ng50){
		this.out = new PrintStream(out);
		this.ng50 = ng50;
		writeHeader();
	}

	private void writeHeader() {
		StringBuffer header = new StringBuffer();
		header.append("K,");
		header.append("# contigs,");
		header.append("N50,");
		header.append("N90,");
		if(this.ng50){
			header.append("NG50,");
		}
		header.append("avg contig length,");
		header.append(">=1000,");
		header.append(">=10 000,");
		header.append(">=100 000,");
		header.append("longest contig");
		out.println(header.toString());
		out.flush();
	}

	public synchronized void writeStatistic(ContigStatistics s){
		StringBuffer content = new StringBuffer();
		content.append(s.getName());
		content.append(",");
		content.append(s.getNumContigs());
		content.append(",");
		content.append(s.getN50());
		content.append(",");
		content.append(s.getN90());
		content.append(",");
		if(this.ng50){
			content.append(s.getNg50());
			content.append(",");
		}
		content.append(String.format( "%.2f", s.getAverageContigLength()));
		content.append(",");
		content.append(s.getMoreThanThousand());
		content.append(",");
		content.append(s.getMoreThanTenThousand());
		content.append(",");
		content.append(s.getMoreThanHundredThousand());
		content.append(",");
		content.append(s.getLongestContig());
		content.append(",");
		content.append(s.getInputFile());
		out.println(content.toString());
		out.flush();
	}

}
