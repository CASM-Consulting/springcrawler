package com.casm.acled.crawler.management;

import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.dao.entities.SourceSourceListDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.csv.*;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;



@Service
public class ImportExportService {

    protected static final Logger logger = LoggerFactory.getLogger(ImportExportService.class);

    @Autowired
    private Reporter reporter;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceSourceListDAO sourceSourceListDAO;

    @Autowired
    private ArticleDAO articleDAO;

    public void exportCrawlerSourceList(CrawlArgs args) throws IOException {

        if (args.workingDir == null || args.path == null){
            throw new RuntimeException("Must specify a working directory (-wd) and path (-P) to export a SourceList.");
        }

        SourceList sourceList = args.sourceLists.get(0);

        Path path = args.workingDir.resolve(args.path);

        exportCrawlerSourcesToCSV(path, sourceList);
    }

    public void importCrawlerSourceList(CrawlArgs args) throws IOException {

        if (args.workingDir == null || args.path == null){
            throw new RuntimeException("Must specify a working directory (-wd) and path (-P) to find the source list import file.");
        }

        Path path = args.workingDir.resolve(args.path);

        List<Source> sources = importCrawlerSourcesFromCSV(path, EntityVersions.get(Source.class).current());

        if(args.flagSet.contains("L")) {

            for(SourceList list : args.sourceLists) {
                for(Source source : sources) {
                    sourceSourceListDAO.link(source, list);
                }
            }
        }
    }

    public void exportCrawlerSourcesToCSV(Path path, SourceList sourceList) throws IOException {
        List<Source> sources = sourceDAO.byList(sourceList);
        exportCrawlerSourcesToCSV(path, sources);
    }

    private static final Set<String> importExportFields = ImmutableSet.of(Source.LINK, Source.EXAMPLE_URLS, Source.DATE_FORMAT,
            Source.LOCALES, Source.CRAWL_DISABLE_SITEMAPS, Source.CRAWL_DISABLE_SITEMAP_DISCOVERY, Source.CRAWL_SITEMAP_LOCATIONS,
            Source.SEED_URLS, Source.CRAWL_SCHEDULE, Source.TIMEZONE, Source.CRAWL_DEPTH);

    public void exportCrawlerSourcesToCSV(Path path, List<Source> sources) throws IOException {

        List<String> headers = ImmutableList.of("id", "field", "value");
        Set<String> fields = importExportFields;

        try (
                final OutputStream outputStream = java.nio.file.Files.newOutputStream(path, StandardOpenOption.CREATE_NEW);
                final PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)), false);
                final CSVPrinter csv = new CSVPrinter(writer, CSVFormat.EXCEL)
//                CSVWriter csv = new CSVWriter(writer)
        ) {

            csv.printRecord(headers);
//            csv.writeNext(headers.toArray(new String[]{}));

            for (Source source : sources) {

                String id = source.get(Source.STANDARD_NAME);

                for(String field : fields) {

                    List<String> values;

                    if(source.hasValue(field) ) {
                        if(isList(source, field) ) {
                            if(((List)source.get(field)).isEmpty()) {

                                values = ImmutableList.of("");
                            } else {

                                values = source.get(field);
                            }
                        } else {

                            values = ImmutableList.of(source.get(field));
                        }
                    } else {

                        values = ImmutableList.of("");
                    }

                    for(Object value : values){

                        List<String> row = new ArrayList<>();

                        row.add(id);
                        row.add(field);
                        row.add(value.toString());
                        csv.printRecord( row );
//                        csv.writeNext(row.toArray(new String[]{}));
                    }
                }
            }
        }
    }

    public List<Source> importCrawlerSourcesFromCSV(Path seedsPath, Source defaultSource) throws IOException {
        String ID = "id";
        String FIELD = "field";
        String VALUE = "value";

        Set<String> allowedFields = importExportFields;

        try (
                Reader reader = java.nio.file.Files.newBufferedReader(seedsPath);
                CSVParser csvReader = new CSVParser(reader, CSVFormat.EXCEL.withFirstRecordAsHeader());
//                CSVReader csvReader = new CSVReader(reader);
        ) {
//            Iterator<String[]> itr = csvReader.iterator();
//            String[] headers = itr.next();
//            Map<String,Integer> headerMap = new HashMap<>();
//            for(int i = 0; i < headers.stream(); ++i) {
//                headerMap.put(headers[i], i);
//            }

//            Map<String,Integer> headerMap = csvReader.getHeaderMap();

            Iterator<CSVRecord> itr = csvReader.iterator();

            Map<String, Source> sourceMap = new HashMap<>();

            while(itr.hasNext()) {
//                String[] row = itr.next();
//                List<String> row = itr.next();
//                String id = row[headerMap.get(ID)];
//                String field = row[headerMap.get(FIELD)];
//                String value = row[headerMap.get(VALUE)];
                CSVRecord record = itr.next();

                String id = record.get(ID);
                String field = record.get(FIELD);
                String value = record.isSet(VALUE) ? record.get(VALUE) : null;

                if(value == null || value.isEmpty()) {
                    continue;
                }

                if(!allowedFields.contains(field)) {
                    logger.warn("{} not allowed", field);
                }

                if (!value.isEmpty()){
                    sourceMap.compute(id, (i, source)->{
                        if(source == null) {
                            source = defaultSource.put(Source.STANDARD_NAME, id);
                        }

                        if(isList(source, field)) {

                            List<String> values;

                            if(source.hasValue(field)) {
                                values = source.get(field);
                            } else {
                                values = new ArrayList<>();
                            }

                            values.add(value);
                            return source.put(field, values);
                        } else if (isBoolean(source, field)){

                            return source.put(field, BooleanUtils.toBoolean(value));
                        }   else if (isInt(source, field)){

                            return source.put(field, Integer.parseInt(value));
                        }  else {

                            return source.put(field, value);
                        }
                    });
                }
            }

            List<Source> sources = sourceMap.values().stream().map(s-> {

                Optional<Source> maybeSource = sourceDAO.byName(s.get(Source.STANDARD_NAME));
                if(maybeSource.isPresent()) {
                    Source source = maybeSource.get();
                    s = s.id(source.id());
                }
                return s;
            }).collect(Collectors.toList());

            sourceDAO.upsert(sources);

            return sources;
        }
    }

    private <V extends VersionedEntity<V>> boolean isList(V entity, String field) {
        if(entity.spec().get(field).getKlass().isAssignableFrom(List.class)) {
            return true;
        } else {
            return false;
        }
    }

    private <V extends VersionedEntity<V>> boolean isBoolean(V entity, String field){
        if (entity.spec().get(field).getKlass().isAssignableFrom(Boolean.class)){
            return true;
        } else {
            return false;
        }
    }

    private <V extends VersionedEntity<V>> boolean isInt(V entity, String field){
        if (entity.spec().get(field).getKlass().isAssignableFrom(Integer.class)){
            return true;
        } else {
            return false;
        }
    }



}


