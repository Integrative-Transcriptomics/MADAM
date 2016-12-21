/**
 * 
 */
package pipelines;

import main.Madam;
import tools.ClipAndMerge;
import tools.Filter;
import tools.Mapping;
import tools.Merge;
import tools.Statistics;
import tools.assembly.Megahit;
import tools.assembly.SoapDenovo2;

/**
 * @author Alexander Seitz
 *
 */
public class AncientBacterialPipeline extends APipeline {

	public AncientBacterialPipeline(String[] args) {
		super(Madam.CLASS_NAME+ " pipeline [options]", args);
		// 1: Clip and Merge
		this.pipeline.add(new ClipAndMerge());
		// 2: Assemble
		switch(this.assembler){
		case SOAP:
			this.pipeline.add(new SoapDenovo2());
			break;
		case MEGAHIT:
			this.pipeline.add(new Megahit());
			break;
		default:
			this.pipeline.add(new SoapDenovo2());
			break;
		}
		// 3: filter
		this.pipeline.add(new Filter(false, true));
		// 4: merge
		this.pipeline.add(new Merge());
		// 5: (filter)
		this.pipeline.add(new Filter(true, false));
		// 6: statistics
		this.pipeline.add(new Statistics());
		// 7: mapping
		this.pipeline.add(new Mapping());
		// 8: statistics again
		this.pipeline.add(new Statistics());
		this.inputsMap.put(Inputs.filterLength, true);
		runPipeline();
	}
	
}
