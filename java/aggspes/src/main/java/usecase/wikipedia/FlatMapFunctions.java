package usecase.wikipedia;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;

import usecase.ExperimentID;
import util.Instrumenter.SerializableProcessFunction;
import util.Instrumenter.SerializableLambdaFlatMap;

public class FlatMapFunctions {

    private static SerializableProcessFunction<TupleWikipediaUpdate, TupleWikipediaUpdate> topKOriginalEntryBasedOnLengthAggBasedMultiOut(
            int k, int minLength) {
        return new SerializableProcessFunction<TupleWikipediaUpdate, TupleWikipediaUpdate>() {
            @Override
            public void process(TupleWikipediaUpdate t, Collector<TupleWikipediaUpdate> out) {
                List<String> words = findMostFrequentWordsAndReturnBasedOnLength(t.f1, k, minLength);
                for (String w : words) {
                    out.collect(new TupleWikipediaUpdate(t.f0, w, "", "", t.f4));
                }
            }

        };
    }

    private static SerializableLambdaFlatMap<TupleWikipediaUpdate, TupleWikipediaUpdate> topKOriginalEntryBasedOnLengthAggBasedSingleOut(
            int k, int minLength) {
        return new SerializableLambdaFlatMap<TupleWikipediaUpdate, TupleWikipediaUpdate>() {

            @Override
            public List<TupleWikipediaUpdate> apply(TupleWikipediaUpdate t) {
                List<String> words = findMostFrequentWordsAndReturnBasedOnLength(t.f1, k, minLength);
                List<TupleWikipediaUpdate> output = new LinkedList<>();
                for (String w : words) {
                    output.add(new TupleWikipediaUpdate(t.f0, w, "", "", t.f4));
                }
                return output;
            }

        };
    }

    private static FlatMapFunction<TupleWikipediaUpdate, TupleWikipediaUpdate> topKOriginalEntryBasedOnLengthNative(
            int k, int minLength) {
        return new FlatMapFunction<TupleWikipediaUpdate, TupleWikipediaUpdate>() {

            @Override
            public void flatMap(TupleWikipediaUpdate t, Collector<TupleWikipediaUpdate> out) throws Exception {
                List<String> words = findMostFrequentWordsAndReturnBasedOnLength(t.f1, k, minLength);
                for (String w : words) {
                    out.collect(new TupleWikipediaUpdate(t.f0, w, "", "", t.f4));
                }
            }

        };
    }

    private static SerializableProcessFunction<TupleWikipediaUpdate, TupleWikipediaUpdate> topKAllEntriesBasedOnLengthAggBasedMultiOut(
            int k, int minLength) {
        return new SerializableProcessFunction<TupleWikipediaUpdate, TupleWikipediaUpdate>() {

            @Override
            public void process(TupleWikipediaUpdate t, Collector<TupleWikipediaUpdate> out) {
                List<String> f1Words = findMostFrequentWordsAndReturnBasedOnLength(t.f1, k, minLength);
                List<String> f2Words = findMostFrequentWordsAndReturnBasedOnLength(t.f2, k, minLength);
                List<String> f3Words = findMostFrequentWordsAndReturnBasedOnLength(t.f3, k, minLength);
                // Notice I take the minimum of the three because there are no guarantees all
                // will have k...
                for (int i = 0; i < Math.min(f1Words.size(), Math.min(f2Words.size(), f3Words.size())); i++) {
                    out.collect(new TupleWikipediaUpdate(t.f0, f1Words.get(i), f2Words.get(i), f3Words.get(i), t.f4));
                }

            }

        };
    }

    private static SerializableLambdaFlatMap<TupleWikipediaUpdate, TupleWikipediaUpdate> topKAllEntriesBasedOnLengthAggBasedSingleOut(
            int k, int minLength) {
        return new SerializableLambdaFlatMap<TupleWikipediaUpdate, TupleWikipediaUpdate>() {

            @Override
            public List<TupleWikipediaUpdate> apply(TupleWikipediaUpdate t) {
                List<String> f1Words = findMostFrequentWordsAndReturnBasedOnLength(t.f1, k, minLength);
                List<String> f2Words = findMostFrequentWordsAndReturnBasedOnLength(t.f2, k, minLength);
                List<String> f3Words = findMostFrequentWordsAndReturnBasedOnLength(t.f3, k, minLength);
                List<TupleWikipediaUpdate> output = new LinkedList<>();
                // Notice I take the minimum of the three because there are no guarantees all
                // will have k...
                for (int i = 0; i < Math.min(f1Words.size(), Math.min(f2Words.size(), f3Words.size())); i++) {
                    output.add(new TupleWikipediaUpdate(t.f0, f1Words.get(i), f2Words.get(i), f3Words.get(i), t.f4));
                }
                return output;
            }

        };
    }

