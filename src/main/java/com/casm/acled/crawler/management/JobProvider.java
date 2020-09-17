package com.casm.acled.crawler.management;

import java.lang.reflect.Method;
import java.time.*;
import java.util.*;

public interface JobProvider {

    List<Job> getJobs(Map<String, String> params);

    Job getJob(int id);

    void setPID(int id, int pid);

}
