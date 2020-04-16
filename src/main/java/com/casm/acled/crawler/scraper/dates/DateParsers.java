package com.casm.acled.crawler.scraper.dates;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DateParsers {

    protected static final Logger logger = LoggerFactory.getLogger(DateParsers.class);

    /**
     * Notes: delim must not be in regex family (added: Pattern.quote(delim))
     */


    public static final DateParser dp1 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy/en"
    ));

    public static final DateParser dp2 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy, HH:mm a/en"
    ));

    public static final DateParser dp3 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd MMM yyyy/en"
    ));

    public static final DateParser dp4 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd.MM.yyyy/en"
    ));

    public static final DateParser dp5 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/ddF-MMM-yyyy/en"
    ));

    public static final DateParser dp6 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/ddF-MMM-yyyy/en/ORD"
    ));

    public static final DateParser dp7 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd MMM yyyy HH:mm a z/en_GB/BST/RE.*Updated: (.*)" //Published: 26 Nov 2019 01:43 AM BdST Updated: 26 Nov 2019 01:44 AM BdST
    ));

    public static final DateParser dp8 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE d MMM YYYY hh:mm/en"  //Sunday 27 October 2019 23:56
    ));

    public static final DateParser dp9 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, d MMM YYYY/en/ORD"  //Monday, 3 February 2020
    ));

    public static final DateParser dp10 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE MMM d YYYY/en/ORD"  //Monday December 2 2019
    ));

    public static final DateParser dp11 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d YYYY/es" // Noviembre 29 2019
    ));

    public static final DateParser dp12 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY, hh:mm a/en/RE.*Update Date - (.*)"  //Update Date - November 03, 2019, 09:08 PM
    ));

    public static final DateParser dp13 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|dd/MM/yyyy hh:mm Z|en"
    ));

    public static final DateParser dp14 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|dd/MM/yyyy hh:mm|en"
    ));

    // too ambiguous?
    public static final DateParser dp15 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|dd/MM/yyyy|en"
    ));

    public static final DateParser dp16 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy/es" //22 septembre 2019
    ));

    public static final DateParser dp17 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE MMM d, yyyy/en/ORD" // Monday Sep 30, 2019
    ));

    public static final DateParser dp18 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy, HH:mm/ru" // 15 Ноября 2019, 16:15
    ));

    public static final DateParser dp19 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy h:mm:ss a/en/RE.*: (.*)" // Published: September 17, 2019 8:32:39 PM
    ));

    public static final DateParser dp20 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM YYYY/fr"
    ));

    public static final DateParser dp21 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM YYYY 'г.', HH:MM/ru" // 17 ноября 2019 г., 23:01
    ));

    public static final DateParser dp22 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY/fr" // octobre 6, 2019
    ));

    public static final DateParser dp23 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, d MMM YYYY HH:MM/en/ORD" // Tuesday, 04 February 2020 14:15'
    ));

//    public static final DateParser dp24 = CompositeDateParser.of(ImmutableList.of(
//            "ISO:|HH:MM / " // Tuesday, 04 February 2020 14:15'
//    ));

    public static final DateParser dp25 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY/en/STRIP^[Ll]ast updated:?" // Last updated Sep 21, 2019'
    ));

    public static final DateParser dp26 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/hh:mm a MMM d, YYYY/en/ORD/STRIP^[Pp]ublished at:?" // 'Published at 06:11 pm November 4th, 2019'
    ));


    private static List<Boolean> getCoverageMask(DateParser parser, List<String> examples) {

        List<Boolean> coverageMask = new ArrayList<>(examples.size());

        for (String example : examples) {
            Optional<LocalDateTime> parseAttempt = parser.parse(example);
            coverageMask.add(parseAttempt.isPresent());
        }

        return coverageMask;
    }

    private static float coverageFromMask(List<Boolean> coverageMask) {
        // stats
        long successes = coverageMask.stream().filter(b -> b).count();
        long failures = coverageMask.size() - successes;
        float coverage = successes/(float)coverageMask.size();

        logger.info("Success: " + successes
                + ", Failures: " + failures
                + ", Coverage: " + coverage);

        return coverage;
    }

    private static List<Boolean> orLists(List<List<Boolean>> lists) {
        List<Boolean> orList = new ArrayList<>(lists.get(0));
        int overlapCount = 0;

        for (List<Boolean> currList : lists.subList(1, lists.size())) {
            for (int i = 0; i < currList.size(); i++) {

                // Brute check for overlap
                if (orList.get(i) && currList.get(i)) {
                    overlapCount++;
                }

                orList.set(i, orList.get(i) || currList.get(i));
            }
        }

        logger.warn(overlapCount + " overlaps!");

        return orList;
    }

    private static List<List<DateParser>> getParseResults(List<DateParser> parsers, List<String> examples) {

        List<List<DateParser>> successfulParsers = new ArrayList<>(examples.size());

        for (String example : examples) {

            List<DateParser> dps = new ArrayList<>();

            for (DateParser parser : parsers) {
                if (parser.parse(example).isPresent()) {
                    dps.add(parser);
                }
            }

            successfulParsers.add(dps);
        }

        return successfulParsers;
    }


    public static void main(String... args) throws IOException {

        List<DateParser> parsers = ImmutableList.of(
                dp1, dp2, dp3, dp4, dp5, dp6, dp7,
                dp8, dp9, dp10, dp11, dp12,
                dp13, dp14, dp15,
                dp16, dp17, dp18, dp19, dp20, dp21, dp22,
                dp23,
                dp25, dp26
        );

        // Load line-separated examples (ignore "null" and empty lines)
        List<String> examples;
        try (Stream<String> stream = Files.lines(Paths.get(args[0]))) {
            examples = stream.filter(p -> !p.trim().isEmpty() && !p.equals("null")).collect(Collectors.toList());
        }

        // Apply parsers (get back used parsers for each example)
        List<List<DateParser>> parseResults = getParseResults(parsers, examples);

        // Log results
        for (int i = 0; i < examples.size(); i++) {
            if (parseResults.get(i).size() > 1) {
                logger.warn("parser overlap for '" + examples.get(i) + "' (" + parseResults.get(i).toString() + ")");
            } else if (parseResults.get(i).isEmpty()) {
                logger.info("no parse for '" + examples.get(i) + "'");
            } else {
                //
            }
        }

        // XXX: refactor - per-parser coverage + overall coverage

        List<List<Boolean>> coverageMasks = new ArrayList<>(parsers.size());

        // Test each parser
        for (DateParser parser : parsers) {
            List<Boolean> coverageMask = getCoverageMask(parser, examples);
            coverageMasks.add(coverageMask);

            System.out.println();
            logger.info("Parser: " + parser.getFormatSpec());
            coverageFromMask(coverageMask);

            for (int i = 0; i < examples.size(); i++) {
                if (coverageMask.get(i)) {
                    logger.info(examples.get(i));
                }
            }
        }

        System.out.println();
        List<Boolean> totalCoverageMask = orLists(coverageMasks);
        coverageFromMask(totalCoverageMask);
    }
}
