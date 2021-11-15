package com.casm.acled.entities;

public class NoSuchEntityFieldException extends EntitySpecificationException {


    public NoSuchEntityFieldException(String field) {
        super(field + " is not specified on entity.");
    }
}
