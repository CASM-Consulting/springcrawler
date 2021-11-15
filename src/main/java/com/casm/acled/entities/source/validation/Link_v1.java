package com.casm.acled.entities.source.validation;

import com.casm.acled.entities.EntityField;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.validation.FieldValidator;
import com.casm.acled.entities.validation.ValidationMessage;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class Link_v1 implements FieldValidator<String> {

    public Link_v1() {

    }

    @Override
    public <V extends VersionedEntity<V>> List<ValidationMessage> validate(VersionedEntity<V> entity, EntityField<String> field, String val) {

        return ImmutableList.of();
    }

}
