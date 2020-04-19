package com.casm.acled.crawler.scraper.dates;

import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by ci53 on 19/04/2020.
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

class CoverageUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageUtils.class);

    static List<Boolean> getCoverageMask(DateParser parser, List<String> dates) {

        List<Boolean> coverageMask = new ArrayList<>(dates.size());

        for (String date : dates) {
            Optional<LocalDateTime> parseAttempt = parser.parse(date);
            coverageMask.add(parseAttempt.isPresent());
        }

        return coverageMask;
    }

    static List<String> loadExamplesFromLineSep(String path) throws IOException {
        return loadExamplesFromLineSep(path, Long.MAX_VALUE);
    }

    static List<String> loadExamplesFromLineSep(String path, long limit) throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(path))) {
            return stream
                    .map(String::trim)
                    .filter(p -> !p.isEmpty() && !p.equals("null"))
                    .collect(Collectors.toList());
        }
    }

    static List<String> loadExamplesFromCsv(String path) throws IOException {
        return loadExamplesFromCsv(path, Long.MAX_VALUE);
    }

    static List<String> loadExamplesFromCsv(String path, long limit) throws IOException {

        // Init CSV reader
        try (CSVReader csvReader = new CSVReader(Files.newBufferedReader(Paths.get(path)))) {

            // Build header-index map
            String[] header = csvReader.readNext();
            Map<String, Integer> headers = IntStream.range(0, header.length).boxed()
                    .collect(Collectors.toMap(i -> header[i], i -> i));

            // Extract dates, filter duds ("null" and "").
            return csvReader.readAll().stream().limit(limit)
                    .map(rec -> rec[headers.get("date")].trim())
                    .filter(d -> !d.isEmpty() && !d.equals("null"))
                    .collect(Collectors.toList());
        }
    }

//    /**
//     * Element-wise OR between `List<Boolean>`
//     *
//     * @param lists
//     * @return
//     */
//    static List<Boolean> orLists(List<List<Boolean>> lists) {
//        List<Boolean> orList = new ArrayList<>(lists.get(0));
//        int overlapCount = 0;
//
//        for (List<Boolean> currList : lists.subList(1, lists.size())) {
//            for (int i = 0; i < currList.size(); i++) {
//
//                // Brute check for overlap
//                if (orList.get(i) && currList.get(i)) {
//                    overlapCount++;
//                }
//
//                orList.set(i, orList.get(i) || currList.get(i));
//            }
//        }
//
//        LOG.warn(overlapCount + " overlaps!");
//
//        return orList;
//    }

//    static List<List<DateParser>> findSuccessfulParsers(List<DateParser> parsers, List<String> dates) {
//
//        List<List<DateParser>> successfulParsers = new ArrayList<>(dates.size());
//
//        for (String date : dates) {
//            List<DateParser> dps = new ArrayList<>();
//
//            // Apply all parsers to given date, add successful
//            for (DateParser parser : parsers) {
//                if (parser.parse(date).isPresent()) {
//                    dps.add(parser);
//                }
//            }
//            successfulParsers.add(dps);
//        }
//        return successfulParsers;
//    }
}

