package com.casm.acled.entities.validation;

import com.casm.acled.entities.VersionedEntity;

import java.util.List;

public interface EntityValidator<V extends VersionedEntity<V>> extends Validator {

    List<List<ValidationMessage>> validate(List<V> batch);
//    default List<List<ValidationMessage>> validate(List<V> batch) {
//        List<List<ValidationMessage>> msgs = new ArrayList<>();
//        for(V entry : batch){
//            msgs.add(validate(entry));
//        }
//        return msgs;
//    }
//    List<ValidationMessage> validate(V batch);
}
