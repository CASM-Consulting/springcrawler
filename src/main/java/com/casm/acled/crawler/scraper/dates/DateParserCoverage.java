package com.casm.acled.crawler.scraper.dates;

import java.util.List;

/**
 * Wraps a DateParser to track coverage and specific (un)successful parses.
 */
class DateParserCoverage {

    private DateParser parser;

    public long getSuccessCount() {
        return successCount;
    }

    public long getFailureCount() {
        return failureCount;
    }

    public double getCoverage() {
        return coverage;
    }

    private long successCount;
    private long failureCount;
    private double coverage;
    private List<Boolean> successMask;

    public DateParserCoverage(DateParser parser) {
        this.parser = parser;
    }

    public List<Boolean> calculate(List<String> dates) {
        successMask = CoverageUtils.getCoverageMask(this.parser, dates);

        // calc stats
        successCount = successMask.stream().filter(b -> b).count();
        failureCount = successMask.size() - successCount;
        coverage = successCount / (double) successMask.size();

        return successMask;
    }

    public List<Boolean> getSuccessMask() {
        return this.successMask;
    }

    public DateParser getParser() {
        return this.parser;
    }

}
