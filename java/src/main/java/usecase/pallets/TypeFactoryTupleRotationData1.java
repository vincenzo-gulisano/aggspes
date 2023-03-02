package usecase.pallets;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import util.T5;
import util.TypeFactory1;

public class TypeFactoryTupleRotationData1 implements TypeFactory1<TupleRotationData> {

    @Override
    public TypeInformation<T5<TupleRotationData>> getT5TypeInformation() {
        return TypeInformation.of(new TypeHint<T5<TupleRotationData>>() {
        });
    }

    @Override
    public TypeInformation<TupleRotationData> getTTypeInformation() {
        return TypeInformation.of(new TypeHint<TupleRotationData>() {
        });
    }

}
