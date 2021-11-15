package com.casm.acled.dao;

import com.casm.acled.camunda.variables.Process;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.crawlreport.CrawlReport;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.location.Location;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableSet;

import java.util.Set;


/**
 * Contains white lists for JDBC objects to mitigate SQL injection.
 */
public class JDBCObjectValidation {

    private static final Set<String> TABLES;
    static {
        TABLES = ImmutableSet.of(
                "ACLED_article_event",
                "ACLED_article",
                "ACLED_source",
                "ACLED_desk",
                "ACLED_event",
                "ACLED_actor",
                "ACLED_actor_desk",
                "ACLED_location",
                "ACLED_location_desk",
                "ACLED_source_list",
                "ACLED_source_source_list",
                "ACLED_source_desk",
                "ACLED_crawl_report",
                "ACLED_change",

                "ACLED_hi_article_event",
                "ACLED_hi_article",
                "ACLED_hi_event"
        );
    }

    private static final Set<String> FIELDS;


    public static void validTable(String table) {
        if(!TABLES.contains(table)) {
            throw new RuntimeException("Invalid table " + table);
        }
    }
    public static void validField(String field) {
        if (!FIELDS.contains(field)) {
            throw new RuntimeException("Invalid field " + field);
        }
    }

    static {
        FIELDS = ImmutableSet.<String>builder()
            .addAll( EntityVersions.get(CrawlReport.class).current().spec().names() )
            .addAll( EntityVersions.get(Actor.class).current().spec().names() )
            .addAll( EntityVersions.get(Article.class).current().spec().names() )
            .addAll( EntityVersions.get(Event.class).current().spec().names() )
            .addAll( EntityVersions.get(Location.class).current().spec().names() )
            .addAll( EntityVersions.get(Desk.class).current().spec().names() )
            .addAll( EntityVersions.get(Source.class).current().spec().names() )
//            .addAll( EntityVersions.get(Feedback.class).current().spec().names() )
            .addAll( EntityVersions.get(SourceList.class).current().spec().names() )
            .add(Process.BUSINESS_KEY)
            .build();
    }
}
