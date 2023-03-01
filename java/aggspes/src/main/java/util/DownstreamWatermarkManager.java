package util;

import java.util.Queue;
import java.util.TreeMap;

import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.ChainingStrategy;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;

public class DownstreamWatermarkManager<IN> extends AbstractStreamOperator<T5<IN>>
        implements OneInputStreamOperator<T5<IN>, T5<IN>> {

    private long lastForwardedWatermark;
    private Queue<StreamRecord<T5<IN>>> loopQueue;
    private final String loopQueueID;
    private final TreeMap<Long, Integer> succWins;

    private final boolean handleFlush = false;

    public DownstreamWatermarkManager(String loopQueueID) {
        this.loopQueueID = loopQueueID;
        setChainingStrategy(ChainingStrategy.ALWAYS);
        this.succWins = new TreeMap<>();
    }

    @Override
    public void open() throws Exception {
        super.open();
        this.loopQueue = LoopQueue.factory(loopQueueID);
    }

    @Override
    public void processElement(StreamRecord<T5<IN>> t) throws Exception {

        loopQueue.add(t);
        output.collect(t);
        if (t.getValue().f1 == 1) {
            if (t.getValue().f0.size() > 1) {
                if (!succWins.containsKey(t.getTimestamp())) {
                    succWins.put(t.getTimestamp(), t.getValue().getOutSize() - 1);
                } else {
                    succWins.put(t.getTimestamp(), succWins.get(t.getTimestamp()) + t.getValue().getOutSize() - 1);
                }
            }
        } else {
            succWins.put(t.getTimestamp(), succWins.get(t.getTimestamp()) - 1);
            if (succWins.get(t.getTimestamp()) == 0) {
                succWins.remove(t.getTimestamp());
            }
            if (succWins.isEmpty()) {
                super.processWatermark(new Watermark(t.getTimestamp()));
                lastForwardedWatermark = t.getTimestamp();
            } else if (succWins.firstKey() - 1 > lastForwardedWatermark) {
                super.processWatermark(new Watermark(succWins.firstKey() - 1));
                lastForwardedWatermark = t.getTimestamp();
            }
        }

    }

    @Override
    public void processWatermark(Watermark mark) throws Exception {
        if (succWins.isEmpty()) {
            super.processWatermark(mark);
            lastForwardedWatermark = mark.getTimestamp();
        } else if (!handleFlush && mark.getTimestamp() == Long.MAX_VALUE) {
            super.processWatermark(mark);
        } else {
            if (succWins.firstKey() - 1 > lastForwardedWatermark) {
                super.processWatermark(new Watermark(succWins.firstKey() - 1));
                lastForwardedWatermark = succWins.firstKey() - 1;
            }
        }

    }

}