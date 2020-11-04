package com.casm.acled.crawler.reporting;

import com.casm.acled.crawler.scraper.ACLEDCommitter;
import com.casm.acled.dao.entities.CrawlReportDAO;
import com.casm.acled.entities.crawlreport.CrawlReport;
import com.casm.acled.entities.source.Source;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Andrew D. Robertson on 21/10/2020.
 */
@Service
public class ReportQueryService {

    @Autowired
    CrawlReportDAO crawlReportDAO;

    public List<String> latestRunIds(int numRuns, int id, Class<?> type, Class<?> reporterType){
        return crawlReportDAO.latestRunIds(id, type, reporterType, numRuns);
    }

    /**
     * Get an EventCountSummary for each of the latest N run IDs. The iteration order of the map
     * will match the timestamp order of the runs.
     */
    public Map<String, EventCountSummary> summaryPerRun(int sourceId, int numRuns){
        // Get an ordered list of latest run IDS for source
        List<String> latestRuns = crawlReportDAO.latestRunIds(sourceId, Source.class, ACLEDCommitter.class, numRuns);
        // If no runs were found, return empty map
        if (latestRuns.isEmpty()){
            return ImmutableMap.of();
        }
        // Get a list of all crawl reports for these runs
        List<CrawlReport> reports = crawlReportDAO.byRunIdAndCrawlSource(latestRuns, sourceId, Source.class, ACLEDCommitter.class);
        // Summarise the event counts by run ID
        return summaryPerRun(reports, latestRuns);
    }

    public EventCountSummary summaryForRun(String runId, int id, Class<?> type, Class<?> reporterType){
        List<CrawlReport> reports = crawlReportDAO.byRunIdAndCrawlSource(runId, id, type, reporterType);

        EventCountSummary summary = new EventCountSummary();

        for (CrawlReport report : reports){
            Event event = Event.valueOf(report.get(CrawlReport.EVENT));
            summary.incCount(event);
        }

        return summary;
    }

    /**
     * Generate a mapping of run IDs to summary of event counts from a list of crawl reports.
     * The map's ordering of IDs will respect the order of IDs in runIdOrder.
     */
    public Map<String, EventCountSummary> summaryPerRun(List<CrawlReport> reports, List<String> runIdOrder){
        Map<String, EventCountSummary> summaryPerRun = new LinkedHashMap<>();

        for (String runId : runIdOrder){
            summaryPerRun.put(runId, new EventCountSummary());
        }

        for (CrawlReport report : reports){
            String runId = report.get(CrawlReport.RUN_ID);
            Event event = Event.valueOf(report.get(CrawlReport.EVENT));

            if (!summaryPerRun.containsKey(runId)){
                System.out.println("The ordering of "+ runId + " was not specified, so the output ordering may not be in order of timestamp.");
                summaryPerRun.put(runId, new EventCountSummary());
            }

            summaryPerRun.get(runId).incCount(event);
        }
        return summaryPerRun;
    }


    /**
     * Make it easier to get counts of Events.
     */
    public static class EventCountSummary extends HashMap<Event, Integer> {

        public EventCountSummary(){
        }

        public EventCountSummary(Iterable<Event> events){
            for(Event event : events){
                incCount(event);
            }
        }

        public int getCount(Event event){
            return getOrDefault(event, 0);
        }

        public int incCount(Event event){
            return incCount(event, 1);
        }

        public int incCount(Event event, int increment){
            return merge(event, increment, Integer::sum);
        }
    }
}
