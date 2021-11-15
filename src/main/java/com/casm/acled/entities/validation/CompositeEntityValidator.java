package com.casm.acled.entities.validation;

import com.casm.acled.entities.VersionedEntity;

import java.util.List;

public class CompositeEntityValidator<V extends VersionedEntity<V>, E extends EntityValidator<V>> implements EntityValidator<V> {



    public CompositeEntityValidator(E validator) {

    }

    @Override
    public List<List<ValidationMessage>> validate(List<V> batch) {
        return null;
    }
}
