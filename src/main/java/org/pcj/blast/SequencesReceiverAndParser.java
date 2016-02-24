/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.blast;

import java.io.IOException;
import java.io.OutputStream;
import org.pcj.PCJ;

/**
 *
 * @author faramir
 */
public class SequencesReceiverAndParser {

    private int blockNo = 0;

    public void receiveAndParseSequences() throws InterruptedException, IOException {
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
    }

    private String receiveSequencesBlock() {
        PCJ.waitFor("values");
        int index = blockNo % Configuration.bufferSize;
        String value = PCJ.getLocal("values", index);
        if (value == null) {
            return null;
        }

        PCJ.put(0, "readIndex", index % Configuration.bufferSize, PCJ.myId());

        return value;
    }

    private void executeBlast(String value) throws InterruptedException, IOException {
        // http://www.ncbi.nlm.nih.gov/books/NBK279675/
        ProcessBuilder processBuiler = new ProcessBuilder(
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
                // "-out", "-".equals(Configuration.output) ? "-" : (Configuration.output + "_" + PCJ.myId() + "_" + blockNo + ".xml"),
                "-num_threads", "" + Configuration.blastThreadCount,
                "-db", Configuration.dbPath
        );
//        processBuiler.redirectError(ProcessBuilder.Redirect.appendTo(new File("outxml/err" + PCJ.myId() + ".txt")));
//        processBuiler.redirectOutput(ProcessBuilder.Redirect.appendTo(new File("outxml/out" + PCJ.myId() + ".txt")));

        long startTime = System.nanoTime();
        Process process = processBuiler.start();
        try (OutputStream os = process.getOutputStream()) {
            os.write(value.getBytes());
        }
        process.waitFor();

//                try (FileOutputStream outFile = new FileOutputStream("outxml/out" + PCJ.myId() + ".xmlout", true)) {
//                        FileOutputStream errFile = new FileOutputStream("outxml/err" + PCJ.myId() + ".txt", true)) {
//                    StreamReader stdout = new StreamReader(process.getInputStream(), outFile);
//                    Thread stdoutThread = new Thread(stdout);
//                    stdoutThread.start();
//
//                    StreamReader stderr = new StreamReader(process.getErrorStream(), errFile);
//                    Thread stderrThread = new Thread(stderr);
//                    stderrThread.start();
//                    process.waitFor();
//
//                    stderrThread.join();
//                    stdoutThread.join();
//                }
        System.out.printf("%d: BLAST execution time: %.7f%n", PCJ.myId(), (System.nanoTime() - startTime) / 1e9);
    }

}
