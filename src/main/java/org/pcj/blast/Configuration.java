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

    final static public String filename;
    final static public String output;
    final static public String dbPath;
    final static public int linesToSendCount;
    final static public int blastThreadCount;
    final static public int bufferSize;

    static {
        int _bufferSize = 3;
        try {
            _bufferSize = Integer.parseInt(System.getProperty("buffer", "3"));
        } catch (NumberFormatException ex) {
        }
        bufferSize = _bufferSize;

        int _linesToSendCount = 2;
        try {
            _linesToSendCount = Integer.parseInt(System.getProperty("linesCount", "2"));
        } catch (NumberFormatException ex) {
        }
        linesToSendCount = _linesToSendCount;

        int _blastThreadCount = 1;
        try {
            _blastThreadCount = Integer.parseInt(System.getProperty("blastThreads", "1"));
        } catch (NumberFormatException ex) {
        }
        blastThreadCount = _blastThreadCount;

        dbPath = System.getProperty("db", "/icm/hydra/software/plgrid/blast/dbs/nt");
        filename = System.getProperty("input", "davit-sequence-file-20151116.fasta");
        output = System.getProperty("output", "stdout");
    }
}
