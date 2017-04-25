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

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 *
 * @author Marek Nowicki
 */
@RegisterStorage(BlastRunner.Shared.class)
public class BlastRunner implements StartPoint {

    @Storage(BlastRunner.class)
    enum Shared {
        args
    }
    @SuppressWarnings("static")
    public static String[] args;

    @SuppressWarnings("method")
    @Override
    public void main() throws Throwable {
        setLoggerLevel(Level.FINE);

        if (PCJ.threadCount() < 2) {
            System.err.println("At least two PCJ threads is required!");
            System.exit(1);
        }

        if (PCJ.myId() == 0) {
            InputFileReader inputFileReader = new InputFileReader();
            inputFileReader.readInputFile(Configuration.INPUT_FILENAME);
        } else {
            SequencesReceiverAndParser sequenceReceiverAndParser = new SequencesReceiverAndParser();
            sequenceReceiverAndParser.receiveAndParseSequences();
        }
    }

    private static void setLoggerLevel(Level level) throws SecurityException {
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(level);

        Logger logger = Logger.getLogger("");
        logger.addHandler(consoleHandler);
        logger.setLevel(level);
    }
}
