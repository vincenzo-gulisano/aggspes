package util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CountStat {

    private final PrintWriter out;
    private final boolean reset;
    private long count;
    private long prevSec;

    public CountStat(String outputFile, boolean autoFlush, boolean reset) {
        this.count = 0;

        FileWriter outFile;
        try {
            outFile = new FileWriter(outputFile);
            out = new PrintWriter(outFile, autoFlush);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        prevSec = System.currentTimeMillis() / 1000;

        this.reset = reset;

    }

    public CountStat(String outputFile, boolean autoFlush) {
        this(outputFile, autoFlush, true);
    }

    public void increase(long v) {
        increase(v, System.currentTimeMillis() / 1000);
    }

    private void increase(long v, long thisSec) {
        while (prevSec < thisSec) {
            out.println(prevSec + "," + count);
            if (reset) {
                count = 0;
            }
            prevSec++;
        }
        count += v;
    }

    public void close() {
        long thisSec = System.currentTimeMillis() / 1000;
        while (prevSec <= thisSec) {
            out.println(prevSec + "," + count);
            count = 0;
            prevSec++;
        }
        out.flush();
        out.close();
    }
}