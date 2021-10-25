import groovy.io.FileType
/*
Example command line:
bpipe -r pipe.gvy or bpipe execute -s pipe.gvy 'index_genome + align_reads'
The paired end read file names should end with _1.fastq.gz and _2.fastq.gz
*/

/*
 * General configuration options. Edit as per your requirements.
 */
runThreadN      = Runtime.getRuntime().availableProcessors()
annotation_file = "mus_annotation.gtf"
genome_file     = "mus_genome.fa"
data_folder     = "Data" 

/*
 * STAR specific configurations
 */

/* Genome index */
runModeGG                      = "genomeGenerate"
genomeDir                      = "GenomeIndex"
genomeFastaFiles               = genome_file
sjdbGTFfile                    = annotation_file
sjdbOverhang                   = 100
sjdbGTFtagExonParentTranscript = "transcript_id"
sjdbGTFtagExonParentGene       = "gene_id"
sjdbGTFfeatureExon             = "exon"
sjdbGTFtagExonParentGeneName   = "gene_name"
sjdbGTFtagExonParentGeneType   = "gene_biotype"

/* Read alignment */
runModeAR                      = "alignReads"
genomeLoad                     = "NoSharedMemory"
quantMode                      = "GeneCounts"
outSAMtype                     = "BAM SortedByCoordinate"
readFilesCommand               = "zcat"


/* Setup directory for traversal */
data_dir = new File(data_folder)

/*
 * Extract the file names
 */
def extract_filenames_from_dir(def dir, def list) 
{
	dir.eachFileRecurse (FileType.FILES) { file ->
		file = file.toString()
		if (file.endsWith("fastq.gz")){
			list << file
		} else {
			throw new Exception("could not extract file name")
		}
	}
}

/*
 * Setup the pipeline stages
 */
index_genome = {
	exec "STAR --runThreadN $runThreadN \
   --runMode $runModeGG --genomeDir $genomeDir \
   --genomeFastaFiles $genomeFastaFiles \
   --sjdbGTFfile $sjdbGTFfile --sjdbOverhang $sjdbOverhang \
   --sjdbGTFtagExonParentTranscript $sjdbGTFtagExonParentTranscript \
   --sjdbGTFtagExonParentGene $sjdbGTFtagExonParentGene \
   --sjdbGTFfeatureExon $sjdbGTFfeatureExon \
   --sjdbGTFtagExonParentGeneName $sjdbGTFtagExonParentGeneName \
   --sjdbGTFtagExonParentGeneType $sjdbGTFtagExonParentGeneType"
}

align_reads = {
	// Iterate through the folder of
	// each sample
	data_dir.eachFile {
		// 'it' is an implicit variable
		// println(it)
		// Iterate through the fastq files
		// inside each subfolder
		def fqs    = []
		def pair_1 = ""
		def pair_2 = ""
		def prefix = ""

		try {
			extract_filenames_from_dir(it, fqs)
			
			for (file in fqs) {
				if (file.contains("_1.fastq.gz")) {
					pair_1 = file
					prefix = it.name.split("/").first()
				}
				if (file.contains("_2.fastq.gz")) {
					pair_2 = file
				}
			}
			println("\n\tMapping data $prefix\n")

			exec "STAR --runMode $runModeAR --genomeLoad $genomeLoad \
			--quantMode $quantMode \
    --outSAMtype $outSAMtype --readFilesCommand $readFilesCommand \
    --genomeDir $genomeDir --runThreadN $runThreadN --outFileNamePrefix $prefix \
    --sjdbGTFfile $sjdbGTFfile --sjdbGTFtagExonParentTranscript $sjdbGTFtagExonParentTranscript \
    --sjdbGTFtagExonParentGene $sjdbGTFtagExonParentGene --sjdbGTFfeatureExon $sjdbGTFfeatureExon \
    --sjdbGTFtagExonParentGeneName $sjdbGTFtagExonParentGeneName \
    --sjdbGTFtagExonParentGeneType $sjdbGTFtagExonParentGeneType \
    --readFilesIn $pair_1 $pair_2"
		} catch(Exception e) {
			println("\n\twarning: extractfilename: No fastq.gz files in folder $it, ignoring ...\n")
		}
	}
}

/* Trim the gene-count files for easy import into DESeq2 */
process_counts = {
	println("Creating Counts/ folder ...")
	exec "mkdir Counts"
	println("Parsing read counts ...")
	exec "cp *ReadsPerGene* Counts/"
	exec """
	cd Counts; for file in *ReadsPerGene*; do sed -i -e 1,4d $file; cat $file | awk '{print \$1"\t"\$2 }' > counts_$file; rm $file; done
	"""
	println("Done")
}


run {index_genome + align_reads + process_counts}
