# MADAM - iMproving Ancient Dna AsseMbly
## Dependencies
The following software has to be installed in order for the software to run:

- jdk7
- bwa: http://bio-bwa.sourceforge.net/
- samtools: http://samtools.sourceforge.net/
- ClipAndMerge: https://github.com/apeltzer/ClipAndMerge
- sga: https://github.com/jts/sga
- SOAPdenovo2: https://sourceforge.net/projects/soapdenovo2/files/SOAPdenovo2/
- MEGAHIT: https://github.com/voutcn/megahit
- qualimap: http://qualimap.bioinfo.cipf.es/

Note: Clip&Merge is actually a java program. For correct usage, the program has to be wrapped in a starter script that can be set in the MADAM script.
This starter script should contain the following line:

<pre><code>java -jar ClipAndMerge $*</code></pre>

## generating the jar file
This program can be built with gradle ([https://gradle.org/]). for that just type

`gradle build`

The jar-files are then contained in the build/libs folder

## Tools
The following tools are available:
### assembly
perform an assembly on input data
#### Parameters:

- SOAP: Run the assembly using SOAPdenovo2
 - -i, --input &lt;INPUT&gt;: the input File(s)
 - -k, --kmer &lt;KMER&gt;: the Kmers to use
 - -o, --output &lt;OUTPUT&gt;: the output Directory
 - -p, --prefix &lt;arg&gt;: the prefix for the output files [genome]
 - -s, --insertSize &lt;arg&gt;: the insert size of the input[20]
 - -t, --threads &lt;arg&gt;: number of threads to use [1]
- MEGAHIT: Run the assembly using MEGAHIT
 - -i, --input &lt;INPUT&gt;: the input File(s)
 - -k, --kmer &lt;KMER&gt;: the Kmers to use
 - -o, --output &lt;OUTPUT&gt;: the output Directory
 - -t, --threads &lt;arg&gt;: number of threads to use [1]
 
### cm
run Clip And Merge on FastQ Files
#### Parameters:
- -f, --forward &lt;arg&gt;: the forward adapter [AGATCGGAAGAGCACACGTCTGAACTCCAGTCAC]
- -h, --help: show this help page
- -in1, --input1 &lt;INPUT1&gt;: Forward reads input file(s) in fastq(.gz) file format
- -in2, --input2 &lt;INPUT2&gt;: Reverse reads input file(s) in fastq(.gz) file format
- -l, --log &lt;arg&gt;: File for Log output [stdout]
- -n, --nomerge: don't merge, just clip and trim
- -o, --output &lt;OUTPUT&gt;: the output Directory
- -q, --quality &lt;arg&gt;: Minimum base quality for quality trimming [20]
- -r, --reverse &lt;arg&gt;: the reverse adapter [AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGTA]
- -s, --single: don't merge and combine for single end assembly (implies -n)

### filter
Filter contigs based on length and/or read concurrency
#### Parameters:
- -f, --filter &lt;arg&gt;: the name of the filtered contigs [contigs_filtered.fasta]
- -h, --help: show this help page
- -i, --input &lt;INPUT&gt;: the input contig File to filter
- -l, --length &lt;arg&gt;: the minimum length of a contig to keep [NULL]
- -m, --minFilter &lt;arg&gt;: the minimum number of reads that have to map against a contig to keep [1]
- -o, --output &lt;OUTPUT&gt;: the output Directory
- -p, --prefix &lt;arg&gt;: the prefix for the mapping files [mapped]
- -q, --fastq &lt;FASTQ&gt;: the input fastQ File(s) to filter
- -t, --threads &lt;arg&gt;: number of threads to use [1]

Note: Keep in mind that either -l or -q has to be given

### merge
Merge different Contig Files into one file using SGA
#### Parameters:
- -f, --file &lt;arg&gt;: name of the merged output file [merged.fasta]
- -h, --help: show this help page
- -i, --input &lt;INPUT&gt;: the input File(s)
- -o, --output &lt;OUTPUT&gt;: the output Directory
- -t, --threads &lt;arg&gt;: number of threads to use [1]

### pipeline
Run the pipeline as described in the paper "Improving ancient DNA genome assembly"
#### Parameters:
- -a, --assembly &lt;ASSEMBLY&gt;: The assembly algorithm of the first Layer: SOAP, MEGAHIT [SOAP]
- -f, --forward &lt;arg&gt;: the forward adapter [AGATCGGAAGAGCACACGTCTGAACTCCAGTCAC]
- -h, --help: show this help page
- -in1, --input1 &lt;INPUT1&gt;: the forward and reverse fastq files
- -in2, --input2 &lt;INPUT2&gt;: the forward and reverse fastq files
- -k, --kmer &lt;KMER&gt;: the Kmers to use [37,47,...,127]
- -l, --filterlength &lt;arg&gt;: the minimum length of a contig to keep [1000]
- -m, --minFilter &lt;arg&gt;: the minimum number of reads that have to map against a contig to keep [1]
- -n, --no_merge: Do not merge the reads
- -o, --output &lt;OUTPUT&gt;: the output Directory
- -q, --quality &lt;arg&gt;: Minimum base quality for quality trimming [20]
- -r, --reverse &lt;arg&gt;: the reverse adapter [AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGTA]
- -R, --reference &lt;REFERENCE&gt;: the reference genome file
- -s, --insertSize &lt;arg&gt;: the insert size of the input[20]
- -S, --single: don't merge and combine for single end assembly (implies -n)
- -t, --threads &lt;arg&gt;: number of threads to use [1]

### statistics
Calculate statistical values on contig files
#### Parameters:
- -h,--help: show this help page
- -i,--input &lt;INPUT&gt;: the list of input Files
- -l,--length &lt;arg&gt;: length of reference
- -n,--names &lt;NAMES&gt;: The names of the samples
- -o,--output &lt;arg&gt;: the output file [stdout]
- -t,--threads &lt;arg&gt;: number of threads to use [1]

### mapping
Map the contigs against a reference, run qualimap on the mapping and extract the contigs that could be mapped against the reference
#### Parameters:
- -h,--help: show this help page
- -i,--input &lt;INPUT&gt;: the input contig File to filter
- -o,--output &lt;OUTPUT&gt;: the output Directory
- -p,--prefix &lt;arg&gt;: the prefix of the bam file [mapped]
- -R,--reference &lt;REFERENCE&gt;: the reference genome file
- -t,--threads &lt;arg&gt;: number of threads to use [1]
