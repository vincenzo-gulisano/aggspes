package usecase.pallets;

import java.util.ArrayList;

import org.apache.flink.api.java.tuple.Tuple5;

import util.TimestampedTuple;

public class TupleRotationData extends Tuple5<Long, Integer, ArrayList<Double>, ArrayList<Double>, Long>
        implements TimestampedTuple {

    public TupleRotationData() {
    }

    public TupleRotationData(Long ts, int id, ArrayList<Double> x, ArrayList<Double> y, Long stimulus) {
        super(ts,id, x, y, stimulus);
    }

    @Override
    public long getTimestamp() {
        return f0;
    }

    @Override
    public void setTimestamp(long timestamp) {
        f0 = timestamp;
    }

    @Override
    public long getStimulus() {
        return f4;
    }

    @Override
    public void setStimulus(long stimulus) {
        f4 = stimulus;
    }

}
