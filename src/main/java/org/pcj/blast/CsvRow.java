/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.blast;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author faramir
 */
public class CsvRow {
    private final Map<String, String> values;

    public CsvRow() {
        this.values = new HashMap<>();
    }
    
    public void set(String column, String value) {
        values.put(column, value);
    }
    
    public void set(String column, int value) {
        values.put(column, Integer.toString(value));
    }
    
    public void set(String column, double value) {
        values.put(column, Double.toString(value));
    }
    
    public String get(String column) {
        return values.get(column);
    }
}
