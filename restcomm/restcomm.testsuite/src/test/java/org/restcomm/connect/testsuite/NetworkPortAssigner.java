package org.restcomm.connect.testsuite;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;

/**
 * Allows integration tests to be run in parallel by assigning a free port.
 *
 * A port range will be assigned based in JVM PID, and a circular sequence
 * around this range will be provided. JVM PID are expected to be sequenced, so
 * incremental PID will be received. It is assumed that low number of forked
 * JMVs will be used to run parallel tests ( near cores in system), so the
 * PORT_RANGE dont provide much collisions.
 *
 */
public class NetworkPortAssigner {

    private static final Logger LOGGER = Logger.getLogger(NetworkPortAssigner.class);
    private static final int PORT_RANGE = 150;

    //provide ports above system reserved range 
    private static final int PORT_MAX_BASE = 65534;

    private static final int INITIAL_PORT_VALUE;

    private static final AtomicInteger PORT_SEQ = new AtomicInteger(0);

    private static final int PID;

    static {
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');

        if (index < 1) {
            // part before '@' empty (index = 0) / '@' not found (index = -1)
            PID = 0;
        } else {
            PID = Integer.parseInt(jvmName.substring(0, index));
        }

        INITIAL_PORT_VALUE = PORT_MAX_BASE - ((PID % PORT_RANGE) * PORT_RANGE);
        LOGGER.info("PID:" + PID);
        LOGGER.info("PID:" + PID + ",INITIAL_PORT_VALUE:" + INITIAL_PORT_VALUE);
    }

    public synchronized static int retrieveNextPort() {
        int nextPort = PORT_MAX_BASE;
        nextPort = retrieveNextPortByFile();
        LOGGER.info("PID:" + PID + ",nextPort:" + nextPort);
        return nextPort;
    }

    public static int retrieveNextPortBySeq() {
        int nextPort = INITIAL_PORT_VALUE - (PORT_SEQ.getAndAdd(1) % PORT_RANGE);
        return nextPort;
    }

    public static int retrieveNextPortByFile() {
        int nextPort = PORT_MAX_BASE;
        //assume maven convention, create file under target so its cleaned
        File portFile = new File("./target/portFile");
        try {
            if (!portFile.exists()) {
                portFile.createNewFile();
                LOGGER.info("PID:" + PID + ",portFile created");
            } else {
                LOGGER.info("PID:" + PID + ",portFile already exists");
            }
        } catch (IOException ex) {
            LOGGER.info("PID:" + PID + ", there is problem when creating portFile");
        }

        RandomAccessFile aFile = null;
        try {
            aFile = new RandomAccessFile(portFile, "rwd");
            FileChannel channel = aFile.getChannel();
            channel.lock();
            try {
                int readInt = aFile.readInt();
                nextPort = readInt - 1;
                if (nextPort <= 0) {
                    nextPort = PORT_MAX_BASE - 1;
                }
            } catch (EOFException eExp) {
                //file was empty, it was just created
                nextPort = PORT_MAX_BASE - 1;
                LOGGER.info("PID:" + PID + ",sequence resetted");
            }
            //rewrite existing value by resetting pointer to the start
            aFile.seek(0);
            aFile.writeInt(nextPort);
        } catch (Exception ex) {
            LOGGER.error("error getting port", ex);
        } finally {
            if (aFile != null) {
                try {
                    aFile.close();
                } catch (Exception e) {
                    LOGGER.error("error getting port", e);
                }
            }
        }
        return nextPort;
    }
}
