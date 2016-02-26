package org.pcj.blast;

import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 *
 * @author faramir
 */
public class BlastRunner extends Storage implements StartPoint {

    @Shared
    private String[] values = new String[Configuration.SEQUENCES_BUFFER_SIZE];

    @Shared
    private int[] readIndex;

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
