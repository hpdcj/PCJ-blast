package org.pcj.biojava;


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
public class ReadFile extends Storage implements StartPoint {

    final private String filename;
    final private String output;
    final private String dbPath;
    final private int linesToSendCount;
    final private int blastThreadCount;

    @Shared
    private String[] values;

    @Shared
    private int[] readIndex;

    {
        try {
            values = new String[Integer.parseInt(System.getProperty("buffer", "3"))];
        } catch (NumberFormatException ex) {
            values = new String[3];
        }

        int linesCount = 2;
        try {
            linesCount = Integer.parseInt(System.getProperty("linesCount", "2"));
        } catch (NumberFormatException ex) {
        }
        linesToSendCount = linesCount;

        int blastThreads = 1;
        try {
            blastThreads = Integer.parseInt(System.getProperty("blastThreads", "1"));
        } catch (NumberFormatException ex) {
        }
        blastThreadCount = blastThreads;

        dbPath = System.getProperty("db", "/icm/hydra/software/plgrid/blast/dbs/nt");
        filename = System.getProperty("input", "davit-sequence-file-20151116.fasta");
        output = System.getProperty("output", "stdout");
    }


    @Override
    public void main() throws Throwable {
        if (PCJ.myId() == 0) {
            readIndex = new int[PCJ.threadCount()];
            Arrays.fill(readIndex, values.length - 1);

            int[] writeIndex = new int[PCJ.threadCount()];

            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                String line;
                StringBuilder sb = new StringBuilder();
                int threadNo = 0;
                for (int lineNo = 1; (line = br.readLine()) != null; ++lineNo) {
                    sb.append(line).append(System.lineSeparator());
                    if (lineNo % linesToSendCount == 0) {
                        threadNo = returnAvailableThreadOrWait(threadNo, writeIndex);
                        String value = sb.toString();
                        System.err.println("send to: " + threadNo + "[" + writeIndex[threadNo] + "] >>> " + value.substring(0, Math.min(value.length(), 60)) + " (" + value.length() + ")");
                        PCJ.put(threadNo, "values", value, writeIndex[threadNo]);
                        writeIndex[threadNo] = (writeIndex[threadNo] + 1) % values.length;
                        sb.setLength(0);
                    }
                }
                if (sb.length() > 0) {
                    threadNo = returnAvailableThreadOrWait(threadNo, writeIndex);
                    String value = sb.toString();
                    System.err.println("send to: " + threadNo + "[" + writeIndex[threadNo] + "] >>> " + value.substring(0, Math.min(value.length(), 60)) + " (" + value.length() + ")");
                    PCJ.put(threadNo, "values", value, writeIndex[threadNo]);
                    writeIndex[threadNo] = (writeIndex[threadNo] + 1) % values.length;
                }
            }

            // finish by sending null to the read cell of array
            for (int newThreadNo = 1; newThreadNo < PCJ.threadCount(); ++newThreadNo) {
                while (true) {
                    if (writeIndex[newThreadNo] != readIndex[newThreadNo]) {
                        break;
                    } else {
                        PCJ.waitFor("readIndex");
                    }
                }

                PCJ.put(newThreadNo, "values", null, writeIndex[newThreadNo]);
            }
        } else {
//            XmlMerger xmlMerger = null;
            for (int blockNo = 0;; blockNo++) {
                PCJ.waitFor("values");
                int index = blockNo % values.length;
                String value = PCJ.getLocal("values", index);
                if (value == null) {
                    System.out.println(PCJ.myId() + ": finished");
//                    if (xmlMerger != null) {
//                        try (Writer output = new BufferedWriter(new FileWriter("blastOut_" + PCJ.myId() + ".xml"))) {
//                            output.write(xmlMerger.generateXml());
//                        }
//                    }
                    return;
                }
                if (blockNo == 0) {
                    long time = (long) (PCJ.myId() * 10);//(long)Math.random()*300;
                    System.out.println(PCJ.myId() + ": sleep by: " + time+ "s");
                    Thread.sleep(time * 1000);
                }
                
                PCJ.put(0, "readIndex", index % values.length, PCJ.myId());
                // process value
                System.err.println(PCJ.myId() + ": received: " + value.substring(0, Math.min(value.length(), 60)) + " (" + value.length() + ")");

                // http://www.ncbi.nlm.nih.gov/books/NBK279675/
                // /icm/hydra/software/plgrid/blast/ncbi-blast-2.2.28+/bin/blastn -word_size 11 -gapopen 0 -gapextend 2 -penalty -1 -reward 1 -max_target_seqs 10 -evalue 0.001 -show_gis -outfmt 5 -db /icm/hydra/software/plgrid/blast/dbs/nt
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
                        "-out", "stdout".equals(output) ? output : (output + "_" + PCJ.myId() + "_" + blockNo + ".xml"),
                        "-num_threads", "" + blastThreadCount,
                        "-db", dbPath
                ); //"/icm/hydra/software/plgrid/blast/dbs/nt"
                processBuiler.redirectError(ProcessBuilder.Redirect.appendTo(new File("outxml/err" + PCJ.myId() + ".txt")));
                processBuiler.redirectOutput(ProcessBuilder.Redirect.appendTo(new File("outxml/out" + PCJ.myId() + ".txt")));

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

//                startTime = System.nanoTime();
//                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//                if (xmlMerger == null) {
//                    xmlMerger = new XmlMerger(bais);
//                } else {
//                    xmlMerger.addXml(bais);
//                }
//                System.out.println(PCJ.myId() + ": XML merging execution time: " + (System.nanoTime() - startTime) / 1e9);
            }
        }
    }

    private int returnAvailableThreadOrWait(int threadNo, int[] writeIndex) {
        // find available cell in one of the threads
        while (true) {
            for (int i = 0; i < PCJ.threadCount(); ++i) {
                int newThreadNo = (threadNo + i) % (PCJ.threadCount() - 1) + 1;
                if (writeIndex[newThreadNo] != readIndex[newThreadNo]) {
                    return newThreadNo;
                }
            }
            PCJ.waitFor("readIndex");
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

class XmlMerger {

    private final DocumentBuilder documentBuilder;
    private final Document toDocument;

    public XmlMerger(InputStream initialStream) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
        toDocument = documentBuilder.parse(initialStream);
    }

    public void addXml(InputStream additionalStream) throws SAXException, IOException {
        Document fromDocument = documentBuilder.parse(additionalStream);
        NodeList toIterationsList = toDocument.getElementsByTagName("BlastOutput_iterations");
        Node toIterations = toIterationsList.item(0);
        NodeList fromIterations = fromDocument.getElementsByTagName("Iteration");
        for (int i = 0; i < fromIterations.getLength(); ++i) {
            Node iteration = toDocument.importNode(fromIterations.item(i), true);
            toIterations.appendChild(iteration);
        }
    }

    public String generateXml() throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//NCBI//NCBI BlastOutput/EN");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.ncbi.nlm.nih.gov/dtd/NCBI_BlastOutput.dtd");

        DOMSource source = new DOMSource(toDocument);
        StreamResult result = new StreamResult(new StringWriter());
        transformer.transform(source, result);

        return result.getWriter().toString();

    }
}
