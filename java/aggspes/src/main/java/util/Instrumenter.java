package util;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.flink.api.common.functions.RichFilterFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

public class Instrumenter implements Serializable {

    public interface SerializableLambda<IN, OUT> extends Function<IN, OUT>, Serializable {
    }

    public interface SerializableProcessFunction<IN, OUT> extends Serializable {
        public void process(IN t, Collector<OUT> out);
    }

    public interface SerializableLambdaFlatMap<IN, OUT> extends Function<IN, List<OUT>>, Serializable {
    }

    public interface SerializableLambdaJoin<IN1, IN2, OUT> extends BiFunction<IN1, IN2, OUT>, Serializable {
    }

    public interface SerializableLambdaKeySelector<IN1, IN2> extends Function<T3<IN1, IN2>, Integer>, Serializable {
    }

    public interface SerializableLambdaINInteger<IN> extends Function<IN, Integer>, Serializable {
    }

    public interface SerializableLambdaINBoolean<IN> extends Function<IN, Boolean>, Serializable {
    }

    class HashKeySelector<IN> implements KeySelector<IN, Integer> {

        @Override
        public Integer getKey(IN t) throws Exception {
            return t.hashCode();
        }

    }

    private <OUT> SingleOutputStreamOperator<OUT> addDuplicator(SingleOutputStreamOperator<T5<OUT>> s,
            Time allowedLateness, TypeFactory1<OUT> factory) {

        // Add UpstreamWatermarkManager
        SingleOutputStreamOperator<T5<OUT>> s1IterStart = s
                .transform(UpstreamWatermarkManager.class.getSimpleName(),
                        s.getType(),
                        new UpstreamWatermarkManager<>(s.getName(),
                                allowedLateness.toMilliseconds()))
                .startNewChain();

        SingleOutputStreamOperator<T5<OUT>> sL = s1IterStart.keyBy(new HashKeySelector<>())
                .window(TumblingEventTimeWindows.of(Time.milliseconds(1))).allowedLateness(allowedLateness)
                .process(new ProcessWindowFunction<T5<OUT>, T5<OUT>, Integer, TimeWindow>() {

                    @Override
                    public void process(Integer key,
                            ProcessWindowFunction<T5<OUT>, T5<OUT>, Integer, TimeWindow>.Context context,
                            Iterable<T5<OUT>> elements, Collector<T5<OUT>> out) throws Exception {

                        Iterator<T5<OUT>> it = elements.iterator();
                        T5<OUT> t = it.next();
                        List<OUT> outT = t.getOutputTuples();

                        while (it.hasNext()) {
                            t = it.next();
                            outT.addAll(t.getOutputTuples());
                        }

                        if (t.getOutputTupleIndex() == 0) {
                            out.collect(new T5<>(outT, 1));
                        } else if (t.getOutputTupleIndex() < t.getOutSize()) {
                            out.collect(new T5<>(t.getOutputTuples(), t.getOutputTupleIndex() + 1));
                        }

                    }
                });

        // Add Downstream Watermark Manager
        SingleOutputStreamOperator<T5<OUT>> sLStar = sL.transform(DownstreamWatermarkManager.class.getSimpleName(),
                sL.getType(),
                new DownstreamWatermarkManager<>(s.getName()));

        // Add Aggregate A2, depending on the type
        SingleOutputStreamOperator<OUT> sO = sLStar.keyBy(new HashKeySelector<>())
                .window(TumblingEventTimeWindows.of(Time.milliseconds(1)))
                .process(new ProcessWindowFunction<T5<OUT>, OUT, Integer, TimeWindow>() {

                    @Override
                    public void process(Integer key,
                            ProcessWindowFunction<T5<OUT>, OUT, Integer, TimeWindow>.Context context,
                            Iterable<T5<OUT>> elements, Collector<OUT> out) throws Exception {
                        T5<OUT> t = elements.iterator().next();
                        out.collect(t.getOutputTuples().get(t.getOutputTupleIndex() - 1));
                    }

                }).returns(factory.getTTypeInformation());

        return sO;

    }

