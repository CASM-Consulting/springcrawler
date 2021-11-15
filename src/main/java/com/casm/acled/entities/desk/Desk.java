package com.casm.acled.entities.desk;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntity;

import java.util.Map;

public class Desk extends VersionedEntity<Desk> {

    public static final String DESK_NAME = "DESK_NAME";

    public Desk(EntitySpecification entitySpecification, String version, Map<String, Object> data, Integer id) {
        super(entitySpecification, version, data, id);
    }
}