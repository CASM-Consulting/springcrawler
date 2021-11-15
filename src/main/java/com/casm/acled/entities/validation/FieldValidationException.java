package com.casm.acled.entities.validation;

import com.casm.acled.entities.VersionedEntityException;

public class FieldValidationException extends VersionedEntityException {
    public FieldValidationException(String msg) {
        super(msg);
    }
}
