package com.casm.acled.dao.rowmappers;

import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.location.Location;
import com.casm.acled.entities.source.Source;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EventRowMapper extends VersionedEntityRowMapper<Event> {

    private static final Logger LOG = LoggerFactory.getLogger(EventRowMapper.class);

    public EventRowMapper(ObjectMapper om) {
        super(Event.class, om);
    }

    @Override
    public Event mapRow(ResultSet row, int i) throws SQLException {

        try {
            Integer eventId = row.getInt("e_id");
            String eventData = row.getString("e_data");
            Integer articleId = row.getInt("a_id");
            String articleData = row.getString("a_data");
            Integer sourceId = row.getInt("s_id");
            String sourceData = row.getString("s_data");

            String actor1Data = row.getString("actor1_data");
            Integer actor1Id = row.getInt("actor1_id");

            String actor2Data = row.getString("actor2_data");
            Integer actor2Id = row.getInt("actor1_id");

            String assocActor1Data = row.getString("assoc_actor1_data");
            Integer assocActor1Id = row.getInt("assoc_actor1_id");

            String assocActor2Data = row.getString("assoc_actor2_data");
            Integer assocActor2Id = row.getInt("assoc_actor2_id");

            String locationData = row.getString("l_data");
            Integer locationId = row.getInt("l_id");

            Event event = om.readValue(eventData, Event.class).id(eventId);

            if(articleData != null ){
                Article article = om.readValue(articleData, Article.class).id(articleId);
                event = event.article(article);
            }

            if (sourceData != null){
                Source source = om.readValue(sourceData, Source.class).id(sourceId);
                event = event.source(source);
            }

            if (actor1Data != null){
                Actor actor = om.readValue(actor1Data, Actor.class).id(actor1Id);
                event = event.actor1(actor);
            }

            if (actor2Data != null){
                Actor actor = om.readValue(actor2Data, Actor.class).id(actor2Id);
                event = event.actor2(actor);
            }

            if (assocActor1Data != null){
                Actor actor = om.readValue(assocActor1Data, Actor.class).id(assocActor1Id);
                event = event.assocActors1(ImmutableList.of(actor));
            }

            if (assocActor2Data != null){
                Actor actor = om.readValue(assocActor2Data, Actor.class).id(assocActor2Id);
                event = event.assocActors2(ImmutableList.of(actor));
            }

            if (locationData != null){
                Location location = om.readValue(locationData, Location.class).id(locationId);
                event = event.location(location);
            }

            return event;

        } catch (NullPointerException e) {
            LOG.error("{} : {}", klass, i);
            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
