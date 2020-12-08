package com.casm.acled.crawler.management;

import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.spring.CrawlService;
import com.casm.acled.crawler.util.Util;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.dao.entities.SourceSourceListDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.csv.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
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

    @Autowired
    private CrawlService crawlService;

    public void exportSources(CrawlArgs args) throws IOException {

        if (args.workingDir == null || args.path == null){
            throw new RuntimeException("Must specify a working directory (-wd) and path (-P) to export a SourceList.");
        }

        SourceList sourceList = args.sourceLists.get(0);

        Path path = args.workingDir.resolve(args.path);

        exportCrawlerSourcesToCSV(path, sourceList);
    }

    public void importSources(CrawlArgs args) throws IOException {

        if (args.workingDir == null || args.path == null){
            throw new RuntimeException("Must specify a working directory (-wd) and path (-P) to find the source list import file.");
        }

        Path path = args.workingDir.resolve(args.path);

        List<Source> sources = importSourceConfigsFromCSV(path, EntityVersions.get(Source.class).current());

        if(args.flagSet.contains("L")) {

            for(SourceList list : args.sourceLists) {
                for(Source source : sources) {
                    sourceSourceListDAO.link(source, list);
                }
            }
        }
    }

    /**
     * Read a CSV of Sources where column 1 is the name of the Source and column 2 is its link.
     * Attempt to resolve the names and links to existing Sources in the database, and extract the list
     * of merged sources (producing warnings and info about the results of merges).
     *
     * The if the C flag is specified, create the specified source lists and link the sources to those lists.
     */
    public void importList(CrawlArgs args) throws IOException {

        if (args.workingDir == null || args.path == null){
            throw new RuntimeException("Must specify a working directory (-wd) and path (-P) to find the source list import file.");
        }

        boolean create = args.flagSet.contains("C");

        Path path = args.workingDir.resolve(args.path);

        List<Source> sources = loadSourceCSV(path,
                ImmutableList.of("SOURCE", "URL"),
                EntityVersions.get(Source.class).current(),
                create
        );

        String listName = args.name;

        //create list and link
        if(create) {

            Optional<SourceList> maybeList = sourceListDAO.byName(listName);
            SourceList list;

            if(maybeList.isPresent()) {
                logger.warn("list exists {}", listName);
                list = maybeList.get();
            } else {
                list = EntityVersions.get(SourceList.class).current();
                list = list.put(SourceList.LIST_NAME, listName);
                list = sourceListDAO.create(list);
            }

            for(Source source : sources) {
                sourceSourceListDAO.link(source, list);
            }
        }
    }

    /**
     * Read a CSV of Sources where column 1 is the name of the Source and column 2 is its link.
     * Attempt to resolve the names and links to existing Sources in the database, and return the list
     * of merged sources (producing warnings and info about the results of merges).
     */
    private List<Source> loadSourceCSV(Path path, List<String> headers, Source defaultSource, boolean create) throws IOException {

        List<Source> sources = new ArrayList<>();

        try (
            Reader reader = java.nio.file.Files.newBufferedReader(path);
            CSVParser csvReader = new CSVParser(reader, CSVFormat.EXCEL.withFirstRecordAsHeader());
        ) {
            Iterator<CSVRecord> itr = csvReader.iterator();

            Map<String, List<String>> sourceMap = new HashMap<>();

            String nameHeader = headers.get(0);
            String linkHeader = headers.get(1);

            while(itr.hasNext()) {
                CSVRecord record = itr.next();

                String name = record.get(nameHeader);
                String link = record.get(linkHeader);

                if(!sourceMap.containsKey(name)) {
                    sourceMap.put(name, new ArrayList<>());
                }

                sourceMap.get(name).add(link);

            }


            for(Map.Entry<String, List<String>> entry : sourceMap.entrySet()) {

                String name = entry.getKey();

                name = name.trim();

                List<String> links = entry.getValue();

//                links = resolve( links );

                Optional<Source> maybeSource = sourceDAO.byName(name);

                Source source;

                if (maybeSource.isPresent()) {

                    source = maybeSource.get();
                    logger.warn("source found {}", name);
                } else {

                    source = defaultSource
                            .put(Source.STANDARD_NAME, name);
                    logger.warn("source not found {}", name);
                }

                source = reconcileLinks(source, links);

                if(create) {
                    source = sourceDAO.upsert(source);
                }

                sources.add(source);
            }

            return sources;
        }
    }

    /**
     * Ensure all links have the protocol and follow redirects to the
     * final link.
     */
    private List<String> resolve(List<String> links) {
        ListIterator<String> itr = links.listIterator();
        while(itr.hasNext()) {
            String link = itr.next();

            link = link.trim();

            String plink = Util.ensureHTTP(link, false);

            String rlink = crawlService.followRedirects(plink);

            itr.set(rlink);
        }

        return links;
    }

    /**
     * Given an existing source and zero or more links provided for the the source, place links on the source
     * in the appropriate fields.
     *
     * Log warnings for link mismatches.
     */
    private Source reconcileLinks(Source source, List<String> links) {
        String name = source.get(Source.STANDARD_NAME);
        String link = null;
        if(source.hasValue(Source.LINK)) {
            link = source.get(Source.LINK);
        }

        if (link == null) {
            if(links.size() > 1) {
                logger.info("{}: adding links {} to SEED_URLS",  name, StringUtils.join(links, ", "));

                source = source.put(Source.SEED_URLS, links);
            } else {

                logger.info("{}: adding link {} to LINK",  name, StringUtils.join(links, ", "));
                source = source.put(Source.LINK, links.get(0));
            }
        } else {
            if(links.size() > 1) {
                if (links.contains(link)) {
                    logger.info("{}: adding links {} to SEED_URLS",  name, StringUtils.join(links, ", "));

                    source = source.put(Source.SEED_URLS, links);
                } else {

                    logger.warn("{}: existing link {} not in {}", name, link, StringUtils.join(links, ", "));
                    source = source.put(Source.SEED_URLS, links);
                }
            } else {
                if(links.get(0).equals(link)) {

                    logger.info("{}: existing link matches {}", name, link);
                    //link match: no-op
                } else {

                    logger.warn("{}: link mismatch, existing:{} new:{}", name, link, links.get(0));
                }
            }
        }

        return source;
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

    private List<Source> importSourceConfigsFromCSV(Path seedsPath, Source defaultSource) throws IOException {
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


