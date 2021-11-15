package com.casm.acled.entities.validation;

import com.casm.acled.entities.EntityField;
import com.casm.acled.entities.VersionedEntity;

import java.util.List;

public interface FieldValidator<T> {

    <V extends VersionedEntity<V>> List<ValidationMessage> validate(VersionedEntity<V> entity, EntityField<T> field, T val);
}
