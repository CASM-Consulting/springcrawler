package com.casm.acled.entities.actordesk.versions;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.actordesk.ActorDesk;

import java.util.Map;

public class ActorDesk_v1 extends ActorDesk {

    private static final EntitySpecification SPECIFICATION = EntitySpecification.empty()
            .add(ACTOR_ID, Integer.class, "actor")
            .add(DESK_ID, Integer.class, "desk");

    public ActorDesk_v1(Map<String, Object> data, Integer id, Integer id1, Integer id2) {
        super(SPECIFICATION, "v1", data, id, id1, id2);
    }

}
