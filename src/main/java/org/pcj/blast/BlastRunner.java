package org.pcj.blast;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

    @Override
    public void main() throws Throwable {
        if (PCJ.myId() == 0) {
            InputFileReader.readInputFile();
        } else {
            SequencesReceiverAndParser sequenceReceiverAndParser = new SequencesReceiverAndParser();
            sequenceReceiverAndParser.receiveAndParseSequences();
        }
    }

}