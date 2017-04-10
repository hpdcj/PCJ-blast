/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.blast;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author faramir
 */
public class Configuration {

    private final static Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    final static public String INPUT_FILENAME;
    final static public String OUTPUT_DIR;
    final static public int OUTPUT_FORMAT;
    final static public String BLAST_BINARY_PATH;
    final static public String BLAST_DB_PATH;
    final static public int SEQUENCES_BATCH_COUNT;
    final static public int BLAST_THREADS_COUNT;
    final static public int SEQUENCES_BUFFER_SIZE;

    static {
        SEQUENCES_BUFFER_SIZE = 1 + 1; // "+1" for one empty slot

        int _sequencesToSendCount = 1;
        try {
            _sequencesToSendCount = Integer.parseInt(System.getProperty("sequenceCount", "1"));
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.CONFIG, "Unable to read sequenceCount property: {0}", ex);
        }
        SEQUENCES_BATCH_COUNT = _sequencesToSendCount;

        int _blastThreadCount = 1;
        try {
            _blastThreadCount = Integer.parseInt(System.getProperty("blastThreads", "1"));
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.CONFIG, "Unable to read blastThreads property: {0}", ex);
        }
        BLAST_THREADS_COUNT = _blastThreadCount;

        int _outputFormat = 0;
        try {
            _outputFormat = Integer.parseInt(System.getProperty("outputFormat", "-1"));
            if (_outputFormat < 0 || _outputFormat > 14) {
                _outputFormat = -1;
            }
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.CONFIG, "Unable to read outputFormat property: {0}", ex);
        }
        OUTPUT_FORMAT = _outputFormat;

        INPUT_FILENAME = System.getProperty("input", "blast-test.fasta");
        OUTPUT_DIR = System.getProperty("output", ".");
        BLAST_DB_PATH = System.getProperty("db", "./dbs/nt");
        BLAST_BINARY_PATH = System.getProperty("blast", "blastn");

        LOGGER.log(Level.CONFIG, "INPUT_FILENAME = {0}", INPUT_FILENAME);
        LOGGER.log(Level.CONFIG, "OUTPUT_DIR = {0}", OUTPUT_DIR);
        LOGGER.log(Level.CONFIG, "OUTPUT_FORMAT = {0}", OUTPUT_FORMAT);
        LOGGER.log(Level.CONFIG, "BLAST_DB_PATH = {0}", BLAST_DB_PATH);
        LOGGER.log(Level.CONFIG, "BLAST_BINARY_PATH = {0}", BLAST_BINARY_PATH);
        LOGGER.log(Level.CONFIG, "SEQUENCES_BATCH_COUNT = {0}", SEQUENCES_BATCH_COUNT);
        LOGGER.log(Level.CONFIG, "BLAST_THREADS_COUNT = {0}", BLAST_THREADS_COUNT);
    }
}
