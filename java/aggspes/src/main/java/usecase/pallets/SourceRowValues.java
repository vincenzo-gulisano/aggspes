package usecase.pallets;

import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;

import util.CountStat;

public class SourceRowValues {

    public static SingleOutputStreamOperator<TupleRowValues> createTimestampStringTupleSourceForPerformanceStudy(
            StreamExecutionEnvironment env, String inputDir, String statFile, long duration,
            int batchSize, long sleepPeriod) {
        return createTimestampStringTupleSourceForPerformanceStudy(env, inputDir, statFile,
                duration, batchSize, sleepPeriod, 10 * 1000000000L, 1);
    }

    public static SingleOutputStreamOperator<TupleRowValues> createTimestampStringTupleSourceForPerformanceStudy(
            StreamExecutionEnvironment env, String inputDir, String statFile, long duration,
            int batchSize, long sleepPeriod, long maxIdleTime, int maxIdleTimeOccurences) {

        return env.addSource(new RichSourceFunction<TupleRowValues>() {

            private CountStat log;
            private ConcurrentLinkedQueue<TupleRowValues> internalQueue;
            private boolean internalThreadCompleted = false;

            @Override
            public void open(Configuration parameters) throws Exception {
                log = new CountStat(statFile, true);
                internalQueue = new ConcurrentLinkedQueue<>();
            }

            @Override
            public void close() throws Exception {
                log.close();
            }

            private void fillInternalQueue(String inputDir) throws IOException {

                // Preload the data
                ArrayList<ArrayList<Double>> rows = new ArrayList<>();
                PrepareInputData.findFiles(new File(inputDir), ".txt", rows);

                long startTime = System.currentTimeMillis();
                int rowIndex = 0;
                int tupleIndex = 0;

                while (System.currentTimeMillis() - startTime <= duration) {

                    long beforeBatchTime = System.nanoTime();
                    for (int j = 0; j < batchSize; j++) {

                        internalQueue.add(new TupleRowValues(System.currentTimeMillis(), tupleIndex, rows.get(rowIndex),
                                System.currentTimeMillis()));
                        log.increase(1);
                        rowIndex = (rowIndex + 1) % rows.size();
                        tupleIndex++;
                    }

                    while (System.nanoTime() < beforeBatchTime + sleepPeriod) {
                    }

                }
                internalThreadCompleted = true;

            }

            @Override
            public void run(SourceContext<TupleRowValues> ctx) throws Exception {

                new Thread(() -> {
                    try {
                        fillInternalQueue(inputDir);
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
                WatermarkStrategy.<TupleRowValues>forMonotonousTimestamps().withIdleness(Duration.ofSeconds(5))
                        .withTimestampAssigner((event, timestamp) -> event.f0));

    }

}
