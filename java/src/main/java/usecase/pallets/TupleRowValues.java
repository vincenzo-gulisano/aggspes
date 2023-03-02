package usecase.pallets;

import java.util.ArrayList;

import org.apache.flink.api.java.tuple.Tuple4;

import util.TimestampedTuple;

public class TupleRowValues extends Tuple4<Long,Integer,ArrayList<Double>,Long> implements TimestampedTuple {

    public TupleRowValues() {
    }

    public TupleRowValues(Long f0, int f1, ArrayList<Double> f2, Long f3) {
        super(f0, f1, f2, f3);
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
        return f3;
    }

    @Override
    public void setStimulus(long stimulus) {
        f3 = stimulus;
    }

}
