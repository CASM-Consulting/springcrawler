package com.casm.acled.entities.change;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntity;

import java.util.Map;

public class Change extends VersionedEntity<Change> {

    public static final String TARGET_TYPE = "TARGET_TYPE";
    public static final String TARGET_ID = "TARGET_ID";
    public static final String TARGET_FIELD = "TARGET_FIELD";
    public static final String FROM = "FROM";
    public static final String TO = "TO";
    public static final String BY = "BY";
    public static final String DATE_TIME = "DATE_TIME";
    public static final String DELETE = "DELETE";
    public static final String IMPLEMENTED = "IMPLEMENTED";

    public Change(EntitySpecification entitySpecification, String version, Map<String, Object> data, Integer id) {
        super(entitySpecification, version, data, id);
    }

}
