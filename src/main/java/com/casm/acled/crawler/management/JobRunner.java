package com.casm.acled.crawler.management;

import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.enioka.jqm.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.time.*;
import java.util.*;

public interface JobRunner {

    // return something implements Job
    List<Job> getJobs();

    // return something implements Job
    Job getJob(int jobPID);

    void runJob(Job j);

}
