package org.pcj.biojava;

import org.pcj.PCJ;

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            PCJ.start(ReadFile.class, ReadFile.class, args[0]);
        } else {
            PCJ.start(ReadFile.class, ReadFile.class,
                    new String[]{"localhost", "localhost"});
        }
    }
}
