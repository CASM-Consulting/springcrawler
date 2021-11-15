package com.casm.acled.entities.actordesk;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntityLink;

import java.util.Map;

public class ActorDesk extends VersionedEntityLink<ActorDesk> {

    public static final String ACTOR_ID = ID1;
    public static final String DESK_ID = ID2;

    public ActorDesk(EntitySpecification entitySpecification, String version, Map<String, Object> data, Integer id, Integer id1, Integer id2) {
        super(entitySpecification, version, data, id, id1, id2);
    }


}