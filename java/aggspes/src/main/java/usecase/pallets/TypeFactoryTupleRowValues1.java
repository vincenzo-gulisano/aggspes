package usecase.pallets;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import util.T5;
import util.TypeFactory1;

public class TypeFactoryTupleRowValues1 implements TypeFactory1<TupleRowValues> {

    @Override
    public TypeInformation<T5<TupleRowValues>> getT5TypeInformation() {
        return TypeInformation.of(new TypeHint<T5<TupleRowValues>>() {
        });
    }

    @Override
    public TypeInformation<TupleRowValues> getTTypeInformation() {
        return TypeInformation.of(new TypeHint<TupleRowValues>() {
        });
    }

}
