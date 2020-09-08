package com.casm.acled.crawler.management;

import java.lang.reflect.Method;
import java.time.*;
import java.util.*;

public interface JobProvider <T extends Job> {

    public abstract List<T> getJobs(Map<String, String> params);

}
