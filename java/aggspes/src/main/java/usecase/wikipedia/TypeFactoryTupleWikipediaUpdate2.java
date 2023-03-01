package usecase.wikipedia;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import util.T3;
import util.TypeFactory2;

public class TypeFactoryTupleWikipediaUpdate2 implements TypeFactory2<TupleWikipediaUpdate,TupleWikipediaUpdate> {

    @Override
    public TypeInformation<T3<TupleWikipediaUpdate, TupleWikipediaUpdate>> getT3TypeInformation() {
        return TypeInformation.of(new TypeHint<T3<TupleWikipediaUpdate, TupleWikipediaUpdate>>() {
        });
    }

}
