package org.pcj.blast;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.NodesDescription;
import org.pcj.PCJ;

public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        setLoggerLevel(Level.FINE);

        File sequenceFile = new File(Configuration.INPUT_FILENAME);
        if (sequenceFile.isFile() == false) {
            System.err.println("File with sequence does not exists: " + Configuration.INPUT_FILENAME);
            System.exit(1);
        }

        File nodesFile = new File(Configuration.NODES_FILENAME);
        if (nodesFile.isFile() == false) {
            System.err.println("Nodes description file does not exists: " + Configuration.NODES_FILENAME);
            System.exit(1);
        }

        BlastRunner.args = args;
        PCJ.start(BlastRunner.class, new NodesDescription(nodesFile));
    }
    
    private static void setLoggerLevel(Level level) throws SecurityException {
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(level);

        Logger logger = Logger.getLogger("");
        logger.addHandler(consoleHandler);
        logger.setLevel(level);
    }
}
