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

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import ncbi.blast.result.generated.BlastOutput;
import ncbi.blast.result.generated.Hit;
import ncbi.blast.result.generated.Hsp;
import ncbi.blast.result.generated.Iteration;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * @author Marek Nowicki
 */
public class BlastXmlParser implements AutoCloseable {

    private final CsvFileWriter localCsvWriter;
    private final CsvFileWriter globalCsvWriter;
    private final XMLReader xmlReader;
    private final Unmarshaller unmarshallel;

    public BlastXmlParser(Writer localWriter, Writer globalWriter) throws JAXBException, SAXException {
        this.localCsvWriter = new CsvFileWriter(localWriter,
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

        this.globalCsvWriter = new CsvFileWriter(globalWriter,
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

        JAXBContext jc = JAXBContext.newInstance(BlastOutput.class);
        unmarshallel = jc.createUnmarshaller();

        xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
        xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
        xmlReader.setEntityResolver(
                (publicId, systemId)
                -> Stream.of("NCBI_BlastOutput.dtd", "NCBI_Entity.mod.dtd", "NCBI_BlastOutput.mod.dtd")
                .filter(file -> systemId.contains(file))
                .findFirst()
                .map(file -> new InputSource(BlastOutput.class.getResourceAsStream("/dtd/" + file)))
                .orElse(null));
    }

    public void processXmlFile(InputStream inputStream) throws JAXBException, SAXException, IOException {
        BlastOutput blastOutput = readBlastXmlFile(inputStream);
        generateRdata(blastOutput);
    }

    public void flush() throws IOException {
        localCsvWriter.flush();
        globalCsvWriter.flush();
    }

    @Override
    public void close() throws IOException {
        localCsvWriter.close();
        globalCsvWriter.close();
    }

    private void generateRdata(BlastOutput blastOutput) throws IOException {
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

                    localCsvWriter.write(localRow);
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

                globalCsvWriter.write(globalRow);
            }
        }
    }

    private BlastOutput readBlastXmlFile(InputStream inputStream) throws JAXBException {
        InputSource inputSource = new InputSource(inputStream);
        Source source = new SAXSource(xmlReader, inputSource);
        return (BlastOutput) unmarshallel.unmarshal(source);
    }

    private boolean isChimera(String hspMidline) {
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

    private String getSubjectDef(String hitDef) {
        String subjectDef;
        if (hitDef.split(" ").length > 1) {
            subjectDef = hitDef;
        } else {
            subjectDef = "<unknown description>";
        }
        return subjectDef.replace("@", "").replace("#", ""); // @ is used for splitting columns, # for comments for R
    }

    private boolean isGlobalChimera(List<Double> globalIdentities) {
        double minValue = globalIdentities.stream().min(Double::compare).get();
        double maxValue = globalIdentities.stream().max(Double::compare).get();
        return maxValue >= 90 && minValue < 90 && maxValue - minValue >= 5;
    }
}
