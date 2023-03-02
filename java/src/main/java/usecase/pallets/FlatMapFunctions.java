package usecase.pallets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;

import usecase.ExperimentID;
import util.Instrumenter.SerializableProcessFunction;
import util.Instrumenter.SerializableLambdaFlatMap;

public class FlatMapFunctions {

    private static SerializableProcessFunction<TupleRowValues, TupleRotationData> convertPolarAggBasedMultiOut(double minAvgDist,
            int maxPointsInRotation, boolean distFromRefPoint) {
        return new SerializableProcessFunction<TupleRowValues, TupleRotationData>() {

            @Override
            public void process(TupleRowValues t, Collector<TupleRotationData> out) {
                transformMultiOut(t, minAvgDist, maxPointsInRotation, distFromRefPoint, out);
            }
        };
    }

    
    private static SerializableLambdaFlatMap<TupleRowValues, TupleRotationData> convertPolarAggBasedSingleOut(double minAvgDist,
            int maxPointsInRotation, boolean distFromRefPoint) {
        return new SerializableLambdaFlatMap<TupleRowValues, TupleRotationData>() {
            @Override
            public List<TupleRotationData> apply(TupleRowValues t) {
                return transformSingleOut(t, minAvgDist, maxPointsInRotation, distFromRefPoint);
            }
        };
    }

    private static FlatMapFunction<TupleRowValues, TupleRotationData> convertPolarNative(double minAvgDist,
            int maxPointsInRotation, boolean distFromRefPoint) {
        return new FlatMapFunction<TupleRowValues, TupleRotationData>() {

            @Override
            public void flatMap(TupleRowValues value, Collector<TupleRotationData> out)
                    throws Exception {
                transformMultiOut(value, minAvgDist, maxPointsInRotation, distFromRefPoint,out);
            }

        };
    }

    private static Map<Integer, Double> coordinatesAnglesCos;
    private static Map<Integer, Double> coordinatesAnglesSin;
    private static int numOfSample = 761;
    private static double refX = 10;
    private static double refY = 10;

    private static void transformMultiOut(TupleRowValues t, double minAvgDist,
            int maxPointsInRotation, boolean distFromRefPoint, Collector<TupleRotationData> out) {

        assert (t.f2.size() == numOfSample);
        double FOV = Math.toRadians(190);
        double res = FOV / t.f2.size();
        double startAngle = -FOV / 2;

        if (coordinatesAnglesCos == null) {
            coordinatesAnglesCos = new HashMap<>();
            coordinatesAnglesSin = new HashMap<>();
            for (int i = 0; i < numOfSample; i++) {
                coordinatesAnglesCos.put(i, Math.cos(startAngle + res * i));
                coordinatesAnglesSin.put(i, Math.sin(startAngle + res * i));
            }
        }

        ArrayList<Double> xValues = new ArrayList<>();
        ArrayList<Double> yValues = new ArrayList<>();
        double distSum = 0;

        for (int i = 0; i < t.f2.size(); i++) {
            double x = t.f2.get(i) * coordinatesAnglesCos.get(i);
            double y = t.f2.get(i) * coordinatesAnglesSin.get(i);

            if (!distFromRefPoint) {
                distSum += t.f2.get(i);
            } else {
                distSum += Math.sqrt(Math.pow(refX - x, 2.0)+Math.pow(refY - y, 2.0));
            }

            xValues.add(x);
            yValues.add(y);

            if (xValues.size() >= maxPointsInRotation && distSum / xValues.size() >= minAvgDist) {
                out.collect(new TupleRotationData(t.f0, t.f1, xValues, yValues, t.f3));
                xValues = new ArrayList<>();
                yValues = new ArrayList<>();
                distSum = 0;
            }

        }

        if (xValues.size() > 0 && distSum / xValues.size() >= minAvgDist) {
            out.collect(new TupleRotationData(t.f0, t.f1, xValues, yValues, t.f3));
        }

    }

    
    private static List<TupleRotationData> transformSingleOut(TupleRowValues t, double minAvgDist,
            int maxPointsInRotation, boolean distFromRefPoint) {

        LinkedList<TupleRotationData> out = new LinkedList<>();

        assert (t.f2.size() == numOfSample);
        double FOV = Math.toRadians(190);
        double res = FOV / t.f2.size();
        double startAngle = -FOV / 2;

        if (coordinatesAnglesCos == null) {
            coordinatesAnglesCos = new HashMap<>();
            coordinatesAnglesSin = new HashMap<>();
            for (int i = 0; i < numOfSample; i++) {
                coordinatesAnglesCos.put(i, Math.cos(startAngle + res * i));
                coordinatesAnglesSin.put(i, Math.sin(startAngle + res * i));
            }
        }

        ArrayList<Double> xValues = new ArrayList<>();
        ArrayList<Double> yValues = new ArrayList<>();
        double distSum = 0;

        for (int i = 0; i < t.f2.size(); i++) {
            double x = t.f2.get(i) * coordinatesAnglesCos.get(i);
            double y = t.f2.get(i) * coordinatesAnglesSin.get(i);

            if (!distFromRefPoint) {
                distSum += t.f2.get(i);
            } else {
                distSum += Math.sqrt(Math.pow(refX - x, 2.0)+Math.pow(refY - y, 2.0));
            }

            xValues.add(x);
            yValues.add(y);

            if (xValues.size() >= maxPointsInRotation && distSum / xValues.size() >= minAvgDist) {
                out.add(new TupleRotationData(t.f0, t.f1, xValues, yValues, t.f3));
                xValues = new ArrayList<>();
                yValues = new ArrayList<>();
                distSum = 0;
            }
        }

        if (xValues.size() > 0 && distSum / xValues.size() >= minAvgDist) {
            out.add(new TupleRotationData(t.f0, t.f1, xValues, yValues, t.f3));
        }

        return out;

    }

