/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.blast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.PCJ;

/**
 *
 * @author faramir
 */
public class InputFileReader {

    private final static Logger LOGGER = Logger.getLogger(InputFileReader.class.getName());

    private int threadNo;
    private final int[] readIndex;
    private final int[] writeIndex;

    public InputFileReader() {
        readIndex = new int[PCJ.threadCount()];
        Arrays.fill(readIndex, Configuration.SEQUENCES_BUFFER_SIZE - 1);
        PCJ.putLocal("readIndex", readIndex);

        writeIndex = new int[PCJ.threadCount()];

        threadNo = 0;
    }

    public void readInputFile(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            StringBuilder sb = new StringBuilder();
            int seqNo = 0;
//            for (int lineNo = 1; (line = br.readLine()) != null; ++lineNo) {
            while ((line = br.readLine()) != null) {
                if (line.startsWith(">")) {
                    if (seqNo++ % (Configuration.SEQUENCES_BATCH_COUNT) == 0) {
                        if (sb.length() > 0) {
                            sendSequences(sb.toString());
                            sb.setLength(0);
                        }
                    }
                }
                if (line.isEmpty() == false) {
                    sb.append(line).append(System.lineSeparator());
                }
            }
            if (sb.length() > 0) {
                sendSequences(sb.toString());
            }
        }

        sendEndOfSequences();
    }

    private void sendSequences(String value) throws ClassCastException {
        chooseNextAvailableThreadOrWait();

        LOGGER.log(Level.FINE, "send to: {0}[{1}] >>> {2} ({3})",
                new Object[]{threadNo, writeIndex[threadNo], value.substring(0, Math.min(value.length(), 40)), value.length()});

        PCJ.put(threadNo, "values", value, writeIndex[threadNo]);
        writeIndex[threadNo] = (writeIndex[threadNo] + 1) % Configuration.SEQUENCES_BUFFER_SIZE;
    }

    private void chooseNextAvailableThreadOrWait() {
        // find available cell in one of the threads
        while (true) {
            for (int i = 0; i < PCJ.threadCount(); ++i) {
                int newThreadNo = (threadNo + i) % (PCJ.threadCount() - 1) + 1;
                if (writeIndex[newThreadNo] != readIndex[newThreadNo]) {
                    threadNo = newThreadNo;
                    return;
                }
            }
            PCJ.waitFor("readIndex");
        }
    }

    private void sendEndOfSequences() throws ClassCastException {
        // finish by sending null to the next read array cell
        for (int newThreadNo = 1; newThreadNo < PCJ.threadCount(); ++newThreadNo) {
            while (true) {
                if (writeIndex[newThreadNo] != readIndex[newThreadNo]) {
                    break;
                } else {
                    PCJ.waitFor("readIndex");
                }
            }

            PCJ.put(newThreadNo, "values", null, writeIndex[newThreadNo]);
        }
    }
}
