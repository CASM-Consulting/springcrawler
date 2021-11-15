package com.casm.acled.dao.entities;

import com.casm.acled.dao.HasDesk;
import com.casm.acled.dao.VersionedEntityDAO;
import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.desk.Desk;

import java.util.List;
import java.util.Optional;

public interface ActorDAO extends VersionedEntityDAO<Actor>, HasDesk<Actor> {

    List<Actor> byRegion(Desk region);
    Optional<Actor> byName(String name);
//    List<Actor> byDesk(Integer deskId);

}
