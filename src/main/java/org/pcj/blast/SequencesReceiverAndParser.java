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

import javax.xml.bind.JAXBException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.pcj.PCJ;
import org.pcj.Storage;
import org.xml.sax.SAXException;

/**
 *
 * @author Marek Nowicki
 */
public class SequencesReceiverAndParser {

    private final static Logger LOGGER = Logger.getLogger(SequencesReceiverAndParser.class.getName());

    private FileSystem getHadoopFileSystem() throws IOException {
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        Arrays.stream(Configuration.HDFS_CONFIGURATIONS)
                .filter(((Predicate<String>) String::isEmpty).negate())
                .map(org.apache.hadoop.fs.Path::new)
                .forEach(conf::addResource);
        org.apache.hadoop.fs.FileSystem fileSystem = org.apache.hadoop.fs.FileSystem.get(conf);
        return fileSystem;
    }

    private void createDir(String outputPath) throws IOException {
        URI uri = URI.create(outputPath);
        if ("hdfs".equals(uri.getScheme())) {
            FileSystem fileSystem = getHadoopFileSystem();
            Path path = new org.apache.hadoop.fs.Path(uri.getPath());

            fileSystem.mkdirs(path);
        } else {
            new File(outputPath).mkdirs();
        }

    }

    private Writer openOutputWriter(String outputFile) throws IOException {
        URI uri = URI.create(outputFile);
        if ("hdfs".equals(uri.getScheme())) {
            FileSystem fileSystem = getHadoopFileSystem();
            Path path = new org.apache.hadoop.fs.Path(uri.getPath());

            return new OutputStreamWriter(
                    fileSystem.create(path).getWrappedStream(),
                    StandardCharsets.UTF_8);
        } else {
            return new FileWriter(outputFile);
        }
    }

    @Storage(SequencesReceiverAndParser.class)
    enum Shared {
        values
    }

    @SuppressWarnings("final")
    private final String[] values = new String[Configuration.SEQUENCES_BUFFER_SIZE];

    private int blockNo;
    private final ProcessBuilder blastProcessBuiler;
    private final Writer outputDataWriter;
    private final BlastXmlParser blastXmlParser;
    private final boolean hasOutputFormat;

    public SequencesReceiverAndParser() throws IOException {
        PCJ.registerStorage(Shared.class, this);
        blockNo = 0;

        // http://www.ncbi.nlm.nih.gov/books/NBK279675/
        List<String> blastCommand = new ArrayList<>();
        blastCommand.add(Configuration.BLAST_BINARY_PATH);

        PCJ.waitFor(BlastRunner.Shared.args);
        List<String> argsAsList = Arrays.asList(BlastRunner.args);
        blastCommand.addAll(argsAsList);

        hasOutputFormat = argsAsList.contains("-outfmt");
        if (!hasOutputFormat) {
            blastCommand.add("-outfmt");
            blastCommand.add("5");
        }

        if (!argsAsList.contains("-num_threads")) {
            blastCommand.add("-num_threads");
            blastCommand.add(Integer.toString(Configuration.BLAST_THREADS_COUNT));
        }

        if (!argsAsList.contains("-db")) {
            blastCommand.add("-db");
            blastCommand.add(Configuration.BLAST_DB_PATH);
        }

        LOGGER.log(Level.FINE, "Blast command: ''{0}''", String.join("' '", blastCommand));

        blastProcessBuiler = new ProcessBuilder(blastCommand);
        blastProcessBuiler.redirectError(ProcessBuilder.Redirect.INHERIT);

        if (hasOutputFormat) {
            createDir(Configuration.OUTPUT_DIR);

            this.outputDataWriter = new BufferedWriter(
                    openOutputWriter(String.format("%s%c%d.blastOutput",
                            Configuration.OUTPUT_DIR, File.separatorChar, PCJ.myId())));

            this.blastXmlParser = null;
        } else {
            createDir(Configuration.OUTPUT_DIR);

            PrintWriter localWriter = new PrintWriter(new BufferedWriter(
                    openOutputWriter(String.format("%s%c%d.txtResultFile",
                            Configuration.OUTPUT_DIR, File.separatorChar, PCJ.myId()))));

            PrintWriter globalWriter = new PrintWriter(new BufferedWriter(
                    openOutputWriter(String.format("%s%c%d.txtGlobalResultFile",
                            Configuration.OUTPUT_DIR, File.separatorChar, PCJ.myId()))));

            BlastXmlParser xmlParser = null;
            try {
                xmlParser = new BlastXmlParser(localWriter, globalWriter);
            } catch (JAXBException | SAXException ex) {
                LOGGER.log(Level.SEVERE, "Exception while creating BlastXmlParser", ex);
            }
            this.blastXmlParser = xmlParser;

            this.outputDataWriter = null;
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

                try {
                    executeBlast(value);
                } catch (IOException | InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, "Exception while executing BLAST.", ex);
                }

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

    private void executeBlast(String value) throws IOException, InterruptedException {
        long startTime = System.nanoTime();

        Process process = blastProcessBuiler.start();
        LOGGER.log(Level.FINE, "{0}: BLAST started", PCJ.myId());

        Thread outputParserThread;
        if (hasOutputFormat) {
            outputParserThread = new Thread(
                    () -> {
                        int len;
                        char[] bytes = new char[1024];
                        try (Reader inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                            while ((len = inputStream.read(bytes)) != -1) {
                                outputDataWriter.write(bytes, 0, len);
                                outputDataWriter.flush();
                            }
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, "Exception while writing to file", ex);
                        }
                    }
            );
        } else {
            outputParserThread = new Thread(
                    () -> {
                        try (InputStream inputStream = new BufferedInputStream(process.getInputStream())) {
                            blastXmlParser.processXmlFile(inputStream);
                            blastXmlParser.flush();
                        } catch (JAXBException | SAXException | IOException ex) {
                            LOGGER.log(Level.SEVERE, "Exception while processing XML file", ex);
                        }
                    }
            );
        }
        outputParserThread.start();

        try (OutputStream os = new BufferedOutputStream(process.getOutputStream())) {
            os.write(value.getBytes());
        }

        process.waitFor();

        outputParserThread.join();

        LOGGER.log(Level.INFO, "{0}: BLAST execution time: {1}", new Object[]{PCJ.myId(), (System.nanoTime() - startTime) / 1e9});
    }

}
