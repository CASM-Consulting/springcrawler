package com.casm.acled.crawler.scraper;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.*;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.casm.acled.crawler.ScraperNotFoundException;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.util.Util;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.source.Source;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.config.XMLConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.impl.DOMTagger.DOMExtractDetails;
import com.norconex.importer.util.DOMUtil;

import com.norconex.importer.handler.tagger.impl.*;
import com.norconex.importer.handler.tagger.impl.DOMTagger.DOMExtractDetails;
import org.apache.xalan.xsltc.DOM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.susx.tag.norconex.scraping.GeneralSplitterFactory;
import uk.ac.susx.tag.norconex.scraping.POJOHTMLMatcherDefinition;

/**
 * @author Pascal Essiembre
 * @since 2.4.0
 */
public class ACLEDTagger {
    // should be initialised by scraperPath, Source source, Reporter reporter(no need),
    // addDOMExtractDetails by the job.json file from scraperPath;
    // then if should do the work;

    protected static final Logger logger = LoggerFactory.getLogger(ACLEDTagger.class);

    // html file used for testing;
    String xmlstr;

    private final Path scraperPath;
    public static final String JOB_JSON = "job.json";
    public static final String ARTICLE = "field.name/article";
    public static final String TITLE = "field.name/title";
    public static final String DATE = "field.name/date";
    private final Source source;


    public static DOMTagger tagger;


    public ACLEDTagger(Path path, Source source) {
        if(source.hasValue(Source.CRAWL_SCRAPER_PATH)) {
            path = Paths.get((String)source.get(Source.CRAWL_SCRAPER_PATH));
        } else {
            String id = Util.getID(source);
            path = path.resolve(id);
        }

        this.scraperPath = path.resolve(JOB_JSON);
        this.source = source;
        if(Files.notExists(this.scraperPath)) {
            throw new ScraperNotFoundException(this.scraperPath + " doesn't exist");
        }
    }

    public ACLEDTagger(String jsonPath) {
        Path path = Paths.get(jsonPath);
        this.scraperPath = path.resolve(JOB_JSON);
        this.source = null;
    }

    public static Map<String, List<Map<String, String>>> buildScraperDefinition(List<POJOHTMLMatcherDefinition> matcherList) {

        Map<String, List<Map<String, String>>> fields = new HashMap<>();
        for(POJOHTMLMatcherDefinition matcher : matcherList) {
            List<Map<String, String>> tags = matcher.getTagDefinitions();
            fields.put(matcher.field,tags);
        }
        return fields;
    }

    public void load() throws IOException {
        // hoow to separate them.. all from file; all from source; part from file and part from source;
        DOMTagger t = new DOMTagger();

        String processed = Util.processJSON(scraperPath.toFile());
        Map<String, List<Map<String, String>>> scraperDef = buildScraperDefinition(GeneralSplitterFactory.parseJsonTagSet(processed));

        String articleRule = source.get(Source.SCRAPER_RULE_ARTICLE);
        String titleRule = source.get(Source.SCRAPER_RULE_TITLE);
        String dateRule = source.get(Source.SCRAPER_RULE_DATE);


        if (articleRule!=null) {
//            Map<String, List<Map<String, String>>> articleDef = buildScraperDefinition(GeneralSplitterFactory.parseJsonTagSet(articleRule));
//            addDOMDetailsSingle(articleDef, ARTICLE, ScraperFields.SCRAPED_ARTICLE, t);
            addDOMDetailsSingleFromQuery(articleRule, ARTICLE, ScraperFields.SCRAPED_ARTICLE, t);
        }
        else {
            addDOMDetailsSingle(scraperDef, ARTICLE, ScraperFields.SCRAPED_ARTICLE, t);
        }

        if (titleRule!=null) {
//            Map<String, List<Map<String, String>>> titleDef = buildScraperDefinition(GeneralSplitterFactory.parseJsonTagSet(titleRule));
//            addDOMDetailsSingle(titleDef, TITLE, ScraperFields.SCRAPED_TITLE, t);
            addDOMDetailsSingleFromQuery(titleRule, TITLE, ScraperFields.SCRAPED_TITLE, t);

        }
        else {
            addDOMDetailsSingle(scraperDef, TITLE, ScraperFields.SCRAPED_TITLE, t);
        }

        if (dateRule!=null) {
//            Map<String, List<Map<String, String>>> dateDef = buildScraperDefinition(GeneralSplitterFactory.parseJsonTagSet(dateRule));
//            addDOMDetailsSingle(dateDef, DATE, ScraperFields.SCRAPED_DATE, t);
            addDOMDetailsSingleFromQuery(dateRule, DATE, ScraperFields.SCRAPED_DATE, t);
        }
        else {
            addDOMDetailsSingle(scraperDef, DATE, ScraperFields.SCRAPED_DATE, t);
        }
//        addDOMDetailsAll(scraperDef, t);
        tagger = t;
    }

