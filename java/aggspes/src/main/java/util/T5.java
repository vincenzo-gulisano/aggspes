package util;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.flink.api.java.tuple.Tuple2;

public class T5<T> extends Tuple2<List<T>,Integer> {// extends Tuple5<List<T>,Integer,Integer,Integer,Integer> {


    public T5() {
    }

    public T5(List<T> f0, Integer f1) {
        super(f0, f1);
    }

    public List<T> getOutputTuples() {
        return f0;
    }
    
    public int getOutputTupleIndex() {
        return f1;
    }

    public int getOutSize() {
        return f0.size();
    }

    @Override
    public String toString() {
        return f0+","+f1;
    }

    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
            append(f0).
            append(f1).
            toHashCode();
    }
    
    public boolean equals(Object obj) {
    
        if (obj == this)
            return true;
        if (!(obj instanceof T5))
            return false;
    
        T5 rhs = (T5) obj;
        return new EqualsBuilder().
            append(f0, rhs.f0).
            append(f1, rhs.f1).
            isEquals();
    }

}
