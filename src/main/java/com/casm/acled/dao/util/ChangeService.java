package com.casm.acled.dao.util;


import com.casm.acled.AcledObjectMapper;
import com.casm.acled.camunda.variables.Entities;
import com.casm.acled.dao.VersionedEntityDAO;
import com.casm.acled.dao.VersionedEntityDAOs;
import com.casm.acled.dao.entities.ChangeDAO;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.change.Change;
import com.casm.acled.entities.event.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.casm.acled.entities.change.Change.*;

@Service
public class ChangeService {

    @Autowired
    private ChangeDAO changeDAO;

    @Autowired
    private VersionedEntityDAOs entityDAOs;

    private ObjectMapper om = AcledObjectMapper.get();


    public <V extends VersionedEntity<V>> List<V> computeChanges(String businessKey) {

        List<Change> changes = changeDAO.getByBusinessKey(businessKey);

        Map<Integer, V> changeables = new HashMap<>();

        for(Change change : changes) {

            String targetType = change.get(TARGET_TYPE);
            int id = change.get(TARGET_ID);

            VersionedEntityDAO<V> dao = entityDAOs.get(targetType);

            Optional<V> maybeChangeable;
            if(changeables.containsKey(id)){
                maybeChangeable = Optional.of(changeables.get(id));
            } else {
                maybeChangeable = dao.getById(id);
            }

            if(maybeChangeable.isPresent()) {
                V changeable = maybeChangeable.get();

                if(change.isTrue(DELETE)) {
                    changeable = changeable.put(Entities.DELETED, true);
                } else {
                    changeable = doChange(change, changeable, dao);
                }

                changeables.put(id, changeable);
            }
        }

        return new ArrayList<>(changeables.values());
    }


    public <V extends VersionedEntity<V>> Optional<V> computeChange(Change change) {
        String targetType = change.get(TARGET_TYPE);
        int id = change.get(TARGET_ID);

        VersionedEntityDAO<V> dao = entityDAOs.get(targetType);

        Optional<V> maybeChange = Optional.empty();

        if(change.isTrue(DELETE)) {
            dao.delete(id);
        } else {
            Optional<V> maybeEntity = dao.getById(id);
            if(maybeEntity.isPresent()) {
                V entity = maybeEntity.get();

                entity = doChange(change, entity, dao);

                maybeChange = Optional.of(entity);
            }
        }
        return maybeChange;
    }

    public <V extends VersionedEntity<V>> void implementRequests(String businessKey) {

        List<Change> changes = changeDAO.getByBusinessKey(businessKey);

        for(Change change : changes) {

            implementRequest(change);
        }
    }

    public <V extends VersionedEntity<V>> void implementRequest(Change change) {
        String targetType = change.get(TARGET_TYPE);
        int id = change.get(TARGET_ID);

        VersionedEntityDAO<V> dao = entityDAOs.get(targetType);

        if(change.isTrue(DELETE)) {
            dao.delete(id);
            changeDAO.overwrite(change.put(IMPLEMENTED, true));
        } else {
            Optional<V> maybeEntity = dao.getById(id);
            if(maybeEntity.isPresent()) {
                V entity = maybeEntity.get();

                entity = doChange(change, entity, dao);

                dao.overwrite(entity);
                changeDAO.overwrite(change.put(IMPLEMENTED, true));
            }
        }
    }

    private <V extends VersionedEntity<V>> V doChange(Change change, V entity, VersionedEntityDAO<V> dao) {
        String field = change.get(TARGET_FIELD);
        String to = change.get(TO);

        // If there is no "to" change, then it means we should delete the field
        if (to == null){
            entity = entity.remove(field);
        } else {
            ObjectNode temp = om.createObjectNode();
            temp.put(field, to);
            V entityChange = dao.decode(temp.toString());

            entity = entity.put(field, entityChange.get(field));
        }

        return entity;
    }


    public <V extends VersionedEntity<V>> List<Event> getEventChanges(String businessKey) {

        List<V> changes = computeChanges(businessKey);

        List<Event> events = new ArrayList<>();

        for(V change : changes) {
            if(Event.class.isAssignableFrom(change.getClass())) {
                events.add((Event)change);
            }
        }

        return events;
    }
}