    public <IN> SingleOutputStreamOperator<IN> addAggBasedFilterMultiOut(SingleOutputStreamOperator<IN> s,
            SerializableProcessFunction<IN, IN> filterFunction, Time allowedLateness, TypeFactory1<IN> factory) {

        return s.keyBy(new HashKeySelector<>())
                .window(TumblingEventTimeWindows.of(Time.milliseconds(1)))
                .process(new ProcessWindowFunction<IN, IN, Integer, TimeWindow>() {

                    @Override
                    public void process(Integer key,
                            ProcessWindowFunction<IN, IN, Integer, TimeWindow>.Context context,
                            Iterable<IN> elements, Collector<IN> out)
                            throws Exception {

                        Iterator<IN> it = elements.iterator();
                        while (it.hasNext()) {
                            filterFunction.process(it.next(), out);
                        }
                    }

                }).returns(factory.getTTypeInformation());

    }

    public <IN> SingleOutputStreamOperator<IN> addAggBasedFilterSingleOut(SingleOutputStreamOperator<IN> s,
            SerializableLambdaINBoolean<IN> filterFunction, Time allowedLateness, TypeFactory1<IN> factory) {

        return this.<IN>addDuplicator(s.keyBy(new HashKeySelector<>())
                .window(TumblingEventTimeWindows.of(Time.milliseconds(1)))
                .process(new ProcessWindowFunction<IN, T5<IN>, Integer, TimeWindow>() {

                    @Override
                    public void process(Integer key,
                            ProcessWindowFunction<IN, T5<IN>, Integer, TimeWindow>.Context context,
                            Iterable<IN> elements, Collector<T5<IN>> out)
                            throws Exception {

                        Iterator<IN> it = elements.iterator();
                        IN t = it.next();

                        if (filterFunction.apply(t)) {

                            // Compute the number of copies, but only if the tuple is not discarded
                            LinkedList<IN> outputTuples = new LinkedList<>();
                            outputTuples.add(t);
                            while (it.hasNext()) {
                                it.next();
                                outputTuples.add(t);
                            }

                            out.collect(new T5<>(outputTuples, 0));
                        }
                    }

                }), allowedLateness, factory);

    }

    public <IN, OUT> SingleOutputStreamOperator<OUT> addAggBasedMapMultiOut(SingleOutputStreamOperator<IN> s,
            SerializableProcessFunction<IN, OUT> mapFunction, Time allowedLateness, TypeFactory1<OUT> factory) {

        return s.keyBy(new HashKeySelector<>())
                .window(TumblingEventTimeWindows.of(Time.milliseconds(1)))
                .process(new ProcessWindowFunction<IN, OUT, Integer, TimeWindow>() {

                    @Override
                    public void process(Integer key,
                            ProcessWindowFunction<IN, OUT, Integer, TimeWindow>.Context context,
                            Iterable<IN> elements, Collector<OUT> out)
                            throws Exception {

                        Iterator<IN> it = elements.iterator();
                        while (it.hasNext()) {
                            mapFunction.process(it.next(), out);
                        }
                    }

                }).returns(factory.getTTypeInformation());

    }

    public <IN, OUT> SingleOutputStreamOperator<OUT> addAggBasedMapSingleOut(SingleOutputStreamOperator<IN> s,
            SerializableLambda<IN, OUT> mapFunction, Time allowedLateness, TypeFactory1<OUT> factory) {

        return this.<OUT>addDuplicator(s.keyBy(new HashKeySelector<>())
                .window(TumblingEventTimeWindows.of(Time.milliseconds(1)))
                .process(new ProcessWindowFunction<IN, T5<OUT>, Integer, TimeWindow>() {

                    @Override
                    public void process(Integer key,
                            ProcessWindowFunction<IN, T5<OUT>, Integer, TimeWindow>.Context context,
                            Iterable<IN> elements, Collector<T5<OUT>> out)
                            throws Exception {

                        Iterator<IN> it = elements.iterator();
                        IN t = it.next();
                        LinkedList<OUT> outputTuples = new LinkedList<>();
                        outputTuples.add(mapFunction.apply(t));
                        while (it.hasNext()) {
                            it.next();
                            outputTuples.add(mapFunction.apply(t));
                        }
                        out.collect(new T5<>(outputTuples, 0));
                    }

                }), allowedLateness, factory);

    }

