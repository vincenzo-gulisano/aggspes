package usecase.pallets;

import org.apache.flink.api.java.tuple.Tuple4;

import util.TimestampedTuple;

public class TupleRowValuesIDs extends Tuple4<Long, Integer, Integer, Long> implements TimestampedTuple {

    public TupleRowValuesIDs() {
    }

    public TupleRowValuesIDs(Long f0, int f1, int f2, Long f3) {
        super(f0, f1, f2, f3);
    }

    public TupleRowValuesIDs(TupleRowValues first, TupleRowValues second) {
        super(first.f0 > second.f0 ? first.f0 : second.f0, first.f1, second.f1,
                first.f3 > second.f3 ? first.f3 : second.f3);
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
