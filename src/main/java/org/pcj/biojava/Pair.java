/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.biojava;

/**
 * @author faramir
 */
public class Pair<T, V> {

    public final T first;
    public final V second;

    private Pair(T first, V second) {
        this.first = first;
        this.second = second;
    }

    public static <T, V> Pair<T, V> of(T first, V second) {
        return new Pair(first, second);
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}
