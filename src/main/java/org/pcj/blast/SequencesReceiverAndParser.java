/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.blast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.pcj.PCJ;
import org.pcj.Storage;
import org.xml.sax.SAXException;

/**
 *
 * @author faramir
 */
public class SequencesReceiverAndParser {

    private final static Logger LOGGER = Logger.getLogger(SequencesReceiverAndParser.class.getName());

    @Storage(SequencesReceiverAndParser.class)
    enum Shared {
        values
    }

    @SuppressWarnings("final")
    private final String[] values = new String[Configuration.SEQUENCES_BUFFER_SIZE];

    private int blockNo;
    private final ProcessBuilder blastProcessBuiler;
    private final BlastXmlParser blastXmlParser;
    private final boolean hasOutputFormat;

    public SequencesReceiverAndParser() throws IOException {
        PCJ.registerStorage(Shared.class);
        blockNo = 0;

        // http://www.ncbi.nlm.nih.gov/books/NBK279675/
        List<String> blastCommand = new ArrayList<>();
        blastCommand.add(Configuration.BLAST_BINARY_PATH);

        PCJ.waitFor(BlastRunner.Shared.args);
        blastCommand.addAll(Arrays.asList(BlastRunner.args));

        hasOutputFormat = Arrays.asList(BlastRunner.args).contains("-outfmt");
        if (!hasOutputFormat) {
            blastCommand.add("-outfmt");
            blastCommand.add("5");
        }

        blastCommand.add("-num_threads");
        blastCommand.add(Integer.toString(Configuration.BLAST_THREADS_COUNT));

        LOGGER.log(Level.FINE, "Blast command: ''{0}''", String.join("' '", blastCommand));

        blastProcessBuiler = new ProcessBuilder(blastCommand);
        blastProcessBuiler.redirectError(ProcessBuilder.Redirect.INHERIT);

        if (hasOutputFormat) {
            blastProcessBuiler.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(
                    String.format("%s%c%d.blastOutput", Configuration.OUTPUT_DIR, File.separatorChar, PCJ.myId()))));

            this.blastXmlParser = null;
        } else {
            PrintWriter localWriter = new PrintWriter(new BufferedWriter(new FileWriter(String.format("%s%c%d.txtResultFile", Configuration.OUTPUT_DIR, File.separatorChar, PCJ.myId()))));
            PrintWriter globalWriter = new PrintWriter(new BufferedWriter(new FileWriter(String.format("%s%c%d.txtGlobalResultFile", Configuration.OUTPUT_DIR, File.separatorChar, PCJ.myId()))));

            BlastXmlParser xmlParser = null;
            try {
                xmlParser = new BlastXmlParser(localWriter, globalWriter);
            } catch (JAXBException | SAXException ex) {
                LOGGER.log(Level.SEVERE, "Exception while creating BlastXmlParser", ex);
            }
            this.blastXmlParser = xmlParser;
        }
    }

    public void receiveAndParseSequences() throws InterruptedException, IOException, JAXBException, SAXException {
        try {
            while (true) {
                String value = receiveSequencesBlock();
                if (value == null) {
                    LOGGER.log(Level.FINE, "{0}: finished", PCJ.myId());

                    return;
                }

                LOGGER.log(Level.FINE, "{0}: received: {1} ({2})",
                        new Object[]{PCJ.myId(), value.substring(0, Math.min(value.length(), 40)), value.length()});

                executeBlast(value);

                informAboutCompletion();

                ++blockNo;
            }
        } finally {
            if (blastXmlParser != null) {
                blastXmlParser.close();
            }
        }
    }

    private String receiveSequencesBlock() {
        PCJ.waitFor(SequencesReceiverAndParser.Shared.values);
        int index = blockNo % Configuration.SEQUENCES_BUFFER_SIZE;
        return PCJ.getLocal(SequencesReceiverAndParser.Shared.values, index);
    }

    private void informAboutCompletion() {
        int index = blockNo % Configuration.SEQUENCES_BUFFER_SIZE;
        PCJ.put(index, 0, InputFileReader.Shared.readIndex, PCJ.myId());
    }

    private void executeBlast(String value) throws InterruptedException, IOException, JAXBException, SAXException {
        long startTime = System.nanoTime();

        Process process = blastProcessBuiler.start();
        LOGGER.log(Level.FINE, "{0}: BLAST started", PCJ.myId());

        Thread xmlParserThread = null;
        if (!hasOutputFormat) {
            xmlParserThread = new Thread(
                    () -> {
                        try (InputStream inputStream = new BufferedInputStream(process.getInputStream())) {
                            blastXmlParser.processXmlFile(inputStream);
                            blastXmlParser.flush();
                        } catch (JAXBException | SAXException | IOException ex) {
                            LOGGER.log(Level.SEVERE, "Exception while processing XML file", ex);
                        }
                    }
            );

            xmlParserThread.start();
        }

        try (OutputStream os = new BufferedOutputStream(process.getOutputStream())) {
            os.write(value.getBytes());
        }

        process.waitFor();

        if (xmlParserThread != null) {
            xmlParserThread.join();
        }

        LOGGER.log(Level.INFO, "{0}: BLAST execution time: {1}", new Object[]{PCJ.myId(), (System.nanoTime() - startTime) / 1e9});
    }

}
