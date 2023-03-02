package util;

import java.util.ArrayList;
import java.util.Queue;
import java.util.TreeMap;

import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.ChainingStrategy;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpstreamWatermarkManager<IN> extends AbstractStreamOperator<T5<IN>>
    implements OneInputStreamOperator<T5<IN>, T5<IN>> {

  private Queue<StreamRecord<T5<IN>>> loopQueue;
  private final long L;
  private long watermarkBound;
  private final TreeMap<Long, Integer> succWins;
  private final ArrayList<Watermark> pendingWatermarks;
  private final String loopQueueID;

  private final boolean handleFlush = false;


  public UpstreamWatermarkManager(String loopQueueID, long allowedLateness) {
    this.L = allowedLateness;
    this.watermarkBound = Long.MAX_VALUE;

    this.succWins = new TreeMap<>();
    this.pendingWatermarks = new ArrayList<>();
  
    this.loopQueueID = loopQueueID;

    setChainingStrategy(ChainingStrategy.ALWAYS);
  }

  @Override
  public void open() throws Exception {
    super.open();
    this.loopQueue = LoopQueue.factory(loopQueueID);
  }

  private void handleLoop() throws Exception {
    while (!loopQueue.isEmpty()) {

      StreamRecord<T5<IN>> tLoop = loopQueue.poll();
      output.collect(tLoop);
      succWins.put(tLoop.getTimestamp(), succWins.get(tLoop.getTimestamp()) - 1);
      
      if (succWins.get(tLoop.getTimestamp()) == 0) {
        succWins.remove(tLoop.getTimestamp());
      }
    }
  }

  @Override
  public void processElement(StreamRecord<T5<IN>> t) throws Exception {

    // Handle Loop
    handleLoop();

    // Now process the tuple from E
    output.collect(t);
    if (!succWins.containsKey(t.getTimestamp())) {
      succWins.put(t.getTimestamp(), t.getValue().getOutSize());
    } else {
      succWins.put(t.getTimestamp(), succWins.get(t.getTimestamp()) + t.getValue().getOutSize());
    }

    // Here I now succWins is not empty, because I just added a tuple in it
    watermarkBound = succWins.firstKey() + L;

    Watermark nextWatermark = new Watermark(-1);
    while (!pendingWatermarks.isEmpty() && pendingWatermarks.get(0).getTimestamp() <= watermarkBound) {
      nextWatermark = pendingWatermarks.remove(0);
    }

    if (nextWatermark.getTimestamp() != -1) {
      super.processWatermark(nextWatermark);
    }

  }

  @Override
  public void processWatermark(Watermark mark) throws Exception {

    // Handle Loop
    handleLoop();
    if (mark.getTimestamp() <= watermarkBound) {
      super.processWatermark(mark);
    } else {
      if (mark.getTimestamp() < Long.MAX_VALUE) {
        pendingWatermarks.add(mark);
      } else {
        if (handleFlush) {
          pendingWatermarks.add(new Watermark(watermarkBound));
          while (!succWins.isEmpty()) {
            handleLoop();
            if (succWins.isEmpty()) {
              watermarkBound = Long.MAX_VALUE;
            } else {
              watermarkBound = succWins.firstKey() + L;
            }
            Watermark nextWatermark = new Watermark(-1);
            while (!pendingWatermarks.isEmpty() && pendingWatermarks.get(0).getTimestamp() <= watermarkBound) {
              nextWatermark = pendingWatermarks.remove(0);
            }
            if (nextWatermark.getTimestamp() != -1) {
              super.processWatermark(nextWatermark);
            }
            Thread.sleep(100);
          }
        } else {
          System.out.println("Received flush, but handlingFlush is disabled, so just forwarding it");
        }
        super.processWatermark(mark);
      }

    }

  }

}