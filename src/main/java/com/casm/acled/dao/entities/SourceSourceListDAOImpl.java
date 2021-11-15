package com.casm.acled.dao.entities;

import com.casm.acled.dao.LinkDAOImpl;
import com.casm.acled.dao.Tables;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.casm.acled.entities.sourcesourcelist.SourceSourceList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Repository
@Primary
public class SourceSourceListDAOImpl extends LinkDAOImpl<Source, SourceList, SourceSourceList> implements SourceSourceListDAO {

    public SourceSourceListDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                                   @Value(Tables.T_SOURCE_SOURCE_LIST) String table,
                                   @Autowired VersionedEntityRowMapperFactory rowMapperFactory
    ) {
        super(jdbcTemplate, table, SourceSourceList.class, rowMapperFactory.ofLink(SourceSourceList.class));
    }
}
