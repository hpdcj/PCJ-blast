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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.PCJ;
import org.pcj.Storage;

/**
 *
 * @author Marek Nowicki
 */
public class InputFileReader {

    private final static Logger LOGGER = Logger.getLogger(InputFileReader.class.getName());

    @Storage(InputFileReader.class)
    enum Shared {
        readIndex
    }

    @SuppressWarnings("final")
    private final int[] readIndex = new int[PCJ.threadCount()];
    private final int[] writeIndex;
    private int threadNo;

    public InputFileReader() {
        PCJ.registerStorage(Shared.class);

        Arrays.fill(readIndex, Configuration.SEQUENCES_BUFFER_SIZE - 1);
        PCJ.putLocal(Shared.readIndex, readIndex);

        writeIndex = new int[PCJ.threadCount()];

        threadNo = 0;

        PCJ.broadcast(BlastRunner.args, BlastRunner.Shared.args);
    }

    public void readInputFile(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            StringBuilder sb = new StringBuilder();
            int seqNo = 0;
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

        PCJ.put(value, threadNo, SequencesReceiverAndParser.Shared.values, writeIndex[threadNo]);
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
            PCJ.waitFor(InputFileReader.Shared.readIndex);
        }
    }

    private void sendEndOfSequences() throws ClassCastException {
        // finish by sending null to the next read array cell
        for (int newThreadNo = 1; newThreadNo < PCJ.threadCount(); ++newThreadNo) {
            while (true) {
                if (writeIndex[newThreadNo] != readIndex[newThreadNo]) {
                    break;
                } else {
                    PCJ.waitFor(InputFileReader.Shared.readIndex);
                }
            }

            PCJ.put(null, newThreadNo, SequencesReceiverAndParser.Shared.values, writeIndex[newThreadNo]);
        }
    }
}
