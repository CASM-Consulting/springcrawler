package com.casm.acled.dao.entities;

import com.casm.acled.dao.LinkDAOImpl;
import com.casm.acled.dao.Tables;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcedesk.SourceDesk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Repository
@Primary
public class SourceDeskDAOImpl extends LinkDAOImpl<Source, Desk, SourceDesk> implements SourceDeskDAO {

    public SourceDeskDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                             @Value(Tables.T_SOURCE_DESK) String table,
                             @Autowired VersionedEntityRowMapperFactory rowMapperFactory) {
        super(jdbcTemplate, table, SourceDesk.class,  rowMapperFactory.ofLink(SourceDesk.class));
    }
}
