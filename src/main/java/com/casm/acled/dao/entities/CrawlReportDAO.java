package com.casm.acled.dao.entities;

import com.casm.acled.dao.VersionedEntityDAO;
import com.casm.acled.entities.crawlreport.CrawlReport;

import java.util.List;

public interface CrawlReportDAO extends VersionedEntityDAO<CrawlReport> {

    List<CrawlReport> byRunId(String runId);

    /**
     * Get all CrawlReports with a specific run ID, source ID and types.
     */
    default List<CrawlReport> byRunIdAndCrawlSource(String runId, Integer id, Class<?> type, Class<?> reporterType){
        return byRunIdAndCrawlSource(runId, id, type.getName(), reporterType.getName());
    }
    List<CrawlReport> byRunIdAndCrawlSource(String runId, Integer id, String type, String reporterType);

    /**
     * Get all CrawlReports that match a list of run IDs, and a specific source ID and types.
     */
    default List<CrawlReport> byRunIdAndCrawlSource(List<String> runId, Integer id, Class<?> type, Class<?> reporterType){
        return byRunIdAndCrawlSource(runId, id, type.getName(), reporterType.getName());
    }
    List<CrawlReport> byRunIdAndCrawlSource(List<String> runId, Integer id, String type, String reporterType);

    /**
     * Return the latest run IDs (most recent first) for a given id, type and reporter type.
     */
    default List<String> latestRunIds(Integer id, Class<?> type, Class<?> reporterType, Integer numRuns) {
        return latestRunIds(id, type.getName(), reporterType.getName(), numRuns);
    }
    List<String> latestRunIds(Integer id, String type, String reporterType, Integer numRuns);
}
