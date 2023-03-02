package usecase.pallets;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import util.T3;
import util.TypeFactory2;

public class TypeFactoryTupleRowValues2 implements TypeFactory2<TupleRowValues,TupleRowValues> {

    @Override
    public TypeInformation<T3<TupleRowValues, TupleRowValues>> getT3TypeInformation() {
        return TypeInformation.of(new TypeHint<T3<TupleRowValues, TupleRowValues>>() {
        });
    }

}
