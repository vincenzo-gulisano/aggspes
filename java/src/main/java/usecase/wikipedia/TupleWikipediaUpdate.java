package usecase.wikipedia;

import org.apache.flink.api.java.tuple.Tuple5;

import util.TimestampedTuple;

public class TupleWikipediaUpdate extends Tuple5<Long,String,String,String,Long> implements TimestampedTuple {

    public TupleWikipediaUpdate() {
    }

    public TupleWikipediaUpdate(Long f0, String f1, String f2, String f3, Long f4) {
        super(f0, f1, f2, f3, f4);
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
