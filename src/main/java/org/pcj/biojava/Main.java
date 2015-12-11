package org.pcj.biojava;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import ncbi.blast.result.generated.BlastOutput;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Throwable {

        BlastOutput blastOutput = test();

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

        XMLReader xmlreader = XMLReaderFactory.createXMLReader();
        xmlreader.setFeature("http://xml.org/sax/features/namespaces", true);
        xmlreader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
        xmlreader.setEntityResolver(new EntityResolver() {
            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                String file = null;
                if (systemId.contains("NCBI_BlastOutput.dtd")) {
                    file = "/dtd/NCBI_BlastOutput.dtd";
                }
                if (systemId.contains("NCBI_Entity.mod.dtd")) {
                    file = "/dtd/NCBI_Entity.mod.dtd";
                }
                if (systemId.contains("NCBI_BlastOutput.mod.dtd")) {
                    file = "/dtd/NCBI_BlastOutput.mod.dtd";
                }
                return new InputSource(BlastOutput.class.getResourceAsStream(file));
            }
        });
        InputSource input = new InputSource(new FileReader(new File("blast1.xml")));
        Source source = new SAXSource(xmlreader, input);
        return (BlastOutput) u.unmarshal(source);
    }
}
