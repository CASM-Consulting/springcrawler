package com.casm.acled.crawler.scraper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.casm.acled.crawler.ScraperNotFoundException;
import com.casm.acled.crawler.util.Util;
import com.casm.acled.entities.source.Source;

import com.google.common.collect.ImmutableMap;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.tagger.impl.DOMTagger.DOMExtractDetails;
import com.norconex.importer.util.DOMUtil;

import com.norconex.importer.handler.tagger.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.susx.tag.norconex.scraping.GeneralSplitterFactory;
import uk.ac.susx.tag.norconex.scraping.POJOHTMLMatcherDefinition;

/**
 *
 */
public class ACLEDTagger {
    // should be initialised by scraperPath, Source source, Reporter reporter(no need),
    // addDOMExtractDetails by the job.json file from scraperPath;
    // then if should do the work;

    protected static final Logger logger = LoggerFactory.getLogger(ACLEDTagger.class);

    // html file used for testing;
    String xmlstr;

    public static final String JOB_JSON = "job.json";
    public static final String ARTICLE = "field.name/article";
    public static final String TITLE = "field.name/title";
    public static final String DATE = "field.name/date";

    private final Source source;
    private final Path scraperPath;


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

    private Map<String, List<Map<String, String>>> getDef() {

        String processed = Util.processJSON(scraperPath.toFile());

        try {

            Map<String, List<Map<String, String>>> scraperDef = buildScraperDefinition(GeneralSplitterFactory.parseJsonTagSet(processed));
            return scraperDef;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadRule(Source source, Map<String, List<Map<String, String>>> scraperDef, DOMTagger tagger, String sourceField, String prop, String pipelineField) {
        if (source.hasValue(sourceField)) {
            String articleRule = source.get(sourceField);
            addDOMDetailsSingleFromQuery(articleRule, prop, pipelineField, tagger);
        } else {
            addDOMDetailsSingle(scraperDef, prop, pipelineField, tagger);
        }

    }

    /**
     * Simple wrapper for the DOMTagger class to provide convenient public access to the
     * underlying protected tagApplicableDocument() method of the DOMTagger.
     * Returned by the ACLEDTagger.get() method.
     */
    public static class DomTaggerOpenAccess extends DOMTagger {

        /**
         * Convenience method for directly tagging a String of html.
         * Permits public availability of the functionality of the protected tagApplicableDocument() method.
         */
        public Map<String, String> tag(String html) throws ImporterHandlerException {

            ImporterMetadata metadata = new ImporterMetadata();

            // URL param left blank and parsed set to true - so assuming already UTF-8
            super.tagApplicableDocument("", new ByteArrayInputStream(html.getBytes()), metadata, true);

            return new ImmutableMap.Builder<String, String>()
                    .put(ScraperFields.SCRAPED_ARTICLE, metadata.getString(ScraperFields.SCRAPED_ARTICLE))
                    .put(ScraperFields.SCRAPED_DATE, metadata.getString(ScraperFields.SCRAPED_DATE))
                    .put(ScraperFields.SCRAPED_TITLE, metadata.getString(ScraperFields.SCRAPED_TITLE))
                    .build();
        }
    }

    public DomTaggerOpenAccess get()  {
        // hoow to separate them.. all from file; all from source; part from file and part from source;
        DomTaggerOpenAccess tagger = new DomTaggerOpenAccess();

        Map<String, List<Map<String, String>>> scraperDef = getDef();

        loadRule(source, scraperDef, tagger, Source.SCRAPER_RULE_ARTICLE, ARTICLE, ScraperFields.SCRAPED_ARTICLE);
        loadRule(source, scraperDef, tagger, Source.SCRAPER_RULE_TITLE, TITLE, ScraperFields.SCRAPED_TITLE);
        loadRule(source, scraperDef, tagger, Source.SCRAPER_RULE_DATE, DATE, ScraperFields.SCRAPED_DATE);

        return tagger;
    }


    public static String constructRoot(Map<String, List<Map<String, String>>> entry) {
        String rootSelector = "";
        List<Map<String, String>> rootValues = entry.get("root/root");
        for (Map<String, String> rootValue : rootValues) {
            if (rootValue.containsKey("tag") || rootValue.containsKey("custom")) {
                if (rootSelector.equals("")) {
                    if (rootValue.containsKey("tag")) {
                        rootSelector += rootValue.get("tag");
                    }
                    if (rootValue.containsKey("custom")){
                        rootSelector += rootValue.get("custom");
                    }
                }
                else {
                    // for sub spans, always get the first one;
                    if (rootValue.containsKey("tag")) {
                        // seems okay to add :nth-child(1) or not
                        rootSelector += " " +rootValue.get("tag") + "";
                    }
                    if (rootValue.containsKey("custom")) {
                        // seems okay to add :nth-child(1) or not
                        rootSelector += " " +rootValue.get("custom") + "";
                    }

                }
                if (rootValue.containsKey("class")) {
                    rootSelector += "." + rootValue.get("class");
                }
                if( rootValue.containsKey("att")) {
                    rootSelector += "[" + rootValue.get("att") + "]";

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

            String att = null;

            String extract = "text";

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
                    if( select.containsKey("att")) {
                        selector += "[" + select.get("att") + "]";
                        att = select.get("att");
                    }

                    if(att != null) {
                        extract = "attr("+att+")";
                    }
                }

            }
            // separate them in case want to modify article's selector;
            if (field.equals(ARTICLE)) {
                t.addDOMExtractDetails(new DOMExtractDetails(rootSelector + selector, ScraperFields.SCRAPED_ARTICLE, true, extract));
            }
            else if (field.equals(TITLE)){
                t.addDOMExtractDetails(new DOMExtractDetails(rootSelector + selector, ScraperFields.SCRAPED_TITLE, true, extract));
            }
            else if (field.equals(DATE)) {
                t.addDOMExtractDetails(new DOMExtractDetails(rootSelector + selector, ScraperFields.SCRAPED_DATE, true, extract));

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

        String att = null;

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
                if( select.containsKey("att")) {
                    selector += "[" + select.get("att") + "]";
                    att=select.get("att");
                }
            }
        }

        String extract = "text";
        if(att != null) {
            extract = "attr("+att+")";
        }
        tagger.addDOMExtractDetails(new DOMExtractDetails(rootSelector + selector, toField, true, extract));

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
        Map<String, String> params = new HashMap<>();
        params.put("<script.*?>.*?<\\/script>", "");
        ACLEDTransformer transformer = new ACLEDTransformer(params);

        ACLEDTagger a = new ACLEDTagger("/Users/pengqiwei/Downloads/My/PhDs/acled_thing/acled-scrapers/24chasabg");
        a.get();
        DOMTagger t = a.get();

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
