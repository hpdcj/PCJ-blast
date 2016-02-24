package org.pcj.blast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.pcj.PCJ;
import org.pcj.StartPoint;
import org.pcj.Storage;
import org.xml.sax.SAXException;

public class ProcessJoinedXmlFilesMain extends Storage implements StartPoint {

    final private String outxmlDirPath;
    final private String outxmlFileExtension;

    {
        // /mnt/unicore/user-space/304f32e6-a792-47ab-9900-eb39339b7ddc/outxml/outXXX.xmlout - 64n4t7b
        // /mnt/unicore/user-space/1b2f1d17-dd30-45f6-97af-60654c76eac4/ - 16n4t7b
        outxmlDirPath = System.getProperty("outxml.dir", "/mnt/unicore/user-space/1b2f1d17-dd30-45f6-97af-60654c76eac4/");
        outxmlFileExtension = System.getProperty("outxml.ext", ".xmlout");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Throwable {
        if (args.length > 0) {
            PCJ.start(ProcessJoinedXmlFilesMain.class, ProcessJoinedXmlFilesMain.class, args[0]);
        } else {
            PCJ.start(ProcessJoinedXmlFilesMain.class, ProcessJoinedXmlFilesMain.class,
                    new String[]{"localhost", "localhost"});
        }
    }

    public void main() {
        File[] outxmlFiles = Paths.get(outxmlDirPath).toFile().listFiles(
                (name) -> name.isFile() && name.getName().endsWith(outxmlFileExtension)
        );

        if (outxmlFiles == null) {
            System.err.printf("%d: outxml directory path (%s) is invalid (doesn't exists?)%n", PCJ.myId(), outxmlDirPath);
            return;
        }
        System.out.printf("%d: found %d files with extension%n", PCJ.myId(), outxmlFiles.length, outxmlFileExtension);
        
        Arrays.sort(outxmlFiles);

        long globalStartTime = System.nanoTime();
        try (PrintWriter localWriter = new PrintWriter(new BufferedWriter(new FileWriter(String.format("%d.txtResultFile", PCJ.myId()))));
                PrintWriter globalWriter = new PrintWriter(new BufferedWriter(new FileWriter(String.format("%d.txtGlobalResultFile", PCJ.myId()))))) {

            for (int i = PCJ.myId(); i < outxmlFiles.length; i += PCJ.threadCount()) {
                System.out.printf("%d: processing file %s%n", PCJ.myId(), outxmlFiles[i]);
                long fileStartTime = System.nanoTime();
                XmlSplitter xmlSplitter = new XmlSplitter(new FileReader(outxmlFiles[i]));
                for (int file = 1;; ++file) {
                    Reader reader = xmlSplitter.nextXml();
                    if (reader == null) {
                        break;
                    }

                    System.out.printf("%d: processing part %d%n", PCJ.myId(), file);
                    long partStartTime = System.nanoTime();
                    try {
                        BlastXmlParser.processXmlFile(reader, localWriter, globalWriter);
                    } catch (IOException | JAXBException | SAXException ex) {
                        Logger.getLogger(ProcessJoinedXmlFilesMain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    System.out.printf("%d: part processing time: %.7f%n", PCJ.myId(), (System.nanoTime() - partStartTime) / 1e9);
                }
                System.out.printf("%d: file processing time: %.7f%n", PCJ.myId(), (System.nanoTime() - fileStartTime) / 1e9);
            }
        } catch (IOException ex) {
            Logger.getLogger(ProcessJoinedXmlFilesMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.printf("%d: Total execution time: %.7f%n", PCJ.myId(), (System.nanoTime() - globalStartTime) / 1e9);
    }
}
