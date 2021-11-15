package com.casm.acled.dao.entities;

import com.casm.acled.dao.LinkDAOImpl;
import com.casm.acled.dao.Tables;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.location.Location;
import com.casm.acled.entities.locationdesk.LocationDesk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Repository
@Primary
public class LocationDeskDAOImpl extends LinkDAOImpl<Location, Desk, LocationDesk> implements LocationDeskDAO {

    public LocationDeskDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                               @Value(Tables.T_LOCATION_DESK) String table,
                               @Autowired VersionedEntityRowMapperFactory rowMapperFactory) {
        super(jdbcTemplate, table, LocationDesk.class, rowMapperFactory.ofLink(LocationDesk.class));
    }
}
