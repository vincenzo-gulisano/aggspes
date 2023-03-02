package util;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.flink.api.java.tuple.Tuple2;

public class T3<T1, T2> extends Tuple2<List<T1>, List<T2>> {

    public T3(List<T1> f0, List<T2> f1) {
        super(f0, f1);
    }

    public boolean isFromA1() {
        return !f0.isEmpty() ? true : false;
    }

    public T3() {
    }

    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
                append(f0).append(f1).toHashCode();
    }

    public boolean equals(Object obj) {

        if (obj == this)
            return true;
        if (!(obj instanceof T3))
            return false;

        T3<T1, T2> rhs = (T3<T1, T2>) obj;
        return new EqualsBuilder().append(f0, rhs.f0).append(f1, rhs.f1).isEquals();
    }

}
