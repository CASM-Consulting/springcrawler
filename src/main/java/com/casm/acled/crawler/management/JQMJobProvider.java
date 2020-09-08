package com.casm.acled.crawler.management;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.Crawl;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.enioka.jqm.api.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.time.*;
import java.util.*;

@Component
public class JQMJobProvider implements JobProvider <JQMJob>{

    @Autowired
    private SourceDAO sourceDAO;
    @Autowired
    private SourceListDAO sourceListDAO;

    public static final String appName = "JQMSpringCollectorV1";
    public static final String JQM_USER = "crawler-submission-service";

    JQMJobProvider() {
    }

    public void setSourceDAO(SourceDAO sourceDAO) {
        this.sourceDAO = sourceDAO;
    }

    public void setSourceListDAO(SourceListDAO sourceListDAO) {
        this.sourceListDAO = sourceListDAO;
    }

    public JQMJob sourceToJob (Source source, Map<String,String> params) {
        // from and to are not obtained by using source however; need to add other parameters;
        JobRequest jobRequest = JobRequest.create(appName, JQM_USER);
        jobRequest.addParameter( Crawl.SOURCE_ID, Integer.toString( source.id()));
        jobRequest.addParameter( "CRAWL_SCHEDULE",  source.CRAWL_SCHEDULE);

        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                jobRequest.addParameter(entry.getKey(), entry.getValue());
            }
        }

        return new JQMJob(source, jobRequest);

    }

    public List<JQMJob> getJobs(Map<String, String> params) {
        // a little bit confused about the difference between JobProvider's getJobs and JobRunner's getJobs;
        // get all jobs by providing corresponding resources;
        // in order to set more parameters, this method should have more parameters like from, to and etc. passing a map paramter, add all elements of that map into our job request;
        List<SourceList> lists = sourceListDAO.getAll();

        //if want all sources
//        Set<Source> allSources = new HashSet<>();
        Set<Source> globalActiveSources = new HashSet<>();
        List<JQMJob> allJobs = new ArrayList();

        for(SourceList list : lists) {
            List<Source> listSources = sourceDAO.byList(list);
            // if want all sources
//            allSources.addAll(listSources);

            // when the value of the crawl_active would be changed? I can't find the assigment thing for this.
            // crawl_active is always not assigned anywhere, and all sourcelists have no crawl_active parameter. basically none source would be added when using fake-net testing;
            if(list.isTrue(SourceList.CRAWL_ACTIVE)) {
                globalActiveSources.addAll(listSources);
            }
        }

        for (Source source: globalActiveSources) {
            JQMJob job = sourceToJob(source, params);
            allJobs.add(job);
        }

        return allJobs;


    }
}
