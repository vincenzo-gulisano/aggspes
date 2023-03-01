package usecase.pallets;

import java.io.Serializable;

import org.apache.flink.api.common.functions.FlatJoinFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

import usecase.ExperimentID;
import usecase.wikipedia.SinkLatencyRateLogger;
import util.CountStat;
import util.ImplementationType;
import util.Instrumenter;
import util.Instrumenter.SerializableLambdaJoin;
import util.Instrumenter.SerializableLambdaKeySelector;
import util.T3;

class TupleMatcher implements Serializable {

        public boolean match(TupleRowValues t1, TupleRowValues t2, ExperimentID expID) {
                double distSum = 0;
                for (int i = 0; i < t1.f2.size()/10; i++) {
                        distSum += Math.abs(t1.f2.get(i) - t2.f2.get(i));
                }
                return distSum > 0 && distSum  <= JoinParameters.getMaxAverageDistance(expID);
        }

}

public class QueryJoin {

        static CountStat throughputStat;

        public static void main(String[] args) throws Exception {

                // Managing parameters (here and only here)
                ParameterTool parameter = ParameterTool.fromArgs(args);

                int maxLatency = parameter.getInt("maxLatency", 20000);
                int maxLatencyViolations = parameter.getInt("maxLatencyViolations", 10);

                // The following pararmeters are option and used for performance experiments
                String injectionRateOutputFile1 = parameter.get("injectionRateOutputFile");
                String throughputOutputFile1 = parameter.get("throughputOutputFile");
                String latencyOutputFile = parameter.get("latencyOutputFile");
                String outputRateOutputFile = parameter.get("outputRateOutputFile");
                long duration = parameter.getLong("duration");
                int batchSize = parameter.getInt("batchSize");
                long sleepTime = parameter.getLong("sleepTime");

                // Environment setup
                StreamExecutionEnvironment env = StreamExecutionEnvironment
                                .getExecutionEnvironment();
                env.setParallelism(1);

                Instrumenter inst = new Instrumenter();

                SingleOutputStreamOperator<TupleRowValues> sourceStream1 = SourceRowValues
                                .createTimestampStringTupleSourceForPerformanceStudy(env,
                                                parameter.get("inputDir"), injectionRateOutputFile1, duration,
                                                batchSize, sleepTime);

                // Make sure a chain starts here
                sourceStream1.startNewChain();

                DataStream<TupleRowValuesIDs> joinStream = null;
                // System.out.println(ImplementationType.valueOf(parameter.get("implementationType")));
                switch (ImplementationType.valueOf(parameter.get("implementationType"))) {
                        case NATIVE:
                                joinStream = sourceStream1.join(sourceStream1).where(t -> 0)
                                                .equalTo(t -> 0)
                                                .window(SlidingEventTimeWindows.of(
                                                                Time.milliseconds(JoinParameters
                                                                                .getWS(ExperimentID.valueOf(parameter
                                                                                                .get("experimentID")))),
                                                                Time.milliseconds(JoinParameters.getWA(
                                                                                ExperimentID.valueOf(parameter.get(
                                                                                                "experimentID"))))))
                                                .apply(new FlatJoinFunction<TupleRowValues, TupleRowValues, TupleRowValuesIDs>() {

                                                        TupleMatcher tm = new TupleMatcher();

                                                        @Override
                                                        public void join(TupleRowValues first,
                                                                        TupleRowValues second,
                                                                        Collector<TupleRowValuesIDs> out)
                                                                        throws Exception {

                                                                QueryJoin.throughputStat.increase(1);
                                                                if (tm.match(first, second, ExperimentID.valueOf(
                                                                                parameter.get("experimentID")))) {
                                                                        out.collect(new TupleRowValuesIDs(first,second));
                                                                }
                                                        }

                                                });
                                break;
                        case SINGLEOUT:
                                joinStream = inst
                                                .<TupleRowValues, TupleRowValues, TupleRowValuesIDs>addAggBasedJoinSingleOut(
                                                                sourceStream1, sourceStream1,
                                                                new SerializableLambdaKeySelector<TupleRowValues, TupleRowValues>() {

                                                                        // For now returning always the same value, so
                                                                        // basically no
                                                                        // parallelism.
                                                                        // Can fix later
                                                                        @Override
                                                                        public Integer apply(
                                                                                        T3<TupleRowValues, TupleRowValues> t) {
                                                                                return 0;
                                                                        }

                                                                },
                                                                new SerializableLambdaJoin<TupleRowValues, TupleRowValues, TupleRowValuesIDs>() {

                                                                        TupleMatcher tm = new TupleMatcher();

                                                                        @Override
                                                                        public TupleRowValuesIDs apply(
                                                                                        TupleRowValues first,
                                                                                        TupleRowValues second) {

                                                                                // System.out.println("Comparing
                                                                                // "+first+" - "+second);
                                                                                QueryJoin.throughputStat.increase(1);
                                                                                if (tm.match(first, second, ExperimentID
                                                                                                .valueOf(parameter.get(
                                                                                                                "experimentID")))) {
                                                                                        return new TupleRowValuesIDs(first,second);
                                                                                }
                                                                                return null;
                                                                        }

                                                                },
                                                                Time.milliseconds(JoinParameters
                                                                                .getWA(ExperimentID.valueOf(parameter
                                                                                                .get("experimentID")))),
                                                                Time.milliseconds(JoinParameters
                                                                                .getWS(ExperimentID.valueOf(parameter
                                                                                                .get("experimentID")))),
                                                                Time.milliseconds(parameter.getInt("allowedLateness",
                                                                                Integer.MAX_VALUE)),
                                                                new TypeFactoryTupleRowValues2(),
                                                                new TypeFactoryTupleRowValuesIDs1());
                                break;
                        case MULTIOUT:
                                joinStream = inst
                                                .<TupleRowValues, TupleRowValues, TupleRowValuesIDs>addAggBasedJoinMultiOut(
                                                                sourceStream1, sourceStream1,
                                                                new SerializableLambdaKeySelector<TupleRowValues, TupleRowValues>() {

                                                                        // For now returning always the
                                                                        // same value, so basically no
                                                                        // parallelism.
                                                                        // Can fix later
                                                                        @Override
                                                                        public Integer apply(
                                                                                        T3<TupleRowValues, TupleRowValues> t) {
                                                                                return 0;
                                                                        }

                                                                },
                                                                new SerializableLambdaJoin<TupleRowValues, TupleRowValues, TupleRowValuesIDs>() {

                                                                        TupleMatcher tm = new TupleMatcher();

                                                                        @Override
                                                                        public TupleRowValuesIDs apply(
                                                                                        TupleRowValues first,
                                                                                        TupleRowValues second) {

                                                                                // System.out.println("Comparing
                                                                                // "+first+" -
                                                                                // "+second);
                                                                                QueryJoin.throughputStat
                                                                                                .increase(1);
                                                                                if (tm.match(first,
                                                                                                second,
                                                                                                ExperimentID.valueOf(
                                                                                                                parameter.get("experimentID")))) {
                                                                                        return new TupleRowValuesIDs(first,second);
                                                                                }
                                                                                return null;
                                                                        }

                                                                },
                                                                Time.milliseconds(JoinParameters
                                                                                .getWA(ExperimentID.valueOf(parameter
                                                                                                .get("experimentID")))),
                                                                Time.milliseconds(JoinParameters
                                                                                .getWS(ExperimentID.valueOf(parameter
                                                                                                .get("experimentID")))),
                                                                Time.milliseconds(
                                                                                parameter.getInt("allowedLateness",
                                                                                                Integer.MAX_VALUE)),
                                                                new TypeFactoryTupleRowValues2(),
                                                                new TypeFactoryTupleRowValuesIDs1());
                                break;
                        default:
                                break;
                }
               
                SinkLatencyRateLogger.createTimestampStringTupleSinkPerformance(joinStream, latencyOutputFile,
                                outputRateOutputFile, maxLatency, maxLatencyViolations);

                QueryJoin.throughputStat = new CountStat(throughputOutputFile1, true);

                env.execute();

        }
}
