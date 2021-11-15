package com.casm.acled.dao.entities.history;

import com.casm.acled.camunda.variables.Entities;
import com.casm.acled.dao.CancelCreateException;
import com.casm.acled.dao.entities.EventDAOImpl;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.location.Location;
import com.neovisionaries.i18n.CountryCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Repository
public class EventHistoryDAOImpl extends EventDAOImpl implements EventHistoryDAO {

    public EventHistoryDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                               @Autowired VersionedEntityRowMapperFactory rowMapperFactory,
                               @Autowired ArticleEventHistoryDAO articleEventHistoryDAO) {
        super(jdbcTemplate, rowMapperFactory, articleEventHistoryDAO,"ACLED_hi_event");
    }

    @Override
    protected Event preCreate(Event event) throws CancelCreateException {

        if(event.isTrue(Entities.HISTORICAL)) {

            event = event.id(event.get(Entities.HISTORICAL_ID));
            overwrite(event);

            throw new CancelCreateException();

        } else {

            if(event.get(Event.API_ID) == null) {

                Integer isoId =  event.location().get(Location.ISO);
                int n = getNextID(isoId);
                String isoA3;
                try {
                    isoA3 = CountryCode.getByCode(isoId).getAlpha3();
                } catch (NullPointerException npe) {
                    String country = event.location().get(Location.COUNTRY);
                    isoA3 = CountryCode.findByName(country).get(0).getAlpha3();
                }

                event = event.put(Event.EVENT_ID_NO_CNTY, n);
                event = event.put(Event.EVENT_ID_CNTY, isoA3 + n);
            }

            event = event.put(Entities.HISTORICAL, true);

            return event;
        }

    }

    private int getNextID(Integer isoId) {
        String sql = joinedSql().append(
                "WHERE L.data->>'${ISO}' = ?",
                "ORDER BY E.data->'EVENT_ID_NO_CNTY' DESC",
                "LIMIT 1")
                .bind("EVENT_ID_NO_CNTY", Event.EVENT_ID_NO_CNTY)
                .bind("EVENT_LOCATION", Event.EVENT_LOCATION)
                .bind("ISO", Location.ISO)
                .bind();
        List<Event> result = query(sql, isoId);
        int id;
        if(result.isEmpty()) {
            id = 0;
        } else {
            id = result.get(0).get(Event.EVENT_ID_NO_CNTY);
        }
        return id+1;
    }
}
