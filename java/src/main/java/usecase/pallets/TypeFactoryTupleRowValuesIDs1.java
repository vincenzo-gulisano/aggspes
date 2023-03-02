package usecase.pallets;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import util.T5;
import util.TypeFactory1;

public class TypeFactoryTupleRowValuesIDs1 implements TypeFactory1<TupleRowValuesIDs> {

    @Override
    public TypeInformation<T5<TupleRowValuesIDs>> getT5TypeInformation() {
        return TypeInformation.of(new TypeHint<T5<TupleRowValuesIDs>>() {
        });
    }

    @Override
    public TypeInformation<TupleRowValuesIDs> getTTypeInformation() {
        return TypeInformation.of(new TypeHint<TupleRowValuesIDs>() {
        });
    }

}
