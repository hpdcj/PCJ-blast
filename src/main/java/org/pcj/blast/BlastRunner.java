package org.pcj.blast;

import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 *
 * @author faramir
 */
@RegisterStorage(BlastRunner.Shared.class)
public class BlastRunner implements StartPoint {

    @Storage(BlastRunner.class)
    enum Shared {
        args
    }
    public static String[] args;

    @SuppressWarnings("method")
    @Override
    public void main() throws Throwable {
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

}
