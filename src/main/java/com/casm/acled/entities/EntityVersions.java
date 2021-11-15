package com.casm.acled.entities;

import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.actor.ActorVersions;
import com.casm.acled.entities.actordesk.ActorDesk;
import com.casm.acled.entities.actordesk.ActorDeskVersions;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.article.ArticleVersions;
import com.casm.acled.entities.articleevent.ArticleEvent;
import com.casm.acled.entities.articleevent.ArticleEventVersions;
import com.casm.acled.entities.change.Change;
import com.casm.acled.entities.change.ChangeVersions;
import com.casm.acled.entities.crawlreport.CrawlReport;
import com.casm.acled.entities.crawlreport.CrawlReportVersions;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.desk.DeskVersions;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.event.EventVersions;
import com.casm.acled.entities.feedback.Feedback;
import com.casm.acled.entities.location.Location;
import com.casm.acled.entities.location.LocationVersions;
import com.casm.acled.entities.locationdesk.LocationDesk;
import com.casm.acled.entities.locationdesk.LocationDeskVersions;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.source.SourceVersions;
import com.casm.acled.entities.sourcedesk.SourceDesk;
import com.casm.acled.entities.sourcedesk.SourceDeskVersions;
import com.casm.acled.entities.sourcelist.SourceList;
import com.casm.acled.entities.sourcelist.SourceListVersions;
import com.casm.acled.entities.sourcesourcelist.SourceSourceList;
import com.casm.acled.entities.sourcesourcelist.SourceSourceListVersions;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EntityVersions {

    @Autowired
    private List<Event> events;

    @Autowired
    private List<Article> articles;

    @Autowired
    private List<Actor> actors;

    @Autowired
    private List<Location> locations;

    @Autowired
    private List<Source> sources;


    public static final String EVENT_HISTORY = "event_history";
    public static final String ARTICLE_HISTORY = "article_history";
    public static final String ARTICLE_EVENT_HISTORY = "article_event_history";

    public static final String DESK = "desk";
    public static final String CRAWL_REPORT = "crawl_report";
    public static final String ACTOR = "actor";
    public static final String ACTOR_DESK = "actor_desk";
    public static final String ARTICLE = "article";
    public static final String ARTICLE_EVENT = "article_event";
    public static final String EVENT = "event";
    public static final String LOCATION = "location";
    public static final String LOCATION_DESK = "location_desk";
    public static final String SOURCE = "source";
    public static final String SOURCE_DESK = "source_desk";
    public static final String SOURCE_LIST = "source_list";
    public static final String FEEDBACK = "feedback";
    public static final String SOURCE_SOURCE_LIST = "source_source_list";
    public static final String CHANGE = "change";

    private static final ImmutableMap<String, VersionedEntityLinkSupplier> links =
            ImmutableMap.<String, VersionedEntityLinkSupplier>builder()
                    .put(SOURCE_SOURCE_LIST, new SourceSourceListVersions())
                    .put(SOURCE_DESK, new SourceDeskVersions())
                    .put(LOCATION_DESK, new LocationDeskVersions())
                    .put(ACTOR_DESK, new ActorDeskVersions())
                    .put(ARTICLE_EVENT, new ArticleEventVersions())
                    .put(ARTICLE_EVENT_HISTORY, new ArticleEventVersions())
                    .build();

    private static final ImmutableMap<String, VersionedEntitySupplier> versions =
            ImmutableMap.<String, VersionedEntitySupplier>builder()
                    .putAll(links)
                    .put(ACTOR, new ActorVersions())
                    .put(ARTICLE, new ArticleVersions())
                    .put(ARTICLE_HISTORY, new ArticleVersions())
                    .put(EVENT, new EventVersions())
                    .put(EVENT_HISTORY, new EventVersions())
                    .put(LOCATION, new LocationVersions())
                    .put(DESK, new DeskVersions())
                    .put(CRAWL_REPORT, new CrawlReportVersions())
                    .put(SOURCE, new SourceVersions())
                    .put(SOURCE_LIST, new SourceListVersions())
                    .put(CHANGE, new ChangeVersions())
                    .build();

    private static final ImmutableMap<Class, String> linkMap =
            ImmutableMap.<Class, String>builder()
                    .put(SourceSourceList.class, SOURCE_SOURCE_LIST)
                    .put(LocationDesk.class, LOCATION_DESK)
                    .put(SourceDesk.class, SOURCE_DESK)
                    .put(ActorDesk.class, ACTOR_DESK)
                    .put(ArticleEvent.class, ARTICLE_EVENT)
                    .build();

    private static final ImmutableMap<Class, String> classMap =
    ImmutableMap.<Class, String>builder()
                    .putAll(linkMap)
                    .put(Actor.class, ACTOR)
                    .put(Article.class, ARTICLE)
                    .put(Event.class, EVENT)
                    .put(Location.class, LOCATION)
                    .put(Desk.class, DESK)
                    .put(CrawlReport.class, CRAWL_REPORT)
                    .put(Source.class, SOURCE)
                    .put(SourceList.class, SOURCE_LIST)
                    .put(Feedback.class, FEEDBACK)
                    .put(Change.class, CHANGE)
                    .build();


    public static final <V extends VersionedEntity<V>> VersionedEntitySupplier<V> get(Class<V> klass) {
        return get(classMap.get(klass));
    }

    public static final <V extends VersionedEntity<V>> VersionedEntitySupplier<V> get(String klass) {
        VersionedEntitySupplier supplier = versions.get(klass);

        if (supplier == null) {
            throw new RuntimeException("Unable to look up supplier for entity " + klass);
        }

        return supplier;
    }

    public static final <V extends VersionedEntityLink<V>> VersionedEntityLinkSupplier<V> getLink(Class<V> klass) {
        return getLink(linkMap.get(klass));
    }

    public static final <V extends VersionedEntityLink<V>> VersionedEntityLinkSupplier<V> getLink(String klass) {
        VersionedEntityLinkSupplier supplier = links.get(klass);

        if (supplier == null) {
            throw new RuntimeException("Unable to look up supplier for entity " + klass);
        }

        return supplier;
    }


    private <V extends VersionedEntity<V>> V entity(List<V> versions, String version) {

        V entity = null;
        for(V e : versions) {
            if(e.version().equals(version)) {
                entity = e;
            }
        }

        if(entity == null){
            throw new VersionedEntityException("Unknown version " + version + ".");
        }

        return entity;
    }


    public Event event(String version) {
        return entity(events, version);
    }
    public Article article(String version) {
        return entity(articles, version);
    }
    public Actor actor(String version) {
        return entity(actors, version);
    }
    public Location location(String version) {
        return entity(locations, version);
    }
    public Source source(String version) {
        return entity(sources, version);
    }
}
