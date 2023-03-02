package util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;

public class LoopQueue<T> extends LinkedList<StreamRecord<T5<T>>> {
    
    static private HashMap<String,Object> loops = new HashMap<>();

    synchronized static public <T> Queue<StreamRecord<T5<T>>> factory(String id) {
        if (!loops.containsKey(id)) {
            loops.put(id, new ConcurrentLinkedQueue<StreamRecord<T5<T>>>());
        }
        return (Queue<StreamRecord<T5<T>>>) loops.get(id);
    }

}
