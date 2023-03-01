package usecase.wikipedia;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import util.T5;
import util.TypeFactory1;

public class TypeFactoryTupleWikipediaUpdate1 implements TypeFactory1<TupleWikipediaUpdate> {

    @Override
    public TypeInformation<T5<TupleWikipediaUpdate>> getT5TypeInformation() {
        return TypeInformation.of(new TypeHint<T5<TupleWikipediaUpdate>>() {
        });
    }

    @Override
    public TypeInformation<TupleWikipediaUpdate> getTTypeInformation() {
        return TypeInformation.of(new TypeHint<TupleWikipediaUpdate>() {
        });
    }

}
