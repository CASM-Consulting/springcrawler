package com.casm.acled.dao.entities;

import com.casm.acled.dao.LinkDAOImpl;
import com.casm.acled.dao.Tables;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.actordesk.ActorDesk;
import com.casm.acled.entities.desk.Desk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Repository
@Primary
public class ActorDeskDAOImpl extends LinkDAOImpl<Actor, Desk, ActorDesk> implements ActorDeskDAO {

    public ActorDeskDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                            @Value(Tables.T_ACTOR_DESK) String table,
                            @Autowired VersionedEntityRowMapperFactory rowMapperFactory) {
        super(jdbcTemplate, table, ActorDesk.class, rowMapperFactory.ofLink(ActorDesk.class));
    }

}