    public <IN, OUT> SingleOutputStreamOperator<OUT> addAggBasedFlatMapMultiOut(SingleOutputStreamOperator<IN> s,
            SerializableProcessFunction<IN, OUT> flatMapFunction, Time allowedLateness, TypeFactory1<OUT> factory) {

        return s.keyBy(new HashKeySelector<>())
                .window(TumblingEventTimeWindows.of(Time.milliseconds(1)))
                .process(new ProcessWindowFunction<IN, OUT, Integer, TimeWindow>() {

                    @Override
                    public void process(Integer key,
                            ProcessWindowFunction<IN, OUT, Integer, TimeWindow>.Context context,
                            Iterable<IN> elements, Collector<OUT> out)
                            throws Exception {

                        Iterator<IN> it = elements.iterator();
                        while (it.hasNext()) {
                            flatMapFunction.process(it.next(), out);
                        }
                    }

                }).returns(factory.getTTypeInformation());

    }


    public <IN, OUT> SingleOutputStreamOperator<OUT> addAggBasedFlatMapSingleOut(SingleOutputStreamOperator<IN> s,
            SerializableLambdaFlatMap<IN, OUT> flatMapFunction, Time allowedLateness, TypeFactory1<OUT> factory) {

        return this.<OUT>addDuplicator(s.keyBy(new HashKeySelector<>())
                .window(TumblingEventTimeWindows.of(Time.milliseconds(1)))
                .process(new ProcessWindowFunction<IN, T5<OUT>, Integer, TimeWindow>() {

                    @Override
                    public void process(Integer key,
                            ProcessWindowFunction<IN, T5<OUT>, Integer, TimeWindow>.Context context,
                            Iterable<IN> elements, Collector<T5<OUT>> out)
                            throws Exception {

                        Iterator<IN> it = elements.iterator();
                        IN t = it.next();
                        List<OUT> outputTuples = flatMapFunction.apply(t);
                        if (outputTuples != null && outputTuples.size() > 0) {
                            while (it.hasNext()) {
                                it.next();
                                outputTuples.addAll(flatMapFunction.apply(t));
                            }
                            out.collect(new T5<>(outputTuples, 0));
                        }
                    }

                }), allowedLateness, factory);

    }


