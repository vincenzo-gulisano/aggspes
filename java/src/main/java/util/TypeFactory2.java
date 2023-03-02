package util;

import org.apache.flink.api.common.typeinfo.TypeInformation;

public interface TypeFactory2<T1,T2> {
    
    TypeInformation<T3<T1,T2>> getT3TypeInformation();

}
