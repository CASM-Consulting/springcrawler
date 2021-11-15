package com.casm.acled.dao.entities;

import com.casm.acled.dao.Tables;
import com.casm.acled.dao.VersionedEntityDAOImpl;
import com.casm.acled.dao.rowmappers.VersionedEntityRowMapperFactory;
import com.casm.acled.dao.util.SqlBinder;
import com.casm.acled.entities.crawlreport.CrawlReport;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;


@Transactional
@Repository
@Primary
public class CrawlReportDAOImpl extends VersionedEntityDAOImpl<CrawlReport> implements CrawlReportDAO {

    public CrawlReportDAOImpl(@Autowired JdbcTemplate jdbcTemplate,
                              @Autowired VersionedEntityRowMapperFactory rowMapperFactory,
                              @Value(Tables.T_CRAWL_REPORT) String table) {
        super(jdbcTemplate, table, CrawlReport.class, rowMapperFactory.of(CrawlReport.class));
    }

    @Override
    public List<CrawlReport> byRunId(String runId) {
        return getBy(CrawlReport.RUN_ID, runId);
    }

    /**
     * Get all CrawlReports with a specific run ID, source ID and types.
     */
    @Override
    public List<CrawlReport> byRunIdAndCrawlSource(String runId, Integer id, String type, String reporterType) {
        String sql = SqlBinder.sql("SELECT * FROM ${table}",
                "WHERE data->>'${run_id_field}' = ?",
                        "AND (data->>'${id_field}')::int = ?",
                        "AND data->>'${type_field}' = ?",
                        "AND data->>'${reporter_type_field}' = ?")
                .bind("table", table)
                .bind("run_id_field", CrawlReport.RUN_ID)
                .bind("id_field", CrawlReport.ID)
                .bind("type_field", CrawlReport.TYPE)
                .bind("reporter_type_field", CrawlReport.REPORTER_TYPE)
                .bind();
        return query(sql, runId, id, type, reporterType);
    }

    /**
     * Get all CrawlReports that match a list of run IDs, and a specific source ID and types.
     */
    @Override
    public List<CrawlReport> byRunIdAndCrawlSource(List<String> runIds, Integer id, String type, String reporterType) {
        String inSql = String.join(",", Collections.nCopies(runIds.size(), "?"));

        String sql = SqlBinder.sql("SELECT * FROM ${table}",
                String.format("WHERE data->>'${run_id_field}' IN (%s)", inSql),
                "AND (data->>'${id_field}')::int = ?",
                "AND data->>'${type_field}' = ?",
                "AND data->>'${reporter_type_field}' = ?")
                .bind("table", table)
                .bind("run_id_field", CrawlReport.RUN_ID)
                .bind("id_field", CrawlReport.ID)
                .bind("type_field", CrawlReport.TYPE)
                .bind("reporter_type_field", CrawlReport.REPORTER_TYPE)
                .bind();


        Object[] params = {id, type, reporterType};

        params = ArrayUtils.addAll(runIds.toArray(), params);

        return query(sql, params);
    }

    /**
     * Return the latest run IDs (most recent first) for a given id, type and reporter type.
     */
    @Override
    public List<String> latestRunIds(Integer id, String type, String reporterType, Integer numRuns){
        String sql = SqlBinder.sql(
            "SELECT run_id FROM (",
                    "SELECT DISTINCT ON (run_id) data->>'${run_id}' AS run_id, data->>'${timestamp}' AS time",
                    "FROM ${table}",
                        "WHERE (data->>'${id}')::int = ?",
                        "AND data->>'${type}' = ?",
                        "AND data->>'${reporter_type}' = ?",
                        "ORDER BY run_id, time DESC",
                    ") AS sub",
            "ORDER BY time DESC",
            "LIMIT ?")
                .bind("table", table)
                .bind("id", CrawlReport.ID)
                .bind("type", CrawlReport.TYPE)
                .bind("reporter_type", CrawlReport.REPORTER_TYPE)
                .bind("run_id", CrawlReport.RUN_ID)
                .bind("timestamp", CrawlReport.TIMESTAMP)
                .bind();

        return jdbcTemplate.query(sql, (rs, i) -> rs.getString("run_id"), id, type, reporterType, numRuns);
    }
}
