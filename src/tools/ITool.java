package tools;

import java.util.List;

import pipelines.APipeline;
import pipelines.Inputs;
import utilities.Tools;

public interface ITool {
	
	public Tools getName();
	public void runInPipeline(APipeline pipeline);
	public List<Inputs> needsInputs();
	public List<Inputs> providesInputs();

}
