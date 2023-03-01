package util;

import org.apache.flink.api.common.typeinfo.TypeInformation;

public interface TypeFactory1<T> {
    
    TypeInformation<T5<T>> getT5TypeInformation();

    TypeInformation<T> getTTypeInformation();

}
