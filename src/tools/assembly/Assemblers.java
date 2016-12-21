package tools.assembly;

public enum Assemblers {
	SOAP{
		@Override
		public String toString(){
			return "\tThe SOAPdenovo assembler";
		}
	},
	MEGAHIT{
	@Override
	public String toString(){
		return "The MEGAHIT assembler";
	}
	};
}

//},
//	VELVET{
//		@Override
//		public String toString(){
//			return "The Velvet assembler";
//		}
//	},
//	SGA{
//		@Override
//		public String toString(){
//			return "The String Graph Assembler";
//		}