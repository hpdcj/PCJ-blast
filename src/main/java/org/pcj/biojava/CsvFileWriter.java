/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.biojava;

import java.io.PrintWriter;

/**
 *
 * @author faramir
 */
public class CsvFileWriter implements AutoCloseable {
    
    private final PrintWriter output;
    private final String[] columns;
    private final char delimiter;
    
    public CsvFileWriter(PrintWriter output, String[] columns, char delimiter) {
        if (columns.length < 1) {
            throw new IllegalArgumentException("Array columns must not be empty");
        }
        this.output = output;
        this.columns = columns;
        this.delimiter = delimiter;
    }
    
    public void write(CsvRow row) {
        int colNo = 0;
        for (String column : columns) {
            if (colNo > 0) {
                output.write(delimiter);
            }
            output.write(row.get(column));
            ++colNo;
        }
        output.write(System.lineSeparator());
    }

    @Override
    public void close() {
        output.flush();
    }
}
