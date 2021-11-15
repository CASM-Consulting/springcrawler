package com.casm.acled.entities;

public class VersionedEntityException extends RuntimeException {


    public VersionedEntityException(Throwable t) {
        super(t);
    }
    public VersionedEntityException(String msg) {
        super(msg);
    }
}
