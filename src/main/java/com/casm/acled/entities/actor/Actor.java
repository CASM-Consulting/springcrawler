package com.casm.acled.entities.actor;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntity;

import java.util.Map;

public class Actor extends VersionedEntity<Actor>  {

    public static final String ACTOR_NAME = "ACTOR_NAME";
    public static final String INTER = "INTER";
    public static final String MAIN_COUNTRY = "MAIN_COUNTRY";
    public static final String DATE_ADDED = "DATE_ADDED";
    public static final String MANDATORY_ASSOCIATE_ACTOR = "MANDATORY_ASSOCIATE_ACTOR";
    public static final String MANDATORY_ACTOR1_2 = "MANDATORY_ACTOR1_2";
    public static final String ALIAS_CODING_NOTES = "ALIAS_CODING_NOTES";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String LINKS = "LINKS";
    public static final String PAUSED_MANDATORY_ASSOCIATE_ACTOR = "PAUSED_MANDATORY_ASSOCIATE_ACTOR";
    public static final String VERIFIED = "VERIFIED";

    public Actor(EntitySpecification entitySpecification, String version, Map<String, Object> data, Integer id) {
        super(entitySpecification, version, data, id);
    }
}
