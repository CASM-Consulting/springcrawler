package com.casm.acled.entities.event;


import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.VersionedEntityException;
import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.event.eventtypes.EventType_v1;
import com.casm.acled.entities.location.Location;
import com.casm.acled.entities.source.Source;
//import com.casm.acled.rest.resources.Entities;
import com.google.common.collect.ImmutableList;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Event extends VersionedEntity<Event> implements OutputPropProvider {

    public static final String SOURCES = "SOURCES";
    public static final String API_ID = "API_ID";
    public static final String EVENT_ID_CNTY = "EVENT_ID_CNTY";
    public static final String EVENT_ID_NO_CNTY = "EVENT_ID_NO_CNTY";
    public static final String EVENT_DATE = "EVENT_DATE";
    public static final String EVENT_LOCATION = "EVENT_LOCATION";
    public static final String LOCATION_PRECISION = "LOCATION_PRECISION";
    public static final String TIME_PRECISION = "TIME_PRECISION";
    public static final String EVENT_TYPE = "EVENT_TYPE";  // Only an output parameter, computed from the sub type
    public static final String SUB_EVENT_TYPE = "SUB_EVENT_TYPE";
    public static final String ACTOR1 = "ACTOR1";
    public static final String ASSOC_ACTOR_1 = "ASSOC_ACTOR_1";
    public static final String ACTOR2 = "ACTOR2";
    public static final String ASSOC_ACTOR_2 = "ASSOC_ACTOR_2";
    public static final String NOTES = "NOTES";
    public static final String MISC = "MISC";
    public static final String FATALITIES = "FATALITIES";
    public static final String FATALITIES_PRECISION = "FATALITIES_PRECISION";
    public static final String INTERACTION = "INTERACTION";
    public static final String RM_FEEDBACK = "RM_FEEDBACK";
    public static final String RM_CHECK = "RM_CHECK";
//    public static final String RM_VERIFIED = "RM_VERIFIED";
    public static final String GRM_FEEDBACK = "GRM_FEEDBACK";
    public static final String GRM_CHECK = "GRM_CHECK";
//    public static final String GRM_VERIFIED = "GRM_VERIFIED";
    public static final String HIGHLIGHT = "HIGHLIGHT";
    public static final String SCALE_PRIMARY = "SCALE_PRIMARY";
    public static final String SCALE_SECONDARY = "SCALE_SECONDARY";

    protected List<Article> articles;
    protected List<Source> sources;

    protected Actor actor1;
    protected List<Actor> assocActors1;
    protected Actor actor2;
    protected List<Actor> assocActors2;

    protected Location location;

    public Event(EntitySpecification entitySpec, String version, Map<String, Object> data, Integer id,
                 List<Article> articles, List<Source> sources, Actor actor1, List<Actor> assocActors1, Actor actor2, List<Actor> assocActors2, Location location) {
        super(entitySpec, version, data, id);
        this.articles = articles;
        this.sources = sources;
        this.actor1 = actor1;
        this.actor2 = actor2;
        this.assocActors1 = assocActors1;
        this.assocActors2 = assocActors2;
        this.location = location;
    }

    public Event(EntitySpecification entitySpec, String version, Map<String, Object> data, Integer id) {
        super(entitySpec, version, data, id);
        articles = ImmutableList.of();
        sources = ImmutableList.of();
        assocActors1 = ImmutableList.of();
        assocActors2 = ImmutableList.of();
    }

    abstract public Set<String> getTypes();

    public Event article(Article article) {
        return articles(ImmutableList.<Article>builder().addAll(articles()).add(article).build());
    }

    public Event source(Source source) {
        return sources(ImmutableList.<Source>builder().addAll(sources()).add(source).build());
    }

    public List<Article> articles () {
        return articles;
    }

    public List<Source> sources() {
        return sources;
    }

    public List<Actor> assocActors1() {
        return assocActors1;
    }

    public List<Actor> assocActors2() {
        return assocActors2;
    }

    public Event articles(List<Article> articles) {
        return construct(data, id.orElse(null), articles, sources, actor1, assocActors1, actor2, assocActors2, location);
    }

    public Event clearArticles() {
        return construct(data, id.orElse(null), ImmutableList.of(), sources, actor1, assocActors1, actor2, assocActors2, location);
    }

    public Event sources(List<Source> sources){
        return construct(data, id.orElse(null), articles, sources, actor1, assocActors1, actor2, assocActors2, location);
    }

    public Event actor1(Actor actor1){
        return construct(data, id.orElse(null), articles, sources, actor1, assocActors1, actor2, assocActors2, location);
    }

    public Event actor2(Actor actor2){
        return construct(data, id.orElse(null), articles, sources, actor1, assocActors1, actor2, assocActors2, location);
    }

    public Event assocActors1(List<Actor> assocActors1){
        return construct(data, id.orElse(null), articles, sources, actor1, assocActors1, actor2, assocActors2, location);
    }

    public Event assocActors2(List<Actor> assocActors2){
        return construct(data, id.orElse(null), articles, sources, actor1, assocActors1, actor2, assocActors2, location);
    }

    public Event location(Location location){
        return construct(data, id.orElse(null), articles, sources, actor1, assocActors1, actor2, assocActors2, location);
    }

    public Location location() {
        return location;
    }

    @Override
    protected Event newInstance(Map<String, Object> data, Integer id) {
        return construct(data, id, articles, sources, actor1, assocActors1, actor2, assocActors2, location);
    }

    private Event construct(Map<String, Object> data, Integer id, List articles, List sources, Actor actor1, List assocActor1,
                            Actor actor2, List assocActor2, Location location)  {
        try {
            Constructor<Event> constructor = (Constructor<Event>)this.getClass().getConstructor(Map.class, Integer.class,
                    List.class, List.class, Actor.class, List.class, Actor.class, List.class, Location.class);

            return constructor.newInstance(data, id, articles, sources, actor1, assocActor1, actor2, assocActor2, location);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e){
            throw new VersionedEntityException(e);
        }
    }

    private String asStr(Object o){
        return o == null? "" : o.toString();
    }

    private void putProp(String prop, Map<String, String> props){
        putProp(prop, prop, props, this);
    }

    private void putProp(String prop, Map<String, String> props, VersionedEntity fromEntity){
        putProp(prop, prop, props, fromEntity);
    }

    private void putProp(String prop, String label, Map<String, String> props, VersionedEntity fromEntity){
        if (fromEntity != null){
            props.put(label, asStr(fromEntity.get(prop)));
        } else {
            props.put(label, "");
        }
    }

    private <V extends VersionedEntity<V>> void putProp(String prop, String label, Map<String, String> props, List<V> fromEntities){
        if (fromEntities != null){
            props.put(label, fromEntities.stream().map(e -> asStr(e.get(prop))).collect(Collectors.joining(";")));
        } else {
            props.put(label, "");
        }
    }

    private String getAsStr(String key, VersionedEntity e){
        return asStr(e.get(key));
    }

    @Override
    public Map<String, String> outputProps(){
        Map<String, String> props = new LinkedHashMap<>();

        putProp(Location.ISO, props, location);

        props.put("EVENT_ID_CNTY", "");    //TODO
        props.put("EVENT_ID_NO_CNTY", ""); // TODO

        putProp(EVENT_DATE, props);

        LocalDate date = get(EVENT_DATE);
        //if it doesn't have a date... i don't know what... (might as well not throw an error)
        props.put("YEAR", date == null? "" : Integer.toString(date.getYear()));

        putProp(TIME_PRECISION, props);
        props.put(EVENT_TYPE, EventType_v1.typeFromSubType(get(SUB_EVENT_TYPE)));
        putProp(SUB_EVENT_TYPE, props);

        putProp(Actor.ACTOR_NAME, ACTOR1, props, actor1);
        putProp(Actor.ACTOR_NAME, ASSOC_ACTOR_1, props, assocActors1);
        putProp(Actor.INTER, "INTER1", props, actor1);

        putProp(Actor.ACTOR_NAME, ACTOR2, props, actor2);
        putProp(Actor.ACTOR_NAME, ASSOC_ACTOR_2, props, assocActors2);
        putProp(Actor.INTER, "INTER2", props, actor2);

//        props.put("INTERACTION", Entities.interaction(props.get("INTER1"), props.get("INTER2")));

        putProp(Location.COUNTRY, props, location);
        putProp(Location.ADMIN1, props, location);
        putProp(Location.ADMIN2, props, location);
        putProp(Location.ADMIN3, props, location);
        putProp(Location.LOCATION, props, location);
        putProp(Location.LATITUDE, props, location);
        putProp(Location.LONGITUDE, props, location);
        putProp(Location.GEO_PRECISION, props, location);

        props.put("SOURCE", sources().stream().map(s -> s.get(Source.NAME).toString()).collect(Collectors.joining(";")));
        //props.put("SOURCE_LINK ", sources().stream().map(s -> getAsStr(Source.LINK, s)).collect(Collectors.joining(";")));

        putProp(NOTES, props);
        putProp(FATALITIES, props);
        putProp(com.casm.acled.camunda.variables.Entities.DELETED, props);

        return props;
    }
}
