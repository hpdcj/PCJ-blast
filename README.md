# PCJ-blast

PCJ-blast is small piece of software which allows to run sequence alignment in parallel in highly scalable manner. PCJ-blast reads input sequence and compares it with the reference database using NCBI-BLAST. Due to the dynamic load balancing PCJ-blast is couple of times faster than solutions based on the static partitioning of input data and reference database. Moreover PCJ-blast can be run efficiently without partitioning reference database which significantly simplifies installation and usage. The PCJ-blast allows to run analysis on different hardware, starting from workstation, thorugh Hadoop clusters up to large supercomputers with thousands of cores. The observed speedup is almost linear which can reduce analysis time from weeks to single hours. 

PCJ-blast requires NCBI-BLAST installed and PCJ library. To obtain the library visit [PCJ Homepage](http://pcj.icm.edu.pl) or [GitHub repository](https://github.com/hpdcj/PCJ). The NCBI-BLAST can be obtained form [NCBI repository](https://blast.ncbi.nlm.nih.gov/). 

## Usage

`java <JVM_PARAMS> -jar PCJ-blast.jar <BLAST_PARAMS>`

There are some parameters for PCJ-blast that can be used as JVM parameters (`-D<parameter>=<value>`):
* `nodes=<path>` - path to nodes file with description of nodes (and threads) to use. It is necessary to have at least 2 lines in the file (first for _dispatcher_, next for _processors_). Default: _nodes.txt_
* `input=<path>` - path to FASTA input file. Default: _blast-test.fasta_
* `output=<path>` - path to output directory. Default: _._
* `blast=<path>` - path to BLAST executable file. Default: _blastn_
* `blastDb=<path>` - path to BLAST database file. Default: _nt_. Can be overriden by BLAST _-db_ parameter
* `hdfsConf=<path>[:<path>...]` - paths for HDFS configurations (separated by path separator character, i.e. colon (_:_) for Linux). Default: _none_
* `sequenceCount=<int>` - number of sequences in one block to submit to _processors_. Default: _1_
* `blastThreads=<int>` - number of BLAST threads. Default: _1_. Can be overriden by BLAST _-num_threads_ parameter

If BLAST `-outfmt` parameter is not set, the PCJ-blast will process it using its _output processor_.

## Reference
The usage should be acknowledged by reference to the papers:
* Marek Nowicki, Davit Bzhalava, and Piotr Bała. ["Massively Parallel Implementation of Sequence Alignment with Basic Local Alignment Search Tool Using Parallel Computing in Java Library."](https://www.liebertpub.com/doi/10.1089/cmb.2018.0079) Journal of Computational Biology (2018).
* Marek Nowicki, Davit Bzhalava, and Piotr Bała. ["Massively Parallel Sequence Alignment with BLAST Through Work Distribution Implemented using PCJ Library."](https://link.springer.com/chapter/10.1007/978-3-319-65482-9_36) International Conference on Algorithms and Architectures for Parallel Processing. Springer, Cham, 2017, p. 503-512.
