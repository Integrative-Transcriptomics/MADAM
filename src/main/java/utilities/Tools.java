package utilities;

import pipelines.AncientBacterialPipeline;
import tools.Assembly;
import tools.ClipAndMerge;
import tools.Filter;
import tools.Mapping;
import tools.Merge;
import tools.Statistics;

public enum Tools {
	assembly{
		@Override
		public String toString(){
			return "run Assembly on input Data";
		}

		@Override
		public String getConstructorName() {
			return Assembly.class.getName();
		}
	},
	cm{
		@Override
		public String toString(){
			return "\tClip And Merge";
		}

		@Override
		public String getConstructorName() {
			return ClipAndMerge.class.getName();
		}
	},
	filter{
		@Override
		public String toString(){
			return "\tFilter contigs based on the input reads";
		}

		@Override
		public String getConstructorName() {
			return Filter.class.getName();
		}
	},
	merge{
		@Override
		public String toString(){
			return "\tMerge different Contig Files into one file";
		}

		@Override
		public String getConstructorName() {
			return Merge.class.getName();
		}
	},
	pipeline{
		@Override
		public String toString(){
			return "A predifined pipeline";
		}

		@Override
		public String getConstructorName() {
			return AncientBacterialPipeline.class.getName();
		}
	},
	statistics{
		@Override 
		public String toString(){
			return "Calculate statistical values on contig files";
		}

		@Override
		public String getConstructorName() {
			return Statistics.class.getName();
		}
	},
	mapping{
		@Override
		public String toString(){
			return "Map the contigs against a reference";
		}

		@Override
		public String getConstructorName() {
			return Mapping.class.getName();
		}
	};

	/**
	 * @return the name of the constructor to call via
	 * Java reflections
	 */
	public abstract String getConstructorName();

}
