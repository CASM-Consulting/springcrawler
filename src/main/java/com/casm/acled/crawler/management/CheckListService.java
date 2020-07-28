package com.casm.acled.crawler.management;

import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opencsv.CSVReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CheckListService {

    protected static final Logger logger = LoggerFactory.getLogger(CheckListService.class);

    @Autowired
    private SourceDAO sourceDAO;
    public void exportCrawlerSourcesToCSV(Path outputDir, String fileName, SourceList sourceList) throws IOException {
        List<Source> sources = sourceDAO.byList(sourceList);
        exportCrawlerSourcesToCSV(outputDir, fileName, sources);
    }
    public void exportCrawlerSourcesToCSV(Path outputDir, String fileName, List<Source> sources) throws IOException {

        List<String> headers = ImmutableList.of("id", "field", "value");
        Set<String> fields = ImmutableSet.of(Source.EXAMPLE_URLS, Source.DATE_FORMAT, Source.LOCALES);

        try (
                final OutputStream outputStream = java.nio.file.Files.newOutputStream(outputDir.resolve(fileName), StandardOpenOption.CREATE_NEW);
                final PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)), false);
                final CSVPrinter csv = new CSVPrinter(writer, CSVFormat.EXCEL.withQuoteMode(QuoteMode.NON_NUMERIC))
        ) {

            csv.printRecord(headers);

            for (Source source : sources) {

                String id = source.get(Source.STANDARD_NAME);

                for(String field : fields) {


                    if(!source.hasValue(field)) {
                        continue;
                    }

                    List<String> values = source.get(field);

                    for(String value : values){

                        List<String> row = new ArrayList<>();

                        row.add(id);
                        row.add(field);
                        row.add(value);
                        csv.printRecord( row );
                    }
                }
            }
        }
    }

    public void importCrawlerSourcesFromCSV(Path seedsPath, Source defaultSource) throws IOException {
        String ID = "id";
        String FIELD = "field";
        String VALUE = "value";

        Set<String> allowedFields = ImmutableSet.of(Source.EXAMPLE_URLS, Source.DATE_FORMAT, Source.LOCALES);

        try (
                Reader reader = java.nio.file.Files.newBufferedReader(seedsPath);
                CSVReader csvReader = new CSVReader(reader);
        ) {
            Iterator<String[]> itr = csvReader.iterator();
            String[] headers = itr.next();

            Map<String,Integer> headerMap = new HashMap<>();

            for(int i = 0; i < headers.length; ++i) {
                headerMap.put(headers[i], i);
            }

            Map<String, Source> sourceMap = new HashMap<>();

            while(itr.hasNext()) {
                String[] row = itr.next();

                String id = row[headerMap.get(ID)];
                String field = row[headerMap.get(FIELD)];
                String value = row[headerMap.get(VALUE)];

                if(!allowedFields.contains(field)) {
                    logger.warn("{} not allowed", field);
                }

                sourceMap.compute(id, (i, source)->{
                    if(source == null) {
                        source = defaultSource.put(Source.STANDARD_NAME, id);
                    }

                    List<String> values;

                    if(source.hasValue(field)) {
                        values = source.get(field);
                    } else {
                        values = new ArrayList<>();
                    }
                    if(!value.isEmpty()) {
                        values.add(value);
                    }

                    return source.put(field, values);
                });
            }

            List<Source> sources = sourceMap.values().stream().map(s-> {
//                doesn't work.
//                for(String field : allowedFields) {
//                    if(s.hasValue(field) && ((List)s.get(field)).isEmpty()) {
//                        s.put(field, null);
//                    }
//                }

                Optional<Source> maybeSource = sourceDAO.byName(s.get(Source.STANDARD_NAME));
                if(maybeSource.isPresent()) {
                    Source source = maybeSource.get();
                    s = s.id(source.id());
                }
                return s;
            }).collect(Collectors.toList());

            sourceDAO.create(sources);
        }
    }
}
