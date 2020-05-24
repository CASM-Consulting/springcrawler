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
 * Data I/O and general utils
 */
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
            return stream.limit(limit)
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
}
