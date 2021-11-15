package com.casm.acled.entities.source;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.VersionedEntityException;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class Source extends VersionedEntity<Source> {

    public static final String NAME = "NAME";
    public static final String STANDARD_NAME = "STANDARD_NAME";
    public static final String REGION = "REGION";
    public static final String COUNTRY = "COUNTRY";
    public static final String ADMIN1 = "ADMIN1";
    public static final String SCALE = "SCALE";
    public static final String NOTES = "NOTES";
    public static final String ALIAS = "ALIAS";
    public static final String LINK = "LINK";
    public static final String VERIFIED = "VERIFIED";
    public static final String DATE_FORMAT = "DATE_FORMAT";
    public static final String TIMEZONE = "TIMEZONE";
    public static final String LANGUAGES = "LANGUAGES";
    public static final String COUNTRY_CODES = "COUNTRY_CODES";
    public static final String LOCALES = "LOCALES";

    public static final String EXAMPLE_URLS = "EXAMPLE_URLS";
    public static final String SEED_URLS = "SEED_URLS";
    public static final String CRAWL_RECRAWL_PATTERN = "CRAWL_RECRAWL_PATTERN";
    public static final String CRAWL_DISABLED = "CRAWL_DISABLED";
    public static final String CRAWL_DEPTH = "CRAWL_DEPTH";
    public static final String CRAWL_OFF_DOMAIN = "CRAWL_OFF_DOMAIN"; // allow crawler to follow links to other domains
    public static final String CRAWL_EXCLUDE_PATTERN = "CRAWL_EXCLUDE_PATTERN";
    public static final String CRAWL_IGNORE_SITEMAP = "CRAWL_IGNORE_SITEMAP";
    public static final String CRAWL_IGNORE_ROBOTS = "CRAWL_IGNORE_ROBOTS";
    public static final String CRAWL_INCLUDE_SUBDOMAINS = "CRAWL_INCLUDE_SUBDOMAINS";
    public static final String CRAWL_SCHEDULE = "CRAWL_SCHEDULE";
    public static final String CRAWL_JOB_ID  = "CRAWL_JOB_ID";
    public static final String CRAWL_SCRAPER_PATH  = "CRAWL_SCRAPER_PATH";
    public static final String CRAWL_DISABLE_SITEMAP_DISCOVERY = "CRAWL_DISABLE_SITEMAP_DISCOVERY";
    public static final String CRAWL_DISABLE_SITEMAPS = "CRAWL_DISABLE_SITEMAPS";
    public static final String CRAWL_SITEMAP_LOCATIONS = "CRAWL_SITEMAP_LOCATIONS";

    public static final String SCRAPER_RULE_ARTICLE = "SCRAPER_RULE_ARTICLE";
    public static final String SCRAPER_RULE_TITLE = "SCRAPER_RULE_TITLE";
    public static final String SCRAPER_RULE_DATE = "SCRAPER_RULE_DATE";

    List<SourceList> sourceLists;

    public Source(EntitySpecification entitySpecification, String version, Map<String, Object> data, Integer id) {
        super(entitySpecification, version, data, id);
        sourceLists = ImmutableList.of();
    }

    public Source(EntitySpecification entitySpecification, String version, Map<String, Object> data, Integer id, List<SourceList> sourceLists) {
        super(entitySpecification, version, data, id);
        this.sourceLists = sourceLists;
    }

    public List<SourceList> sourceLists() {
        return sourceLists;
    }

    public Source sourceLists(List<SourceList> sourceLists) {
        try {
            Constructor<Source> constructor = (Constructor<Source>)this.getClass().getConstructor(Map.class, Integer.class, List.class);
            return constructor.newInstance(data, id.orElse(null), sourceLists);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Source newInstance(Map<String, Object> data, Integer id) {
        try {
            Constructor<Source> constructor = (Constructor<Source>)this.getClass().getConstructor(Map.class, Integer.class, List.class);
            return constructor.newInstance(data, id, sourceLists);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new VersionedEntityException(e);
        }
    }
}
