package org.pcj.biojava;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import ncbi.blast.result.generated.BlastOutput;
import ncbi.blast.result.generated.Hit;
import ncbi.blast.result.generated.Hsp;
import ncbi.blast.result.generated.Iteration;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Throwable {
        BlastOutput blastOutput = test();

        CsvFileWriter localWriter = new CsvFileWriter(new PrintWriter(System.out),
                new String[]{"Queryid",
                    "gi",
                    "identity",
                    "Coverage",
                    "Strain",
                    "alignment.length",
                    "Chimera",
                    "Strand",
                    "q.start",
                    "q.end",
                    "s.start",
                    "s.end",
                    "e.value",
                    "score",
                    "Length",
                    "Subject_Length",},
                '@');

        CsvFileWriter globalWriter = new CsvFileWriter(new PrintWriter(System.err),
                new String[]{"Queryid",
                    "gi",
                    "global_Coverage",
                    "global_q_start",
                    "global_q_end",
                    "global_total_lengh_of_gaps",
                    "global_gaps",
                    "global_chimera",
                    "global_orientation",},
                '\t');

        int length = Integer.parseInt(blastOutput.getBlastOutputQueryLen());
        for (Iteration iteration : blastOutput.getBlastOutputIterations().getIteration()) {
            String queryId = iteration.getIterationQueryDef();
            int queryLength = Integer.parseInt(iteration.getIterationQueryLen());
            for (Hit hit : iteration.getIterationHits().getHit()) {
                String subjectId = getSubjectId(hit.getHitId(), hit.getHitDef());
                String subjectDef = getSubjectDef(hit.getHitDef());
                int subjectLength = Integer.parseInt(hit.getHitLen());

                int globalMinQueryStart = Integer.MAX_VALUE;
                int globalMaxQueryEnd = Integer.MIN_VALUE;
                int globalTotalLenghOfGaps = 0;
                List<Pair<Integer, Integer>> gaps = new ArrayList<>();
                GlobalOrientation globalOrientation = GlobalOrientation.UNKNOWN;

                List<Double> globalIdentities = new ArrayList<>();

                for (Hsp hsp : hit.getHitHsps().getHsp()) {
                    int hspPositive = Integer.parseInt(hsp.getHspPositive());
                    int hspLength = Integer.parseInt(hsp.getHspAlignLen());
                    int subjectFrom = Integer.parseInt(hsp.getHspHitFrom());
                    int subjectTo = Integer.parseInt(hsp.getHspHitTo());
                    int queryStart = Integer.parseInt(hsp.getHspQueryFrom());
                    int queryEnd = Integer.parseInt(hsp.getHspQueryTo());
                    double eValue = Double.parseDouble(hsp.getHspEvalue());
                    double score = Double.parseDouble(hsp.getHspScore());

                    double identity = 100. * hspPositive / hspLength;
                    globalIdentities.add(identity);

                    int subjectStrand;
                    if (subjectFrom < subjectTo) {
                        subjectStrand = 1;
                    } else {
                        subjectStrand = -1;
                        int temp = subjectFrom;
                        subjectFrom = subjectTo;
                        subjectTo = temp;
                    }

                    if (queryEnd < queryStart) {
                        int temp = queryStart;
                        queryStart = queryEnd;
                        queryEnd = temp;
                    }
                    double coverage;
                    if (queryLength < subjectLength) {
                        coverage = 100. * (queryEnd - queryStart + 1) / queryLength;
                    } else {
                        coverage = 100. * (subjectTo - subjectFrom + 1) / subjectLength;
                    }

                    if (globalOrientation != GlobalOrientation.INVERTED) {
                        if (subjectStrand > 0) {
                            if (globalOrientation == GlobalOrientation.UNKNOWN) {
                                globalOrientation = GlobalOrientation.PLUS;
                            } else if (globalOrientation == GlobalOrientation.MINUS) {
                                globalOrientation = GlobalOrientation.INVERTED;
                            }
                        } else if (globalOrientation == GlobalOrientation.UNKNOWN) {
                            globalOrientation = GlobalOrientation.MINUS;
                        } else if (globalOrientation == GlobalOrientation.PLUS) {
                            globalOrientation = GlobalOrientation.INVERTED;
                        }
                    }

                    if (globalMinQueryStart > queryStart) {
                        globalMinQueryStart = queryStart;
                    }
                    if (globalMaxQueryEnd != Integer.MIN_VALUE && queryStart - globalMaxQueryEnd >= 1) {
                        int gapStart = globalMaxQueryEnd;
                        int gapEnd = queryStart;
                        gaps.add(Pair.of(gapStart, gapEnd));

                        globalTotalLenghOfGaps += gapEnd - gapStart;
                    }
                    if (globalMaxQueryEnd == Integer.MIN_VALUE || queryEnd > globalMaxQueryEnd) {
                        globalMaxQueryEnd = queryEnd;
                    }

                    CsvRow localRow = new CsvRow();

                    localRow.set("Queryid", queryId);
                    localRow.set("gi", subjectId);
                    localRow.set("identity", identity);
                    localRow.set("Coverage", coverage);
                    localRow.set("Strain", subjectDef);
                    localRow.set("alignment.length", hspLength);
                    localRow.set("Chimera", isChimera(hsp.getHspMidline()) ? "Yes" : "No");
                    localRow.set("Strand", subjectStrand);
                    localRow.set("q.start", queryStart);
                    localRow.set("q.end", queryEnd);
                    localRow.set("s.start", subjectFrom);
                    localRow.set("s.end", subjectTo);
                    localRow.set("e.value", eValue);
                    localRow.set("score", score);
                    localRow.set("Length", length);
                    localRow.set("Subject_Length", subjectLength);

                    localWriter.write(localRow);
                }

                CsvRow globalRow = new CsvRow();
                globalRow.set("Queryid", queryId);
                globalRow.set("gi", subjectId);
                globalRow.set("global_Coverage", 100. * ((globalMaxQueryEnd - globalMinQueryStart) - globalTotalLenghOfGaps) / length);
                globalRow.set("global_q_start", globalMinQueryStart);
                globalRow.set("global_q_end", globalMaxQueryEnd);
                globalRow.set("global_total_lengh_of_gaps", globalTotalLenghOfGaps);
                globalRow.set("global_gaps", gaps.toString());
                globalRow.set("global_chimera", isGlobalChimera(globalIdentities) ? "Yes" : "No");
                globalRow.set("global_orientation", globalOrientation.toString());

                globalWriter.write(globalRow);
            }
        }
        globalWriter.close();

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
                (publicId, systemId) -> {
                    return Stream.of("NCBI_BlastOutput.dtd", "NCBI_Entity.mod.dtd", "NCBI_BlastOutput.mod.dtd")
                    .filter(file -> systemId.contains(file))
                    .findFirst()
                    .map(file -> new InputSource(BlastOutput.class.getResourceAsStream("/dtd/" + file)))
                    .orElse(null);
                });
        InputSource input = new InputSource(new FileReader(new File("blast-test.xml")));
        Source source = new SAXSource(xmlReader, input);
        return (BlastOutput) u.unmarshal(source);
    }

    private static boolean isChimera(String hspMidline) {
        double minValue = Double.MAX_VALUE;
        double maxValue = Double.MIN_VALUE;
        int matchLength = hspMidline.length();
        int parts = 3;
        int partLength = matchLength / parts;

        for (int i = 0; i < matchLength; i += partLength) {
            int currentPartLength = Math.min(matchLength, i + partLength) - i;
            if (currentPartLength >= (double) partLength / 3) {
                int sum = 0;
                for (int j = i; j < i + currentPartLength; ++j) {
                    if (hspMidline.charAt(j) == '|') {
                        ++sum;
                    }
                }
                double identity = 100.0 * sum / currentPartLength;
                if (identity > maxValue) {
                    maxValue = identity;
                }
                if (identity < minValue) {
                    minValue = identity;
                }
            }
        }
        return maxValue >= 90 && minValue < 90 && maxValue - minValue >= 5;
    }

    private static String getSubjectId(String hitId, String hitDef) {
        String[] splitId = hitId.split("\\|");
        if (splitId.length > 1) {
            if ("BL_ORD_ID".equals(splitId[1])) {
                String[] splitDef = hitDef.split("\\|");
                if (splitDef.length > 1 && "gi".equals(splitDef[0])) {
                    return splitDef[1];
                } else {
                    return splitDef[0];
                }
            } else {
                return splitId[1];
            }
        }
        return "<unknown id>";
    }

    private static String getSubjectDef(String hitDef) {
        String subjectDef;
        if (hitDef.split(" ").length > 1) {
            subjectDef = hitDef;
        } else {
            subjectDef = "<unknown description>";
        }
        return subjectDef.replace("@", "").replace("#", ""); // @ is used for splitting columns, # for comments for R
    }

    private static boolean isGlobalChimera(List<Double> globalIdentities) {
        double minValue = globalIdentities.stream().min(Double::compare).get();
        double maxValue = globalIdentities.stream().max(Double::compare).get();
        return maxValue >= 90 && minValue < 90 && maxValue - minValue >= 5;
    }
}