    private static FlatMapFunction<TupleWikipediaUpdate, TupleWikipediaUpdate> topKAllEntriesBasedOnLengthNative(int k,
            int minLength) {
        return new FlatMapFunction<TupleWikipediaUpdate, TupleWikipediaUpdate>() {

            @Override
            public void flatMap(TupleWikipediaUpdate t, Collector<TupleWikipediaUpdate> out) throws Exception {
                List<String> f1Words = findMostFrequentWordsAndReturnBasedOnLength(t.f1, k, minLength);
                List<String> f2Words = findMostFrequentWordsAndReturnBasedOnLength(t.f2, k, minLength);
                List<String> f3Words = findMostFrequentWordsAndReturnBasedOnLength(t.f3, k, minLength);
                // Notice I take the minimum of the three because there are no guarantees all
                // will have k...
                for (int i = 0; i < Math.min(f1Words.size(), Math.min(f2Words.size(), f3Words.size())); i++) {
                    out.collect(new TupleWikipediaUpdate(t.f0, f1Words.get(i), f2Words.get(i), f3Words.get(i), t.f4));
                }
            }

        };
    }

    private static List<String> findMostFrequentWordsAndReturnBasedOnLength(String s, int k, int length) {
        List<String> result = new LinkedList<>();
        Map<String, Integer> topk = findFrequentWords(s, k);
        for (String w : topk.keySet()) {
            if (w.length() >= length) {
                result.add(w);
            }
        }
        return result;
    }

    private static Map<String, Integer> findFrequentWords(String s, int numberOfWords) {
        Map<String, Integer> mapOfFrequentWords = new TreeMap<>();

        String[] words = s.split("\\s+");

        for (String word : words) {
            if (!mapOfFrequentWords.containsKey(word)) {
                mapOfFrequentWords.put(word, 1);
            } else {
                mapOfFrequentWords.put(word, mapOfFrequentWords.get(word) + 1);
            }
        }

        Map<String, Integer> sorted = mapOfFrequentWords
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(numberOfWords)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                        LinkedHashMap::new));
        return sorted;
    }

    public static SerializableProcessFunction<TupleWikipediaUpdate, TupleWikipediaUpdate> instantiateFlatMapFunctionAggBasedMultiOut(
            ExperimentID id) {
        switch (id) {
            case LLF:
                return topKOriginalEntryBasedOnLengthAggBasedMultiOut(1, 10);
            case ALF:
                return topKOriginalEntryBasedOnLengthAggBasedMultiOut(1, 0);
            case HLF:
                return topKOriginalEntryBasedOnLengthAggBasedMultiOut(3, 0);
            case LHF:
                return topKAllEntriesBasedOnLengthAggBasedMultiOut(1, 10);
            case AHF:
                return topKAllEntriesBasedOnLengthAggBasedMultiOut(1, 0);
            case HHF:
                return topKAllEntriesBasedOnLengthAggBasedMultiOut(3, 0);
            default:
                throw new RuntimeException("Unrecognized experiment id");
        }
    }

    public static SerializableLambdaFlatMap<TupleWikipediaUpdate, TupleWikipediaUpdate> instantiateFlatMapFunctionAggBasedSingleOut(
            ExperimentID id) {
        switch (id) {
            case LLF:
                return topKOriginalEntryBasedOnLengthAggBasedSingleOut(1, 10);
            case ALF:
                return topKOriginalEntryBasedOnLengthAggBasedSingleOut(1, 0);
            case HLF:
                return topKOriginalEntryBasedOnLengthAggBasedSingleOut(3, 0);
            case LHF:
                return topKAllEntriesBasedOnLengthAggBasedSingleOut(1, 10);
            case AHF:
                return topKAllEntriesBasedOnLengthAggBasedSingleOut(1, 0);
            case HHF:
                return topKAllEntriesBasedOnLengthAggBasedSingleOut(3, 0);
            default:
                throw new RuntimeException("Unrecognized experiment id");
        }
    }

    public static FlatMapFunction<TupleWikipediaUpdate, TupleWikipediaUpdate> instantiateFlatMapFunctionNative(
            ExperimentID id) {
        switch (id) {
            case LLF:
                return topKOriginalEntryBasedOnLengthNative(1, 10);
            case ALF:
                return topKOriginalEntryBasedOnLengthNative(1, 0);
            case HLF:
                return topKOriginalEntryBasedOnLengthNative(3, 0);
            case LHF:
                return topKAllEntriesBasedOnLengthNative(1, 10);
            case AHF:
                return topKAllEntriesBasedOnLengthNative(1, 0);
            case HHF:
                return topKAllEntriesBasedOnLengthNative(3, 0);
            default:
                throw new RuntimeException("Unrecognized experiment id");
        }
    }
}
