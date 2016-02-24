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
import org.pcj.PCJ;

/**
 *
 * @author faramir
 */
public class InputFileReader {

    private static int threadNo;
    private static int[] readIndex;
    private static int[] writeIndex;

    public static void readInputFile() throws IOException {
        readIndex = new int[PCJ.threadCount()];
        Arrays.fill(readIndex, Configuration.bufferSize - 1);
        PCJ.putLocal("readIndex", readIndex);
        
        writeIndex = new int[PCJ.threadCount()];

        threadNo = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(Configuration.filename))) {
            String line;
            StringBuilder sb = new StringBuilder();
            for (int lineNo = 1; (line = br.readLine()) != null; ++lineNo) {
                sb.append(line).append(System.lineSeparator());
                if (lineNo % (Configuration.sequencesToSendCount * 2) == 0) {
                    sendSequences(sb);
                }
            }
            if (sb.length() > 0) {
                sendSequences(sb);
            }
        }

        sendEndOfSequences();
    }

    private static int sendSequences(StringBuilder sb) throws ClassCastException {
        threadNo = returnAvailableThreadOrWait();
        String value = sb.toString();
        System.err.println("send to: " + threadNo + "[" + writeIndex[threadNo] + "] >>> " + value.substring(0, Math.min(value.length(), 60)) + " (" + value.length() + ")");
        PCJ.put(threadNo, "values", value, writeIndex[threadNo]);
        writeIndex[threadNo] = (writeIndex[threadNo] + 1) % Configuration.bufferSize;
        sb.setLength(0);
        return threadNo;
    }

    private static int returnAvailableThreadOrWait() {
        // find available cell in one of the threads
        while (true) {
            for (int i = 0; i < PCJ.threadCount(); ++i) {
                int newThreadNo = (threadNo + i) % (PCJ.threadCount() - 1) + 1;
                if (writeIndex[newThreadNo] != readIndex[newThreadNo]) {
                    return newThreadNo;
                }
            }
            PCJ.waitFor("readIndex");
        }
    }

    private static void sendEndOfSequences() throws ClassCastException {
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
