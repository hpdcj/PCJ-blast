package org.pcj.biojava;

import java.io.File;
import java.io.FileReader;
import java.util.stream.Stream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import ncbi.blast.result.generated.BlastOutput;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Throwable {

        BlastOutput blastOutput = test();
        System.out.println("" + blastOutput.getBlastOutputDb());

//        if (args.length > 0) {
//            PCJ.start(ReadFile.class, ReadFile.class, args[0]);
//        } else {
//            PCJ.start(ReadFile.class, ReadFile.class,
//                    new String[]{"localhost", "localhost"});
//        }
    }

    private static BlastOutput test() throws Throwable {
        JAXBContext jc = JAXBContext.newInstance(BlastOutput.class);
        Unmarshaller u = jc.createUnmarshaller();

        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
        xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
        xmlReader.setEntityResolver(
                (publicId, systemId)
                -> Stream.of("NCBI_BlastOutput.dtd", "NCBI_Entity.mod.dtd", "NCBI_BlastOutput.mod.dtd")
                .filter(file -> systemId.contains(file))
                .findFirst()
                .map(file -> new InputSource(BlastOutput.class.getResourceAsStream("/dtd/" + file)))
                .orElse(null));
        InputSource input = new InputSource(new FileReader(new File("blast1.xml")));
        Source source = new SAXSource(xmlReader, input);
        return (BlastOutput) u.unmarshal(source);
    }
}
