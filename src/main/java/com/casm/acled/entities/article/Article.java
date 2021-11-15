package com.casm.acled.entities.article;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.VersionedEntityException;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.event.OutputPropProvider;
import com.casm.acled.entities.source.Source;
import com.google.common.collect.ImmutableList;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Article extends VersionedEntity<Article> implements OutputPropProvider {

    public static final String TEXT = "TEXT";
    public static final String SOURCE_ID = "SOURCE_ID";
    public static final String NOTES = "NOTES";
    public static final String URL = "URL";
    public static final String DATE = "DATE";
    public static final String EVENT_STUB_IDS = "EVENT_STUB_IDS";
    public static final String TITLE = "TITLE";
    public static final String CRAWL_DATE = "CRAWL_DATE";
    public static final String CRAWL_DEPTH = "CRAWL_DEPTH";
    public static final String SCRAPE_DATE = "SCRAPE_DATE";
    public static final String SCRAPE_KEYWORD_HIGHLIGHT = "SCRAPE_KEYWORD_HIGHLIGHT";
    public static final String SCRAPE_RAW_HTML = "SCRAPE_RAW_HTML";

    protected final List<Event> events;
    protected final Source source;

    public Article(EntitySpecification entitySpec, String version, Map<String, Object> data, Integer id) {
        this(entitySpec, version, data, id, ImmutableList.of(), null);
    }

    public Article(EntitySpecification entitySpec, String version, Map<String, Object> data, Integer id, List<Event> events, Source source) {
        super(entitySpec, version, data, id);
        this.events = events;
        this.source = source;
    }

    public Article article(Event event) {
        return events(ImmutableList.<Event>builder().addAll(events()).add(event).build());
    }

    public Source source() {
        return source;
    }

    public Article source(Source source) {
        return construct(data, id.orElse(null), events, source);
    }

    public List<Event> events() {
        return events;
    }

    public Article clearEvents() {
        return construct(data, id.orElse(null), ImmutableList.of(), source);
    }

    public Article events(List<Event> events) {
        return construct(data, id.orElse(null), events, source);
    }

    @Override
    protected Article newInstance(Map<String, Object> data, Integer id) {

        return construct(data, id, events, source);
    }

    private Article construct(Map<String, Object> data, Integer id, List<Event> events, Source source)  {
        try {
            Constructor<Article> constructor = (Constructor<Article>)this.getClass().getConstructor(Map.class, Integer.class, List.class, Source.class);

            return constructor.newInstance(data, id, events, source);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e){
            throw new VersionedEntityException(e);
        }
    }

    @Override
    public Map<String, String> outputProps(){
        Map<String, String> props = new LinkedHashMap<>();
        // Fictional field

        Source source = source();

        if (source != null) {
            props.put("SOURCE", source().get(Source.NAME));
        }

        props.put(Article.TITLE, v(Article.TITLE));
        props.put(Article.URL, v(Article.URL));
        props.put(Article.DATE, v(Article.DATE));
        props.put(Article.TEXT, v(Article.TEXT));
        props.put(Article.NOTES, v(Article.NOTES));

        return props;
    }

    private String v(String field) {
        Object value = get(field);
        if (value == null) {
            return "";
        } else {
            return value.toString();
        }
    }
}