    public <TIN1, TIN2, TOUT> SingleOutputStreamOperator<TOUT> addAggBasedJoinMultiOut(SingleOutputStreamOperator<TIN1> s1,
            SingleOutputStreamOperator<TIN2> s2, SerializableLambdaKeySelector<TIN1, TIN2> keySelector,
            SerializableLambdaJoin<TIN1, TIN2, TOUT> fJoin, Time WA, Time WS, Time allowedLateness,
            TypeFactory2<TIN1, TIN2> factoryIn, TypeFactory1<TOUT> factoryOut) {

        // Add first aggregate encapsulator
        SingleOutputStreamOperator<T3<TIN1, TIN2>> eA1 = s1.keyBy(new HashKeySelector<>())
                .window(TumblingEventTimeWindows.of(Time.milliseconds(1)))
                .process(new ProcessWindowFunction<TIN1, T3<TIN1, TIN2>, Integer, TimeWindow>() {

                    @Override
                    public void process(Integer key,
                            ProcessWindowFunction<TIN1, T3<TIN1, TIN2>, Integer, TimeWindow>.Context context,
                            Iterable<TIN1> elements, Collector<T3<TIN1, TIN2>> out) throws Exception {
                        Iterator<TIN1> it = elements.iterator();
                        List<TIN1> t1Tuples = new LinkedList<>();
                        t1Tuples.add(it.next());
                        while (it.hasNext()) {
                            t1Tuples.add(it.next());
                        }
                        out.collect(new T3<>(t1Tuples, new LinkedList<TIN2>()));
                    }

                }).returns(factoryIn.getT3TypeInformation());

        // Add second aggregate encapsulator
        SingleOutputStreamOperator<T3<TIN1, TIN2>> eA2 = s2.keyBy(new HashKeySelector<>())
                .window(TumblingEventTimeWindows.of(Time.milliseconds(1)))
                .process(new ProcessWindowFunction<TIN2, T3<TIN1, TIN2>, Integer, TimeWindow>() {

                    @Override
                    public void process(Integer key,
                            ProcessWindowFunction<TIN2, T3<TIN1, TIN2>, Integer, TimeWindow>.Context context,
                            Iterable<TIN2> elements, Collector<T3<TIN1, TIN2>> out) throws Exception {
                        Iterator<TIN2> it = elements.iterator();
                        List<TIN2> t2Tuples = new LinkedList<>();
                        t2Tuples.add(it.next());
                        while (it.hasNext()) {
                            t2Tuples.add(it.next());
                        }
                        out.collect(new T3<>(new LinkedList<TIN1>(), t2Tuples));
                    }

                }).returns(factoryIn.getT3TypeInformation());

        // Union of streams
        DataStream<T3<TIN1, TIN2>> unionEA = eA1.union(eA2.returns(factoryIn.getT3TypeInformation()));

        return unionEA.keyBy(new KeySelector<T3<TIN1, TIN2>, Integer>() {
            @Override
            public Integer getKey(T3<TIN1, TIN2> t) throws Exception {
                return keySelector.apply(t);
            }
        }).window(SlidingEventTimeWindows.of(WS, WA))
                .process(new ProcessWindowFunction<T3<TIN1, TIN2>, TOUT, Integer, TimeWindow>() {

                    @Override
                    public void process(Integer key,
                            ProcessWindowFunction<T3<TIN1, TIN2>, TOUT, Integer, TimeWindow>.Context context,
                            Iterable<T3<TIN1, TIN2>> elements, Collector<TOUT> out) throws Exception {
                        List<TIN1> winS1 = new LinkedList<>();
                        List<TIN2> winS2 = new LinkedList<>();

                        Iterator<T3<TIN1, TIN2>> it = elements.iterator();

                        while (it.hasNext()) {
                            T3<TIN1, TIN2> t = it.next();

                            if (t.isFromA1()) {

                                for (TIN1 t1 : t.f0) {
                                    for (TIN2 otherT : winS2) {
                                        TOUT tOut = fJoin.apply(t1, otherT);
                                        if (tOut != null) {
                                            out.collect(tOut);
                                        }
                                    }
                                    winS1.add(t1);
                                }
                            } else {

                                for (TIN2 t2 : t.f1) {
                                    for (TIN1 otherT : winS1) {

                                        TOUT tOut = fJoin.apply(otherT, t2);
                                        if (tOut != null) {
                                            out.collect(tOut);
                                        }
                                    }
                                    winS2.add(t2);
                                }
                            }

                        }

                    }
                }).returns(factoryOut.getTTypeInformation());

    }

