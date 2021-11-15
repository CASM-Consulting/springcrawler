package com.casm.acled.dao.entities;

import com.casm.acled.dao.LinkDAO;
import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.actordesk.ActorDesk;
import com.casm.acled.entities.desk.Desk;

public interface ActorDeskDAO extends LinkDAO<Actor, Desk, ActorDesk> {

}
