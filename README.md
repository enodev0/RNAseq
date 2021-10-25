# RNAseq

Pipeline for processing paired-end RNA-seq datasets. It generates per-gene read counts for each sample, along with the corresponding BAM files.

## Requirements

- STAR  >= v2.7.7a
- bpipe >= v0.9.9.9
- Java  >= v8
- *nix system with >= 30GB of available RAM
  
## Setup
Before running the script, grab the genome sequence (.fa) and corresponding annotation (.gtf) for your organism of choice.

Arrange your working directory as described below. You can, of course, have any number of samples.

```bash
.
├── Data
│   │
│   ├── SampleA
│   │   ├── SampleA_1.fastq.gz
│   │   └── SampleA_2.fastq.gz
│   │
│   ├── SampleB
│   │   ├── SampleB_1.fastq.gz
│   │   └── SampleB_2.fastq.gz
│   │
│   └── SampleC
│       ├── SampleC_1.fastq.gz
│       └── SampleC_2.fastq.gz
│   
├── genome_annotation.gtf
│   
├── genome_sequence.fa
│   
└── rnaseq_star.gvy

4 directories, 9 files
```
 
Now open up the file **rnaseq_star.gvy** in your favorite text editor. At the very top, change the following lines to reflect your setup:

```groovy
annotation_file = "mus_annotation.gtf" /* change to whatever the name of your GTF file is, here, it'll be "genome_annotation.gtf" */
genome_file     = "mus_genome.fa" /* change to genome_sequence.fa */
```

## Run
Once you have the directory tree set up, run the pipeline:

```bash
~% bpipe -r rnaseq_star.gvy
``` 
The per-sample count files will be available under the **Counts** directory.

## License
MIT
