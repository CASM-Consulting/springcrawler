package com.casm.acled.dao;

import com.casm.acled.camunda.variables.Entities;
import com.casm.acled.dao.entities.*;
import com.casm.acled.dao.entities.history.EventHistoryDAO;
import com.casm.acled.entities.VersionedEntity;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class VersionedEntityDAOs {

    private ActorDAO actorDAO;
    private ActorDeskDAO actorDeskDAO;
    private LocationDAO locationDAO;
    private LocationDeskDAO locationDeskDAO;
    private SourceDAO sourceDAO;
    private DeskDAO deskDAO;
    private SourceListDAO sourceListDAO;
    private SourceDeskDAO sourceDeskDAO;
    private SourceSourceListDAO sourceSourceListDAO;
    private EventDAO eventDAO;
    private EventHistoryDAO eventHistoryDAO;

    private final Map<String, VersionedEntityDAO> daos;
    private final Map<String, Map<String, LinkDAO>> links;

    @Autowired
    public VersionedEntityDAOs(ActorDAO actorDAO,
                               LocationDAO locationDAO,
                               LocationDeskDAO locationDeskDAO,
                               SourceDAO sourceDAO,
                               SourceDeskDAO sourceDeskDAO,
                               DeskDAO deskDAO,
                               ActorDeskDAO actorDeskDAO,
                               SourceSourceListDAO sourceSourceListDAO,
                               SourceListDAO sourceListDAO,
                               EventDAO eventDAO,
                               EventHistoryDAO eventHistoryDAO
                               ) {
        this.actorDAO = actorDAO;
        this.actorDeskDAO = actorDeskDAO;
        this.locationDAO = locationDAO;
        this.sourceDAO = sourceDAO;
        this.sourceDeskDAO = sourceDeskDAO;
        this.deskDAO = deskDAO;
        this.sourceSourceListDAO = sourceSourceListDAO;
        this.sourceListDAO = sourceListDAO;
        this.locationDeskDAO = locationDeskDAO;
        this.eventDAO = eventDAO;
        this.eventHistoryDAO = eventHistoryDAO;

        daos = ImmutableMap.<String, VersionedEntityDAO>builder()
                .put(Entities.ACTOR, actorDAO)
                .put(Entities.LOCATION, locationDAO)
                .put(Entities.SOURCE, sourceDAO)
                .put(Entities.SOURCE_LIST, sourceListDAO)
                .put(Entities.DESK, deskDAO)
                .put(Entities.EVENT, eventDAO)
                .put(Entities.EVENT_HISTORY, eventHistoryDAO)
                .build();

        links = ImmutableMap.<String, Map<String,LinkDAO>>builder()
                .put(Entities.ACTOR, ImmutableMap.of(Entities.DESK, actorDeskDAO))
                .put(Entities.LOCATION, ImmutableMap.of(Entities.DESK, locationDeskDAO))
                .put(Entities.SOURCE, ImmutableMap.of(
                        Entities.DESK, sourceDeskDAO,
                        Entities.SOURCE_LIST, sourceSourceListDAO
                ))
                .build();
    }

    public <V extends VersionedEntity<V>> VersionedEntityDAO<V> get(String type) {
        return daos.get(type);
    }

    public  LinkDAO link(String from, String to) {
        return links.get(from).get(to);
    }

//    private static VersionedEntityDAOs instance;
//    public synchronized static VersionedEntityDAOs getInstance() {
//        if(instance == null) {
//            instance = new VersionedEntityDAOs();
//        }
//        return instance;
//    }
}