    public static SerializableProcessFunction<TupleRowValues, TupleRotationData> instantiateFlatMapFunctionAggBasedMultiOut(
            ExperimentID id) {
        switch (id) {
            case llf:
                return convertPolarAggBasedMultiOut(3, numOfSample + 1, false);
            case alf:
                return convertPolarAggBasedMultiOut(0.0, numOfSample + 1, false);
            case hlf:
                return convertPolarAggBasedMultiOut(0.0, numOfSample / 3 + 1, false);
            case lhf:
                return convertPolarAggBasedMultiOut(13, numOfSample + 1, true);
            case ahf:
                return convertPolarAggBasedMultiOut(0.0, numOfSample + 1, true);
            case hhf:
                return convertPolarAggBasedMultiOut(0.0, numOfSample / 3 + 1, true);
            default:
                throw new RuntimeException("Unrecognized experiment id");
        }
    }

    
    public static SerializableLambdaFlatMap<TupleRowValues, TupleRotationData> instantiateFlatMapFunctionAggBasedSingleOut(
            ExperimentID id) {
        switch (id) {
            case llf:
                return convertPolarAggBasedSingleOut(3, numOfSample + 1, false);
            case alf:
                return convertPolarAggBasedSingleOut(0.0, numOfSample + 1, false);
            case hlf:
                return convertPolarAggBasedSingleOut(0.0, numOfSample / 3 + 1, false);
            case lhf:
                return convertPolarAggBasedSingleOut(13, numOfSample + 1, true);
            case ahf:
                return convertPolarAggBasedSingleOut(0.0, numOfSample + 1, true);
            case hhf:
                return convertPolarAggBasedSingleOut(0.0, numOfSample / 3 + 1, true);
            default:
                throw new RuntimeException("Unrecognized experiment id");
        }
    }

    public static FlatMapFunction<TupleRowValues, TupleRotationData> instantiateFlatMapFunctionNative(
            ExperimentID id) {
        switch (id) {
            case llf:
                return convertPolarNative(3, numOfSample + 1, false);
            case alf:
                return convertPolarNative(0.0, numOfSample + 1, false);
            case hlf:
                return convertPolarNative(0.0, numOfSample / 3 + 1, false);
            case lhf:
                return convertPolarNative(13, numOfSample + 1, true);
            case ahf:
                return convertPolarNative(0.0, numOfSample + 1, true);
            case hhf:
                return convertPolarNative(0.0, numOfSample / 3 + 1, true);
            default:
                throw new RuntimeException("Unrecognized experiment id");
        }
    }
}
