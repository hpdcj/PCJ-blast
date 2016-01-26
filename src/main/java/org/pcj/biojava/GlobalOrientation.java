/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.biojava;

/**
 *
 * @author faramir
 */
public enum GlobalOrientation {
    PLUS,
    MINUS,
    INVERTED,
    UNKNOWN;

    @Override
    public String toString() {
        switch (this) {
            case PLUS:
                return "Plus";
            case MINUS:
                return "Minus";
            case INVERTED:
                return "Inverted";
            default:
                return null;
        }
    }

}
