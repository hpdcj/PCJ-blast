package org.pcj.blast;

import org.pcj.PCJ;

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Throwable {
        if (args.length > 0) {
            PCJ.start(BlastRunner.class, BlastRunner.class, args[0]);
        } else {
            PCJ.start(BlastRunner.class, BlastRunner.class,
                    new String[]{"localhost", "localhost"});
        }
    }
}
