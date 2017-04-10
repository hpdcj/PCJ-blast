package org.pcj.blast;

import org.pcj.PCJ;
import org.pcj.StartPoint;

/**
 *
 * @author faramir
 */
public class BlastRunner implements StartPoint {

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
