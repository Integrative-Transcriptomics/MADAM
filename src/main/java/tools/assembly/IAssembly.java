package tools.assembly;

import java.util.List;

import pipelines.APipeline;

public interface IAssembly {
	public Assemblers getAssemblyName();
	public List<String> getContigFiles();
	public List<String> getContigFileNames();
	public void runInAssembly(APipeline pipeline);
}
