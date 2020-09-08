package com.casm.acled.crawler.management;

import com.casm.acled.crawler.Crawl;
import com.enioka.jqm.api.JobInstance;
import com.enioka.jqm.api.JobRequest;

import com.casm.acled.entities.source.Source;

import java.util.*;


public class JQMJob implements Job{

    private Source source;
    private JobInstance jobInstance;
    private JobRequest jobRequest;

    // construct with jobinstance, which is required by JQMJobRunner
    public JQMJob (JobInstance j) {
        this.jobInstance = j;
        this.source = null;
        this.jobRequest = getJobRequestFromJobInstance(j);
    }

    // construct with jobrequest and source, which is used by JQMJobProvider
    public JQMJob (Source source, JobRequest j) {
        this.jobRequest = j;
        this.jobInstance = null;
        this.source = source;
    }

    public JobInstance getJobInstance () {
        return this.jobInstance;
    }

    public JobRequest getJobRequest () {
        return this.jobRequest;
    }

    public JobRequest getJobRequestFromJobInstance (JobInstance job) {
        JobRequest j = JobRequest.create(job.getApplicationName(), job.getUser());
        j.setParameters(job.getParameters());
        return j;
    }

    public int getId() {
        // return source ID; if want job id, then jobInstance Object has a getId() method, can call it like: j.getJobInstance().getId()
        return Integer.valueOf(this.jobRequest.getParameters().get(Crawl.SOURCE_ID));
    }

    public String getSourceListId() {
        return this.jobRequest.getParameters().get(Crawl.SOURCE_LIST_ID);
    }

    public String getSchedule() {
        // probably need to modify jobInstance request parameter by adding a schedule status
        return this.jobRequest.getParameters().get(Source.CRAWL_SCHEDULE);
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Source getSource() {
        return this.source;
    }

    public void setJobRequestParameters(Map<String, String> params) {
        this.jobRequest.setParameters(params);
    }


}
