/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.blast;

/**
 *
 * @author faramir
 */
public class Configuration {

    final static public String INPUT_FILENAME;
    final static public String BLAST_DB_PATH;
    final static public int SEQUENCES_BATCH_COUNT;
    final static public int BLAST_THREADS_COUNT;
    final static public int SEQUENCES_BUFFER_SIZE;

    static {
        int _bufferSize = 3;
        try {
            _bufferSize = Integer.parseInt(System.getProperty("buffer", "3"));
        } catch (NumberFormatException ex) {
        }
        SEQUENCES_BUFFER_SIZE = _bufferSize;

        int _sequencesToSendCount = 1;
        try {
            _sequencesToSendCount = Integer.parseInt(System.getProperty("sequenceCount", "1"));
        } catch (NumberFormatException ex) {
        }
        SEQUENCES_BATCH_COUNT = _sequencesToSendCount;

        int _blastThreadCount = 1;
        try {
            _blastThreadCount = Integer.parseInt(System.getProperty("blastThreads", "1"));
        } catch (NumberFormatException ex) {
        }
        BLAST_THREADS_COUNT = _blastThreadCount;

        BLAST_DB_PATH = System.getProperty("db", "/icm/hydra/software/plgrid/blast/dbs/nt");
        INPUT_FILENAME = System.getProperty("input", "davit-sequence-file-20151116.fasta");
    }
}
