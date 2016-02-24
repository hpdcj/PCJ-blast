package org.pcj.blast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author faramir
 */
public class BlastRunner extends Storage implements StartPoint {

    @Shared
    private String[] values = new String[Configuration.bufferSize];

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

class StreamReader implements Runnable {

    private final InputStream stream;
    private final OutputStream os;
    private Throwable exception;

    public StreamReader(InputStream reader, OutputStream os) {
        this.stream = reader;
        this.os = os;
    }

    @Override
    public void run() {
        try (BufferedInputStream is = new BufferedInputStream(stream)) {
            int b;
            while ((b = is.read()) != -1) {
                os.write(b);
            }
        } catch (Throwable ex) {
            this.exception = ex;
        }
    }

    public Throwable getException() {
        return exception;
    }
}