    public static DOMTagger load(Path path, Source source) {
        ACLEDTagger acledTagger = new ACLEDTagger(path, source);
        try {
             acledTagger.load();
            return tagger;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String constructRoot(Map<String, List<Map<String, String>>> entry) {
        String rootSelector = "";
        List<Map<String, String>> rootValues = entry.get("root/root");
        for (Map<String, String> rootValue : rootValues) {
            if (rootValue.containsKey("tag") || rootValue.containsKey("custom")) {
                if (rootSelector.equals("")) {
                    if (rootValue.containsKey("tag")) {
                        rootSelector = rootSelector + rootValue.get("tag");
                    }
                    if (rootValue.containsKey("custom")){
                        rootSelector = rootSelector + rootValue.get("custom");
                    }
                }
                else {
                    // for sub spans, always get the first one;
                    if (rootValue.containsKey("tag")) {
                        // seems okay to add :nth-child(1) or not
                        rootSelector = rootSelector + " " +rootValue.get("tag") + "";
                    }
                    if (rootValue.containsKey("custom")) {
                        // seems okay to add :nth-child(1) or not
                        rootSelector = rootSelector + " " +rootValue.get("custom") + "";
                    }

                }
                if (rootValue.containsKey("class")) {
                    rootSelector = rootSelector + "." + rootValue.get("class");
                }
            }


        }
    return rootSelector;
    }

    public void addDOMDetailsAll(Map<String, List<Map<String, String>>> scraperDef, DOMTagger t) {
        t.setParser(DOMUtil.PARSER_XML);

        // add root scope for searching;
        String rootSelector = constructRoot(scraperDef);
        if (!rootSelector.equals("")) {
            rootSelector = rootSelector + " ";
        }

        for (Map.Entry<String, List<Map<String, String>>> entry : scraperDef.entrySet()) {
            String field = entry.getKey();
            String selector = "";

            for (Map<String, String> select : entry.getValue()) {

                if (select.containsKey("tag") || select.containsKey("custom")) {
                    if (selector.equals("")) {
                        if (select.containsKey("tag")) {
                            selector = selector + select.get("tag");
                        }
                        if (select.containsKey("custom")){
                            selector = selector + select.get("custom");
                        }
                    }
                    else {
                        // for sub spans, always get the first one;
                        if (select.containsKey("tag")) {
                            // seems okay to add :nth-child(1) or not
                            selector = selector + " " +select.get("tag") + "";
                        }
                        if (select.containsKey("custom")) {
                            // seems okay to add :nth-child(1) or not
                            selector = selector + " " +select.get("custom") + "";
                        }

                    }
                    if (select.containsKey("class")) {
                        selector = selector + "." + select.get("class");
                    }
                }

            }
            // separate them in case want to modify article's selector;
            if (field.equals(ARTICLE)) {
                t.addDOMExtractDetails(new DOMExtractDetails(rootSelector + selector + "", ScraperFields.SCRAPED_ARTICLE, true, "text"));
            }
            else if (field.equals(TITLE)){
                t.addDOMExtractDetails(new DOMExtractDetails(rootSelector + selector, ScraperFields.SCRAPED_TITLE, true, "text"));
            }
            else if (field.equals(DATE)) {
                t.addDOMExtractDetails(new DOMExtractDetails(rootSelector + selector, ScraperFields.SCRAPED_DATE, true, "text"));

            }
        }

    }

    public void addDOMDetailsSingle(Map<String, List<Map<String, String>>> scraperDef, String fromField, String toField, DOMTagger tagger) {
        String rootSelector = constructRoot(scraperDef);
        if (!rootSelector.equals("")) {
            rootSelector = rootSelector + " ";
        }

        List<Map<String, String>> entry = scraperDef.get(fromField);

        String selector = "";

        for (Map<String, String> select : entry) {

            if (select.containsKey("tag") || select.containsKey("custom")) {
                if (selector.equals("")) {
                    if (select.containsKey("tag")) {
                        selector = selector + select.get("tag");
                    }
                    if (select.containsKey("custom")){
                        selector = selector + select.get("custom");
                    }
                }
                else {
                    // for sub spans, always get the first one;
                    if (select.containsKey("tag")) {
                        // seems okay to add :nth-child(1) or not
                        selector = selector + " " +select.get("tag") + "";
                    }
                    if (select.containsKey("custom")) {
                        // seems okay to add :nth-child(1) or not
                        selector = selector + " " +select.get("custom") + "";
                    }

                }
                if (select.containsKey("class")) {
                    selector = selector + "." + select.get("class");
                }
            }

        }

        tagger.addDOMExtractDetails(new DOMExtractDetails(rootSelector + selector, toField, true, "text"));

    }

    public void addDOMDetailsSingleFromQuery(String query, String fromField, String toField, DOMTagger tagger) {

        tagger.addDOMExtractDetails(new DOMExtractDetails(query, toField, true, "text"));

    }

    // used for testing;
    public void testXMLParser(DOMTagger t)
            throws ImporterHandlerException, IOException {

        ImporterMetadata metadata = new ImporterMetadata();
        performTagging(metadata, t, xmlstr);

        String article = metadata.getString(ARTICLE);
        String title = metadata.getString(TITLE);
        String date = metadata.getString(DATE);

    }

    // used for testing;
    private void performTagging(
            ImporterMetadata metadata, DOMTagger tagger, String html)
            throws ImporterHandlerException, IOException {
        InputStream is = new ByteArrayInputStream(html.getBytes());
        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");

        tagger.tagDocument("n/a", is, metadata, false);
        is.close();
    }

    // used for testing: loading large html file;
    public void setXML() throws IOException {

        String d = new String(Files.readAllBytes(Paths.get("/Users/pengqiwei/Downloads/My/PhDs/acled_thing/test.html")));


        this.xmlstr = d;
    }

    public static void main(String[] args) throws ImporterHandlerException, IOException{
        // test 24chasabg, zeriinfo, awe24com
        // now to check scraperService's behaviour; to match them; see the difference;
        // could use p to get all p and combine them and setString back;

//        Source source = EntityVersions.get(Source.class).current()
//                .put(Source.EXAMPLE_URLS, ImmutableList.of("https://awe24.com/51482/", "https://awe24.com/51482/"))
//                .id(0)
//                .put(Source.CRAWL_SCRAPER_PATH, "/Users/pengqiwei/Downloads/My/PhDs/acled_thing/acled-scrapers/awe24com");

//        DOMTagger t = ACLEDTagger.load(Paths.get("/Users/pengqiwei/Downloads/My/PhDs/acled_thing/acled-scrapers"), source);

        // here add a transformer:
        Map<String, String> params = new HashMap<String, String>();
        params.put("<script.*?>.*?<\\/script>", "");
        ACLEDTransformer transformer = new ACLEDTransformer(params);

        ACLEDTagger a = new ACLEDTagger("/Users/pengqiwei/Downloads/My/PhDs/acled_thing/acled-scrapers/24chasabg");
        a.load();
        DOMTagger t = a.tagger;

        a.setXML();
        // test transformer here, replace all scripts in the source html and return the replaced string to tagger;
        a.xmlstr = transformer.transform(a.xmlstr);
        a.testXMLParser(t);

        ImporterMetadata metadata = new ImporterMetadata();
        a.performTagging(metadata, t, a.xmlstr);

        // have to postprocess it like this, to concatenate all strings;
        List<String> articles = metadata.getStrings(ARTICLE);
        String concatedString = String.join(" ", articles);

        String article = metadata.getString(ARTICLE);
        String title = metadata.getString(TITLE);
        String date = metadata.getString(DATE);
    }

}
