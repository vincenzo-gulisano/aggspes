package usecase.wikipedia;

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
import util.CountStat;
import util.ImplementationType;
import util.Instrumenter;
import util.Instrumenter.SerializableLambdaJoin;
import util.Instrumenter.SerializableLambdaKeySelector;
import util.T3;

class TupleMatcher implements Serializable {

        public boolean match(TupleWikipediaUpdate t1, TupleWikipediaUpdate t2, ExperimentID expID) {
                String lowerCase1 = t1.f1.toLowerCase();
                String lowerCase2 = t2.f1.toLowerCase();
                if (!lowerCase1.equals(lowerCase2)) {
                        if (lowerCase1.length() > JoinParameters
                                        .getMinLength(expID)
                                        && Math.abs(lowerCase1.length()
                                                        - lowerCase2.length()) < 1) {
                                return true;
                        }
                }
                return false;
        }

}

public class QueryJoin {

        static CountStat throughputStat;

        public static void main(String[] args) throws Exception {

                // Managing parameters (here and only here)
                ParameterTool parameter = ParameterTool.fromArgs(args);
                String inputFile1 = parameter.get("inputFile");
                int allowedLateness = parameter.getInt("allowedLateness", Integer.MAX_VALUE);
                ExperimentID expID = ExperimentID.valueOf(parameter.get("experimentID"));
                ImplementationType implementationType = ImplementationType.valueOf(parameter.get("implementationType"));

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

                SingleOutputStreamOperator<TupleWikipediaUpdate> sourceStream1 = SourceWikipediaUpdate
                                .createTimestampStringTupleSourceForPerformanceStudy(env,
                                                inputFile1, injectionRateOutputFile1, allowedLateness, duration,
                                                batchSize, sleepTime)
                                .name("source");

                // Make sure a chain starts here
                sourceStream1.startNewChain();

                DataStream<TupleWikipediaUpdate> joinStream = null;
                switch (implementationType) {
                        case SINGLEOUT:
                                joinStream = inst.<TupleWikipediaUpdate, TupleWikipediaUpdate, TupleWikipediaUpdate>addAggBasedJoinSingleOut(
                                                sourceStream1, sourceStream1,
                                                new SerializableLambdaKeySelector<TupleWikipediaUpdate, TupleWikipediaUpdate>() {

                                                        @Override
                                                        public Integer apply(
                                                                        T3<TupleWikipediaUpdate, TupleWikipediaUpdate> t) {
                                                                return t.isFromA1() ? t.f0.get(0).f2
                                                                                .split(" ").length
                                                                                : t.f1.get(0).f2.split(
                                                                                                " ").length;
                                                        }

                                                },
                                                new SerializableLambdaJoin<TupleWikipediaUpdate, TupleWikipediaUpdate, TupleWikipediaUpdate>() {

                                                        TupleMatcher tm = new TupleMatcher();

                                                        @Override
                                                        public TupleWikipediaUpdate apply(
                                                                        TupleWikipediaUpdate first,
                                                                        TupleWikipediaUpdate second) {

                                                                QueryJoin.throughputStat.increase(1);
                                                                if (tm.match(first, second, expID)) {
                                                                        return new TupleWikipediaUpdate(
                                                                                        Math.max(first.f0,
                                                                                                        second.f0),
                                                                                        first.f2,
                                                                                        second.f2,
                                                                                        "",
                                                                                        Math.max(first.f4,
                                                                                                        second.f4));

                                                                }
                                                                return null;
                                                        }

                                                },
                                                Time.milliseconds(JoinParameters.getWA(expID)),
                                                Time.milliseconds(JoinParameters.getWS(expID)),
                                                Time.milliseconds(allowedLateness),
                                                new TypeFactoryTupleWikipediaUpdate2(),
                                                new TypeFactoryTupleWikipediaUpdate1());
                                break;
                        case MULTIOUT:
                                joinStream = inst.<TupleWikipediaUpdate, TupleWikipediaUpdate, TupleWikipediaUpdate>addAggBasedJoinMultiOut(
                                                sourceStream1, sourceStream1,
                                                new SerializableLambdaKeySelector<TupleWikipediaUpdate, TupleWikipediaUpdate>() {

                                                        @Override
                                                        public Integer apply(
                                                                        T3<TupleWikipediaUpdate, TupleWikipediaUpdate> t) {
                                                                return t.isFromA1() ? t.f0.get(0).f2
                                                                                .split(" ").length
                                                                                : t.f1.get(0).f2.split(
                                                                                                " ").length;
                                                        }

                                                },
                                                new SerializableLambdaJoin<TupleWikipediaUpdate, TupleWikipediaUpdate, TupleWikipediaUpdate>() {

                                                        TupleMatcher tm = new TupleMatcher();

                                                        @Override
                                                        public TupleWikipediaUpdate apply(
                                                                        TupleWikipediaUpdate first,
                                                                        TupleWikipediaUpdate second) {

                                                                QueryJoin.throughputStat.increase(1);
                                                                if (tm.match(first, second, expID)) {
                                                                        return new TupleWikipediaUpdate(
                                                                                        Math.max(first.f0,
                                                                                                        second.f0),
                                                                                        first.f2,
                                                                                        second.f2,
                                                                                        "",
                                                                                        Math.max(first.f4,
                                                                                                        second.f4));

                                                                }
                                                                return null;
                                                        }

                                                },
                                                Time.milliseconds(JoinParameters.getWA(expID)),
                                                Time.milliseconds(JoinParameters.getWS(expID)),
                                                Time.milliseconds(allowedLateness),
                                                new TypeFactoryTupleWikipediaUpdate2(),
                                                new TypeFactoryTupleWikipediaUpdate1());
                                break;

                        case NATIVE:
                                joinStream = sourceStream1.join(sourceStream1).where(t -> t.f2.split(" ").length)
                                                .equalTo(t -> t.f2.split(" ").length)
                                                .window(SlidingEventTimeWindows.of(
                                                                Time.milliseconds(JoinParameters.getWS(expID)),
                                                                Time.milliseconds(JoinParameters.getWA(expID))))
                                                .apply(new FlatJoinFunction<TupleWikipediaUpdate, TupleWikipediaUpdate, TupleWikipediaUpdate>() {

                                                        TupleMatcher tm = new TupleMatcher();

                                                        @Override
                                                        public void join(TupleWikipediaUpdate first,
                                                                        TupleWikipediaUpdate second,
                                                                        Collector<TupleWikipediaUpdate> out)
                                                                        throws Exception {

                                                                QueryJoin.throughputStat.increase(1);
                                                                if (tm.match(first, second, expID)) {

                                                                        out.collect(new TupleWikipediaUpdate(
                                                                                        Math.max(first.f0,
                                                                                                        second.f0),
                                                                                        first.f2,
                                                                                        second.f2,
                                                                                        "",
                                                                                        Math.max(first.f4,
                                                                                                        second.f4)));

                                                                }
                                                        }

                                                });
                                break;
                        default:
                                                throw new RuntimeException("Implementation type not recognized!");
                }

                SinkLatencyRateLogger.createTimestampStringTupleSinkPerformance(joinStream, latencyOutputFile,
                                outputRateOutputFile, maxLatency, maxLatencyViolations);

                QueryJoin.throughputStat = new CountStat(throughputOutputFile1, true);

                env.execute();

        }
}
