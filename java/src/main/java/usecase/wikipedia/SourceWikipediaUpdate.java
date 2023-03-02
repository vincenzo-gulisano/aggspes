package usecase.wikipedia;

import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;

import util.CountStat;

public class SourceWikipediaUpdate {

    public static SingleOutputStreamOperator<TupleWikipediaUpdate> createTimestampStringTupleSourceForPerformanceStudy(
            StreamExecutionEnvironment env, String inputFile, String statFile, long maxAllowedLateness, long duration,
            int batchSize, long sleepPeriod) {
        return createTimestampStringTupleSourceForPerformanceStudy(env, inputFile, statFile, maxAllowedLateness,
                duration, batchSize, sleepPeriod, 10 * 1000000000L, 1);
    }

    public static SingleOutputStreamOperator<TupleWikipediaUpdate> createTimestampStringTupleSourceForPerformanceStudy(
            StreamExecutionEnvironment env, String inputFile, String statFile, long maxAllowedLateness, long duration,
            int batchSize, long sleepPeriod, long maxIdleTime, int maxIdleTimeOccurences) {

        return env.addSource(new RichSourceFunction<TupleWikipediaUpdate>() {

            private CountStat log;
            private ConcurrentLinkedQueue<TupleWikipediaUpdate> internalQueue;
            private boolean internalThreadCompleted=false;

            @Override
            public void open(Configuration parameters) throws Exception {
                log = new CountStat(statFile, true);
                internalQueue = new ConcurrentLinkedQueue<>();
            }

            @Override
            public void close() throws Exception {
                log.close();
            }

            private void fillInternalQueue(String inputFile) throws IOException {

                BufferedReader inputFileReader = new BufferedReader(new FileReader(inputFile));
                String line = inputFileReader.readLine();
                long startTime = System.currentTimeMillis();

                while (line != null && System.currentTimeMillis() - startTime <= duration) {

                    long beforeBatchTime = System.nanoTime();
                    for (int j = 0; j < batchSize; j++) {

                        String[] lineTokens = line.split("\\t");
                        internalQueue.add(new TupleWikipediaUpdate(System.currentTimeMillis(), lineTokens[0], lineTokens[1],
                                lineTokens[2], System.currentTimeMillis()));
                        log.increase(1);
                    }

                    while (System.nanoTime() < beforeBatchTime + sleepPeriod) {
                    }

                    line = inputFileReader.readLine();
                }
                inputFileReader.close();  
                internalThreadCompleted = true;  
                
            }

            @Override
            public void run(SourceContext<TupleWikipediaUpdate> ctx) throws Exception {

                new Thread(() -> {
                    try {
                        fillInternalQueue(inputFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                while (!internalThreadCompleted) {
                    if (!internalQueue.isEmpty()) {
                        ctx.collect(internalQueue.poll());
                    }
                }

            }

            @Override
            public void cancel() {
            }

        }).assignTimestampsAndWatermarks(
                WatermarkStrategy.<TupleWikipediaUpdate>forMonotonousTimestamps().withIdleness(Duration.ofSeconds(5))
                        .withTimestampAssigner((event, timestamp) -> event.f0));

    }

}
