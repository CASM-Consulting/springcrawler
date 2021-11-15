package com.casm.acled.dao.rowmappers;

import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.VersionedEntityLink;
import com.casm.acled.entities.event.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VersionedEntityRowMapperFactory {

    private static final Logger LOG = LoggerFactory.getLogger(VersionedEntityRowMapperFactory.class);

    private final ObjectMapper om;

    public VersionedEntityRowMapperFactory(@Autowired ObjectMapper om) {
        this.om = om;
    }

    public <V extends VersionedEntity<V>> VersionedEntityRowMapper<V> of(Class<V> klass) {
        if ( klass.equals(Event.class) ) {
            return ( VersionedEntityRowMapper<V>) new EventRowMapper(om);
        } else  {
            return new VersionedEntityRowMapper<>(klass, om);
        }
    }

    public <V extends VersionedEntityLink<V>> VersionedEntityLinkRowMapper<V> ofLink(Class<V> klass) {
        return new VersionedEntityLinkRowMapper<>(klass, om);
    }

    public <V extends VersionedEntity<V>> VersionedEntityRowMapper<V> ofDefault(Class<V> klass) {
        return new VersionedEntityRowMapper<>(klass, om);
    }
}
