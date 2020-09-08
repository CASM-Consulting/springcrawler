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

public interface JobRunner <T extends Job>{

    // return something implements Job
    public abstract List<T> getJobs();

    // return something implements Job
    public abstract T getJob(int jobId);

    public void runJob(T j);

}
