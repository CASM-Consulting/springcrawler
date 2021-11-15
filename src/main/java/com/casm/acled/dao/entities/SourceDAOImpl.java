package com.casm.acled.dao.entities;

import com.casm.acled.dao.Tables;
import com.casm.acled.dao.VersionedEntityDAOImpl;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.dao.util.SqlBinder;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
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
public class SourceDAOImpl extends VersionedEntityDAOImpl<Source> implements SourceDAO  {

    private final SourceSourceListDAO sourceSourceListDAO;

    public SourceDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                         @Autowired VersionedEntityRowMapperFactory rowMapperFactory,
                         @Autowired SourceSourceListDAO sourceSourceListDAO,
                         @Value(Tables.T_SOURCE) String table) {
        super(jdbcTemplate, table, Source.class, rowMapperFactory.of(Source.class));
        this.sourceSourceListDAO = sourceSourceListDAO;
    }

//    @Override
//    public List<Source> byDesk(Integer deskId) {
//        String sql = new SqlBinder("SELECT S.id, S.data FROM ${table} AS S")
//                .append("JOIN ${source_source_list_table} AS SSLT ON SSLT.source_id = ST.id")
//                .append("JOIN ${source_list_table} AS SLT ON SSLT.source_list_id = SLT.id")
//                .append("WHERE CAST(SLT.data->>'${desk_id}' AS int) = ?")
//                .bind("table", table)
//                .bind("source_source_list_table", Tables.T_SOURCE_SOURCE_LIST)
//                .bind("source_list_table", Tables.T_SOURCE_LIST)
//                .bind("desk_id", SourceList.DESK_ID)
//                .bind();
//        List<Source> sources = query(sql, deskId);
//        return sources;
//    }
        @Override
        public List<Source> byDesk(Integer deskId) {

            String sql = SqlBinder.sql("SELECT S.id AS S_ID,",
                    "S.data AS S_DATA",
                    "FROM ${table} AS S",
                    "LEFT JOIN ${join_table} AS SD ON (SD.id1 = S.id)",
                    "WHERE SD.id2 = ?")
                    .bind("table", table)
                    .bind("join_table", Tables.T_SOURCE_DESK)
                    .bind();

            List<Source> sources = query(sql, deskId);
            return sources;
        }



//    @Override
//    public List<Source> byRegion(String regionName) {
//        String sql = new SqlBinder("SELECT * FROM ${table} AS ST")
//                .append("JOIN ${source_source_list_table} AS SSLT ON SSLT.source_id = ST.id")
//                .append("JOIN ${source_list_table} AS SLT ON SSLT.source_list_id = SLT.id")
//                .append("JOIN ${region_table} AS RT ON RT.id = (SLT.data->>'${region_id}')::int")
//                .append("WHERE RT.data->>'${region_name}' = ?")
//                .bind("table", table)
//                .bind("region_name", Desk.DESK_NAME)
//                .bind("source_source_list_table", Tables.T_SOURCE_SOURCE_LIST)
//                .bind("source_list_table", Tables.T_SOURCE_LIST)
//                .bind("region_table", Tables.T_DESK)
//                .bind("region_id", SourceList.DESK_ID)
//                .bind();
//        List<Source> sources = query(sql, regionName);
//        return sources;
//    }

    @Override
    public Optional<Source> byName(String sourceName) {
        return getByUnique(Source.STANDARD_NAME, sourceName);
    }

    @Override
    public List<Source> byList(SourceList list) {
        String sql = new SqlBinder("SELECT * FROM ${table} AS ST")
                .append("JOIN ${source_source_list_table} AS SSLT ON SSLT.id1 = ST.id")
                .append("WHERE SSLT.id2 = ?")
                .bind("table", table)
                .bind("source_source_list_table", Tables.T_SOURCE_SOURCE_LIST)
                .bind();

        List<Source> sources = query(sql, list.id());

        return sources;
    }

    @Override
    public List<Source> create(List<Source> sources) {
        sources = super.create(sources);
        for(Source source : sources) {
            for(SourceList sourceList : source.sourceLists()) {
                sourceSourceListDAO.link(source, sourceList);
            }
        }
        return sources;
    }
}
