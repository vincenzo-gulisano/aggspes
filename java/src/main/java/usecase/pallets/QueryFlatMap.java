package usecase.pallets;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;

import usecase.ExperimentID;
import usecase.wikipedia.SinkLatencyRateLogger;
import util.ImplementationType;
import util.Instrumenter;

public class QueryFlatMap {
        public static void main(String[] args) throws Exception {

                // Managing parameters (here and only here)
                ParameterTool parameter = ParameterTool.fromArgs(args);
                String inputDir = parameter.get("inputFile");
                int allowedLateness = parameter.getInt("allowedLateness", Integer.MAX_VALUE);
                ExperimentID expID = ExperimentID.valueOf(parameter.get("experimentID"));
                ImplementationType implementationType = ImplementationType.valueOf(parameter.get("implementationType"));

                int maxLatency = parameter.getInt("maxLatency", 20000);
                int maxLatencyViolations = parameter.getInt("maxLatencyViolations", 10);

                // The following pararmeters are option and used for performance experiments
                String injectionRateOutputFile = parameter.get("injectionRateOutputFile");
                String throughputOutputFile = parameter.get("throughputOutputFile");
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

                SingleOutputStreamOperator<TupleRowValues> sourceStream = SourceRowValues
                                .createTimestampStringTupleSourceForPerformanceStudy(env,
                                                inputDir, injectionRateOutputFile, duration,
                                                batchSize, sleepTime);

                sourceStream = inst.addThroughputLogger(sourceStream, throughputOutputFile);

                // Make sure a chain starts here
                sourceStream.startNewChain();

                // Instantiate the FlatMap
                SingleOutputStreamOperator<TupleRotationData> flatMapStream = null;

                switch (implementationType) {
                        case SINGLEOUT:
                                flatMapStream = inst.addAggBasedFlatMapSingleOut(
                                                sourceStream,
                                                FlatMapFunctions.instantiateFlatMapFunctionAggBasedSingleOut(expID),
                                                Time.milliseconds(allowedLateness),
                                                new TypeFactoryTupleRotationData1());
                                break;
                        case MULTIOUT:
                                flatMapStream = inst.addAggBasedFlatMapMultiOut(sourceStream,
                                                FlatMapFunctions.instantiateFlatMapFunctionAggBasedMultiOut(
                                                                expID),
                                                Time.milliseconds(allowedLateness),
                                                new TypeFactoryTupleRotationData1());
                                break;
                        case NATIVE:
                                flatMapStream = sourceStream
                                                .flatMap(FlatMapFunctions.instantiateFlatMapFunctionNative(expID))
                                                .name("flatmapperformance")
                                                .startNewChain();
                                break;
                        default:
                                throw new RuntimeException("Unknown type");
                }

                // Make sure a chain starts here
                flatMapStream.startNewChain();

                // Instantiate the sink (the same for native and aggregate-based)
                SinkLatencyRateLogger.createTimestampStringTupleSinkPerformance(flatMapStream, latencyOutputFile,
                                outputRateOutputFile, maxLatency, maxLatencyViolations);

                env.execute();

        }
}
