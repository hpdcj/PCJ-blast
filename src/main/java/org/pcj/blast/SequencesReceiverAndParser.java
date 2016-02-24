/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.blast;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.pcj.PCJ;
import org.xml.sax.SAXException;

/**
 *
 * @author faramir
 */
public class SequencesReceiverAndParser {

    private int blockNo;
    private final ProcessBuilder blastProcessBuiler;
    private final BlastXmlParser blastXmlParser;

    public SequencesReceiverAndParser() throws IOException {
        blockNo = 0;

        // http://www.ncbi.nlm.nih.gov/books/NBK279675/
        blastProcessBuiler = new ProcessBuilder(
                "/icm/hydra/software/plgrid/blast/ncbi-blast-2.2.28+/bin/blastn",
                "-word_size", "11",
                "-gapopen", "0",
                "-gapextend", "2",
                "-penalty", "-1",
                "-reward", "1",
                "-max_target_seqs", "10",
                "-evalue", "0.001",
                "-show_gis",
                "-outfmt", "5",
                // "-out", "-".equals(Configuration.OUTPUT_FILENAME) ? "-" : (Configuration.OUTPUT_FILENAME + "_" + PCJ.myId() + "_" + blockNo + ".xml"),
                "-num_threads", "" + Configuration.BLAST_THREADS_COUNT,
                "-db", Configuration.BLAST_DB_PATH
        );
        blastProcessBuiler.redirectError(ProcessBuilder.Redirect.INHERIT);

        PrintWriter localWriter = new PrintWriter(new BufferedWriter(new FileWriter(String.format("%d.txtResultFile", PCJ.myId()))));
        PrintWriter globalWriter = new PrintWriter(new BufferedWriter(new FileWriter(String.format("%d.txtGlobalResultFile", PCJ.myId()))));

        blastXmlParser = new BlastXmlParser(localWriter, globalWriter);
    }

    public void receiveAndParseSequences() throws InterruptedException, IOException, JAXBException, SAXException {
        try {
            while (true) {
                String value = receiveSequencesBlock();
                if (value == null) {
                    System.out.println(PCJ.myId() + ": finished");
                    return;
                }

                System.err.println(PCJ.myId() + ": received: " + value.substring(0, Math.min(value.length(), 60)) + " (" + value.length() + ")");

                executeBlast(value);

                ++blockNo;
            }
        } finally {
            blastXmlParser.close();
        }
    }

    private String receiveSequencesBlock() {
        PCJ.waitFor("values");
        int index = blockNo % Configuration.SEQUENCES_BUFFER_SIZE;
        String value = PCJ.getLocal("values", index);
        if (value == null) {
            return null;
        }

        PCJ.put(0, "readIndex", index % Configuration.SEQUENCES_BUFFER_SIZE, PCJ.myId());

        return value;
    }

    private void executeBlast(String value) throws InterruptedException, IOException, JAXBException, SAXException {
        long startTime = System.nanoTime();
        Process process = blastProcessBuiler.start();

        try (OutputStream os = process.getOutputStream()) {
            os.write(value.getBytes());
        }

        Thread xmlParserThread = new Thread(
                () -> {
                    try {
                        Reader reader = new InputStreamReader(process.getInputStream());
                        blastXmlParser.processXmlFile(reader);
                    } catch (JAXBException | SAXException | IOException ex) {
                        Logger.getLogger(SequencesReceiverAndParser.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
        );

        xmlParserThread.start();

        process.waitFor();
        xmlParserThread.join();

        blastXmlParser.flush();

        System.out.printf("%d: BLAST execution time: %.7f%n", PCJ.myId(), (System.nanoTime() - startTime) / 1e9);
    }

}