public class CoverageCalculator {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageCalculator.class);

    // All (stats-wrapped) DateParsers
    private List<DateParserCoverage> coverageCalcs;

    // List of successful (stats-wrapped) DateParsers for each instance
    private List<List<DateParserCoverage>> successfulCoverage;

    // General stats
    private long successCount;
    private long failureCount;
    private double coverage;
    private long overlapsCount;
    private List<Boolean> successMask;

    // Dates to calculate coverage over
    private List<String> dates;

    public CoverageCalculator(List<DateParserCoverage> coverageCalcs, List<String> dates) {
        this.coverageCalcs = coverageCalcs;
        this.dates = dates;
    }

    private List<DateParserCoverage> getMatches(int maskIdx, List<DateParserCoverage> calcs) {
        List<DateParserCoverage> matches = new ArrayList<>();

        for (DateParserCoverage cc : calcs) {
            if (cc.getSuccessMask().get(maskIdx)) {
                matches.add(cc);
            }
        }
        return matches;
    }

    public List<Boolean> calculate(){

        // For each parser, calc stats for given dates
        for (DateParserCoverage pc : coverageCalcs) {
            pc.calculate(dates);
        }

        successfulCoverage = new ArrayList<>(dates.size());
        successMask = new ArrayList<>(dates.size());
        overlapsCount = 0;

        for (int i = 0; i < dates.size(); i++) {
            List<DateParserCoverage> parserMatches = getMatches(i, coverageCalcs);

            successfulCoverage.add(parserMatches);
            successMask.add(!parserMatches.isEmpty());

            if (parserMatches.size() > 1) {
                overlapsCount++;
            }
        }

        // calc stats
        successCount = successMask.stream().filter(b -> b).count();
        failureCount = successMask.size() - successCount;
        coverage = successCount / (double) successMask.size();

        return successMask;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public long getFailureCount() {
        return failureCount;
    }

    public double getCoverage() {
        return coverage;
    }

    public long getOverlapsCount() {
        return overlapsCount;
    }

    /**
     * Log individual and overall coverage stats.
     */
    public void logStats() {

        for (int i = 0; i < dates.size(); i++) {

            List<DateParserCoverage> parserMatches = successfulCoverage.get(i);

            if (parserMatches.isEmpty()) {
                LOG.warn("no parse for '" + dates.get(i) + "'");

            } else if (parserMatches.size() > 1) {
                String overlappingSpecs = parserMatches.stream()
                        .flatMap(pc -> pc.getParser().getFormatSpec().stream())
                        .map(p -> "\"" + p + "\"")
                        .collect(Collectors.joining(" OVERLAPS "));

                LOG.warn("parser overlap for '" + dates.get(i) + "' (" + overlappingSpecs + ")");
            }
        }

        for (DateParserCoverage dpc : coverageCalcs) {
            LOG.info("Parser: " + dpc.getParser().getFormatSpec());
            LOG.info("Success: {}, Failure: {}, Coverage: {}",
                    dpc.getSuccessCount(), dpc.getFailureCount(), dpc.getCoverage()
            );
            LOG.info("------------------------------------------");

            for (int i = 0; i < dates.size(); i++) {
                if (dpc.getSuccessMask().get(i)) {
                    LOG.debug(dates.get(i));
                }
            }
        }

        LOG.info("Summary");
        LOG.info("Success: {}, Failure: {}, Coverage: {}, Overlaps: {}",
                getSuccessCount(), getFailureCount(), getCoverage(), getOverlapsCount()
        );
    }

    public static void main(String... args) throws IOException {

//        List<String> examples = ImmutableList.of("Monday, 27 January 2020 4:44 PM  [ Last Update: Monday, 27 January 2020 6:24 PM ]");
//        List<String> examples = ImmutableList.of("JUBA, May 7 (Agencies) | Publish Date: 5/7/2019 12:04:09 PM IST");

        // Load examples
        List<String> examples = CoverageUtils.loadExamplesFromCsv(args[0]);
        //List<String> examples = CoverageUtils.loadExamplesFromLineSep(args[0]);

        List<DateParserCoverage> parsers = DateParsers.PARSERS
                .stream()
                .map(DateParserCoverage::new)
                .collect(Collectors.toList());

        CoverageCalculator cc = new CoverageCalculator(parsers, examples);
        cc.calculate();
        cc.logStats();

//        // Apply parsers (get back used parsers for each example)
//        List<List<DateParser>> parseResults = CoverageUtils.findSuccessfulParsers(DateParsers.PARSERS, examples);
//
//        // Log results
//        for (int i = 0; i < examples.size(); i++) {
//            if (parseResults.get(i).size() > 1) {
//                List<DateParser> res = parseResults.get(i);
//
//                String overlappingSpecs = res.stream()
//                        .flatMap(dp -> dp.getFormatSpec().stream())
//                        .map(p -> "\"" + p + "\"")
//                        .collect(Collectors.joining(" OVERLAPS "));
//
//                LOG.warn("parser overlap for '" + examples.get(i) + "' (" + overlappingSpecs + ")");
//            } else if (parseResults.get(i).isEmpty()) {
//                LOG.info("no parse for '" + examples.get(i) + "'");
//            } else {
//                //
//            }
//        }
//
//        // XXX: refactor - per-parser coverage + overall coverage
//
//        List<List<Boolean>> coverageMasks = new ArrayList<>(DateParsers.PARSERS.size());
//
//        // Test each parser
//        for (DateParser parser : DateParsers.PARSERS) {
//            List<Boolean> coverageMask = CoverageCalculator.getCoverageMask(parser, examples);
//            coverageMasks.add(coverageMask);
//
//            System.out.println();
//            logger.info("Parser: " + parser.getFormatSpec());
//            coverageFromMask(coverageMask);
//
//            for (int i = 0; i < examples.size(); i++) {
//                if (coverageMask.get(i)) {
////                    logger.info(examples.get(i));
//                }
//            }
//        }
//
//        System.out.println();
//        List<Boolean> totalCoverageMask = orLists(coverageMasks);
//        coverageFromMask(totalCoverageMask);
//    }
    }
}