    public <TIN1, TIN2, TOUT> SingleOutputStreamOperator<TOUT> addAggBasedJoinSingleOut(SingleOutputStreamOperator<TIN1> s1,
            SingleOutputStreamOperator<TIN2> s2, SerializableLambdaKeySelector<TIN1, TIN2> keySelector,
            SerializableLambdaJoin<TIN1, TIN2, TOUT> fJoin, Time WA, Time WS, Time allowedLateness,
            TypeFactory2<TIN1, TIN2> factoryIn, TypeFactory1<TOUT> factoryOut) {

        // Add first aggregate encapsulator
        SingleOutputStreamOperator<T3<TIN1, TIN2>> eA1 = s1.keyBy(new HashKeySelector<>())
                .window(TumblingEventTimeWindows.of(Time.milliseconds(1)))
                .process(new ProcessWindowFunction<TIN1, T3<TIN1, TIN2>, Integer, TimeWindow>() {

                    @Override
                    public void process(Integer key,
                            ProcessWindowFunction<TIN1, T3<TIN1, TIN2>, Integer, TimeWindow>.Context context,
                            Iterable<TIN1> elements, Collector<T3<TIN1, TIN2>> out) throws Exception {
                        Iterator<TIN1> it = elements.iterator();
                        List<TIN1> t1Tuples = new LinkedList<>();
                        t1Tuples.add(it.next());
                        while (it.hasNext()) {
                            t1Tuples.add(it.next());
                        }
                        out.collect(new T3<>(t1Tuples, new LinkedList<TIN2>()));
                    }

                }).returns(factoryIn.getT3TypeInformation());

        // Add second aggregate encapsulator
        SingleOutputStreamOperator<T3<TIN1, TIN2>> eA2 = s2.keyBy(new HashKeySelector<>())
                .window(TumblingEventTimeWindows.of(Time.milliseconds(1)))
                .process(new ProcessWindowFunction<TIN2, T3<TIN1, TIN2>, Integer, TimeWindow>() {

                    @Override
                    public void process(Integer key,
                            ProcessWindowFunction<TIN2, T3<TIN1, TIN2>, Integer, TimeWindow>.Context context,
                            Iterable<TIN2> elements, Collector<T3<TIN1, TIN2>> out) throws Exception {
                        Iterator<TIN2> it = elements.iterator();
                        List<TIN2> t2Tuples = new LinkedList<>();
                        t2Tuples.add(it.next());
                        while (it.hasNext()) {
                            t2Tuples.add(it.next());
                        }
                        out.collect(new T3<>(new LinkedList<TIN1>(), t2Tuples));
                    }

                }).returns(factoryIn.getT3TypeInformation());

        // Union of streams
        DataStream<T3<TIN1, TIN2>> unionEA = eA1.union(eA2.returns(factoryIn.getT3TypeInformation()));

        return this.<TOUT>addDuplicator(unionEA.keyBy(new KeySelector<T3<TIN1, TIN2>, Integer>() {
            @Override
            public Integer getKey(T3<TIN1, TIN2> t) throws Exception {
                return keySelector.apply(t);
            }
        }).window(SlidingEventTimeWindows.of(WS, WA))
                .process(new ProcessWindowFunction<T3<TIN1, TIN2>, T5<TOUT>, Integer, TimeWindow>() {

                    @Override
                    public void process(Integer key,
                            ProcessWindowFunction<T3<TIN1, TIN2>, T5<TOUT>, Integer, TimeWindow>.Context context,
                            Iterable<T3<TIN1, TIN2>> elements, Collector<T5<TOUT>> out) throws Exception {
                        List<TIN1> winS1 = new LinkedList<>();
                        List<TIN2> winS2 = new LinkedList<>();
                        List<TOUT> outputs = new LinkedList<>();

                        Iterator<T3<TIN1, TIN2>> it = elements.iterator();

                        while (it.hasNext()) {
                            T3<TIN1, TIN2> t = it.next();

                            if (t.isFromA1()) {

                                for (TIN1 t1 : t.f0) {
                                    for (TIN2 otherT : winS2) {
                                        TOUT tOut = fJoin.apply(t1, otherT);
                                        if (tOut != null) {
                                            outputs.add(tOut);
                                        }
                                    }
                                    winS1.add(t1);
                                }
                            } else {

                                for (TIN2 t2 : t.f1) {
                                    for (TIN1 otherT : winS1) {
                                        TOUT tOut = fJoin.apply(otherT, t2);
                                        if (tOut != null) {
                                            outputs.add(tOut);
                                        }
                                    }
                                    winS2.add(t2);
                                }
                            }

                        }

                        if (!outputs.isEmpty()) {
                            out.collect(new T5<TOUT>(outputs, 0));
                        }
                    }
                }), allowedLateness, factoryOut);

    }

    public <T extends TimestampedTuple> SingleOutputStreamOperator<T> addThroughputLogger(
            DataStream<T> s, String outputFile) {

        return s.filter(new RichFilterFunction<T>() {

            private CountStat log;

            @Override
            public void open(Configuration parameters) throws Exception {
                log = new CountStat(outputFile, true);
            }

            @Override
            public void close() throws Exception {
                log.close();
            }

            @Override
            public boolean filter(T value) throws Exception {
                log.increase(1);
                return true;
            }

        });

    }

    public <T extends TimestampedTuple> SingleOutputStreamOperator<T> addLatencyLogger(
            DataStream<T> s, String outputFile) {

        return s.filter(new RichFilterFunction<T>() {

            private AvgStat log;

            @Override
            public void open(Configuration parameters) throws Exception {
                log = new AvgStat(outputFile, true);
            }

            @Override
            public void close() throws Exception {
                log.close();
            }

            @Override
            public boolean filter(T value) throws Exception {
                log.add(System.currentTimeMillis() - value.getStimulus());
                return true;
            }

        });

    }

}
