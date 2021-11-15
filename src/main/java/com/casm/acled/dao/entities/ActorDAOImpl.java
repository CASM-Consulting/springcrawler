package com.casm.acled.dao.entities;

import com.casm.acled.dao.Tables;
import com.casm.acled.dao.VersionedEntityDAOImpl;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.dao.util.SqlBinder;
import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.desk.Desk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
@Repository
@Primary
public class ActorDAOImpl extends VersionedEntityDAOImpl<Actor> implements ActorDAO  {

    public ActorDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                        @Autowired VersionedEntityRowMapperFactory rowMapperFactory,
                        @Value(Tables.T_ACTOR) String table) {
        super(jdbcTemplate, table, Actor.class, rowMapperFactory.of(Actor.class));
    }


    @Override
    public List<Actor> byRegion(Desk region) {
        return null;
    }

    @Override
    public Optional<Actor> byName(String name) {
        Optional<Actor> maybeActor = getByUnique(Actor.ACTOR_NAME, name);
        return maybeActor;
    }

    @Override
    public List<Actor> byDesk(Integer deskId) {

        String sql = SqlBinder.sql("SELECT A.id AS A_ID,",
                "A.data AS A_DATA",
                "FROM ${table} AS A",
                "LEFT JOIN ${join_table} AS AD ON (AD.id1 = A.id)",
                "WHERE AD.id2 = ?")
                .bind("table", table)
                .bind("join_table", Tables.T_ACTOR_DESK)
                .bind();

        List<Actor> actors = query(sql, deskId);
        return actors;
    }

}
