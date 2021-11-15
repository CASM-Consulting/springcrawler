package com.casm.acled.entities.feedback;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntity;

import java.util.Map;

public class Feedback extends VersionedEntity<Feedback> {

    public static final String TEXT = "FEEDBACK";
    public static final String AUTHOR = "AUTHOR";
    public static final String RECIPIENT = "_RECIPIENT";
    public static final String TARGET_ENTITY = "_TARGET_ENTITY";
    public static final String TARGET_ID = "_TARGET_ID";

    public Feedback(EntitySpecification entitySpecification, String version, Map<String, Object> data, Integer id) {
        super(entitySpecification, version, data, id);
    }
}
