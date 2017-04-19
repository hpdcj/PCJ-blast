/* 
 * Copyright (c) 2017, Marek Nowicki
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.pcj.blast;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marek Nowicki
 */
public class Configuration {

    private final static Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    final public static String INPUT_FILENAME;
    final public static String OUTPUT_DIR;
    final public static String BLAST_BINARY_PATH;
    final public static int SEQUENCES_BATCH_COUNT;
    final public static int BLAST_THREADS_COUNT;
    final public static int SEQUENCES_BUFFER_SIZE;
    final public static String NODES_FILENAME;

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

        NODES_FILENAME = System.getProperty("nodes", "nodes.txt");
        INPUT_FILENAME = System.getProperty("input", "blast-test.fasta");
        OUTPUT_DIR = System.getProperty("output", ".");
        BLAST_BINARY_PATH = System.getProperty("blast", "blastn");

        LOGGER.log(Level.CONFIG, "NODES_FILENAME = {0}", NODES_FILENAME);
        LOGGER.log(Level.CONFIG, "INPUT_FILENAME = {0}", INPUT_FILENAME);
        LOGGER.log(Level.CONFIG, "OUTPUT_DIR = {0}", OUTPUT_DIR);
        LOGGER.log(Level.CONFIG, "BLAST_BINARY_PATH = {0}", BLAST_BINARY_PATH);
        LOGGER.log(Level.CONFIG, "SEQUENCES_BATCH_COUNT = {0}", SEQUENCES_BATCH_COUNT);
        LOGGER.log(Level.CONFIG, "BLAST_THREADS_COUNT = {0}", BLAST_THREADS_COUNT);
    }
}
