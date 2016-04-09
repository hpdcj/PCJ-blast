package org.pcj.blast;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.PCJ;

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Throwable {
        setLoggerLevel(Level.FINE);

        if (args.length > 0) {
            PCJ.start(BlastRunner.class, BlastRunner.class, args[0]);
        } else {
            System.err.println("File with nodes description required as parameter!");
            System.exit(1);
        }
    }

    private static void setLoggerLevel(Level level) throws SecurityException {
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(level);

        Logger logger = Logger.getLogger("");
        logger.addHandler(consoleHandler);
        logger.setLevel(level);
    }
}
