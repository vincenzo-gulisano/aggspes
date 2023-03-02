package usecase.wikipedia;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.configuration.Configuration;

import util.AvgStat;
import util.CountStat;
import util.TimestampedTuple;

public class SinkLatencyRateLogger {

    public static <T extends TimestampedTuple> void createTimestampStringTupleSinkPerformance(DataStream<T> s, String outputFileLatency, String outputFileRate) {
        createTimestampStringTupleSinkPerformance(s, outputFileLatency, outputFileRate, 50000, 50);
    }

    
    public static <T extends TimestampedTuple> void createTimestampStringTupleSinkPerformance(DataStream<T> s, String outputFileLatency, String outputFileRate, long maxLatencyValue, int latencyViolationMaxOccurences) {

        // This sink will throw an exception if a latency higher than a threshold is observed consecutively more then a given number of times

        s.addSink(new RichSinkFunction<T>() {

            private AvgStat logLatency;
            private CountStat logRate;
            private int observedViolations = 0;

            @Override
            public void open(Configuration parameters) throws Exception {
                logLatency = new AvgStat(outputFileLatency, true);
                logRate = new CountStat(outputFileRate, true);
            }

            @Override
            public void close() throws Exception {
                logLatency.close();
                logRate.close();
            }

            @Override
            public void invoke(T value, Context context) throws Exception {
                if (logLatency.add(System.currentTimeMillis() - value.getStimulus())>=maxLatencyValue) {
                    observedViolations++;
                    if (observedViolations>=latencyViolationMaxOccurences) {
                        System.out.println("Max latency violated");
                        System.out.println("The sink has observed the max latency "+maxLatencyValue+" being violated at least "+observedViolations+ " times! Terminating!");
                        System.exit(13);
                    }
                }
                logRate.increase(1);
            }

        }).name("sink");

    }
    
}
