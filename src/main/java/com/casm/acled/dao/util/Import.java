package com.casm.acled.dao.util;

import com.casm.acled.AcledObjectMapper;
import com.casm.acled.dao.LinkDAO;
import com.casm.acled.dao.VersionedEntityDAO;
import com.casm.acled.dao.entities.*;
import com.casm.acled.dao.entities.history.EventHistoryDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.location.Location;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class Import  {

    private final ActorDAO actorDAO;
    private final ActorDeskDAO actorDeskDAO;
    private final LocationDAO locationDAO;
    private final LocationDeskDAO locationDeskDAO;
    private final SourceDAO sourceDAO;
    private final DeskDAO deskDAO;
    private final SourceListDAO sourceListDAO;
    private final SourceDeskDAO sourceDeskDAO;
    private final SourceSourceListDAO sourceSourceListDAO;
    private final EventHistoryDAO eventHistoryDAO;


//    private final Function<Source,Source> insertSourceRegions;

    @Autowired
    public Import(ActorDAO actorDAO,
                  LocationDAO locationDAO,
                  LocationDeskDAO locationDeskDAO,
                  SourceDAO sourceDAO,
                  SourceDeskDAO sourceDeskDAO,
                  DeskDAO deskDAO,
                  ActorDeskDAO actorDeskDAO,
                  SourceSourceListDAO sourceSourceListDAO,
                  EventHistoryDAO eventHistoryDAO,
                  SourceListDAO sourceListDAO) {
        this.actorDAO = actorDAO;
        this.actorDeskDAO = actorDeskDAO;
        this.locationDAO = locationDAO;
        this.sourceDAO = sourceDAO;
        this.sourceDeskDAO = sourceDeskDAO;
        this.deskDAO = deskDAO;
        this.sourceSourceListDAO = sourceSourceListDAO;
        this.sourceListDAO = sourceListDAO;
        this.locationDeskDAO = locationDeskDAO;
        this.eventHistoryDAO = eventHistoryDAO;

//        insertSourceRegions = (source) -> {
//            if(source.notNull(Source.REGION)) {
//                List<String> regionNames = Lists.newArrayList(source.get(Source.REGION).toString().split(";"));
//                List<Desk> regions  = new ArrayList<>(regionNames.stream()
//                        .map(r-> EntityVersions.get(Desk.class).current().put(Desk.DESK_NAME, r.trim()))
//                        .collect(Collectors.toSet()));
//
//                for(Desk region : regions) {
//
//                    try {
//                        region = deskDAO.create(region);
//                    } catch (DuplicateKeyException e) {
//                        region = deskDAO.getByUnique(Desk.DESK_NAME, region.get(Desk.DESK_NAME)).get();
//                    }
//
//                    SourceList sourceList = EntityVersions.get(SourceList.class).current()
//                            .put(SourceList.DESK_ID, region.id())
//                            .put(SourceList.LIST_NAME, region.get(Desk.DESK_NAME));
//
//                    try {
//                        sourceList = sourceListDAO.create(sourceList);
//                    } catch (DuplicateKeyException e) {
//                        sourceList = sourceListDAO.getByUnique(SourceList.LIST_NAME, sourceList.get(SourceList.LIST_NAME)).get();
//                    }
//
//                    sourceSourceListDAO.link(source, sourceList);
//                }
//            }
//
//            source = source.remove(Source.REGION);
//            return source;
//        };
    }


    public void json(String... args) throws Exception {
        String path = args[0];

        String clazz = args[1];

        switch (clazz) {
            case "Source": {

                String deskName = args[2];

                Desk d = EntityVersions.get(Desk.class).current().put(Desk.DESK_NAME, deskName);
                try {
                    d = deskDAO.create(d);
                } catch (DuplicateKeyException e) {
                    d = deskDAO.getByUnique(Desk.DESK_NAME, d.get(Desk.DESK_NAME)).get();
                }

                Desk desk = d;

                importEntitiesJSON(path, Source.class, sourceDAO, (source) -> {
                            if(source.get(Source.STANDARD_NAME) != null) {
                                source = source.put(Source.VERIFIED, true).put(Source.NAME, source.get(Source.STANDARD_NAME));
                            }
                            return source;
                        }
                    , (source) -> {

                        try {
                            sourceDeskDAO.link(source, desk);
                        } catch (DuplicateKeyException e) {
                            System.err.println("Link already exists "  + source);
                        }

                    return source;
                });
                break;
            }
            case "SourceList" : {

                String listName = args[2];
                String deskName = args[3];

                Desk desk = EntityVersions.get(Desk.class).current().put(Desk.DESK_NAME, deskName);
                try {
                    desk = deskDAO.create(desk);
                } catch (DuplicateKeyException e) {
                    desk = deskDAO.getByUnique(Desk.DESK_NAME, desk.get(Desk.DESK_NAME)).get();
                }

                SourceList sourceList = EntityVersions.get(SourceList.class).current()
                        .put(SourceList.DESK_ID, desk.id())
                        .put(SourceList.LIST_NAME, listName);

                try {
                    sourceList = sourceListDAO.create(sourceList);
                } catch (DuplicateKeyException e) {
                    sourceList = sourceListDAO.getByUnique(SourceList.LIST_NAME, sourceList.get(SourceList.LIST_NAME)).get();
                }

                linkEntities(path, Source.class, s -> sourceDAO.getByUnique(Source.STANDARD_NAME, s.get(Source.STANDARD_NAME)).orElseGet(()->{
                    System.err.println(s);
                    return null;
                }), sourceList,  sourceSourceListDAO);

                break;
            }
            case "Location": {

                String deskName = args[2];

                Desk d = EntityVersions.get(Desk.class).current().put(Desk.DESK_NAME, deskName);
                try {
                    d = deskDAO.create(d);
                } catch (DuplicateKeyException e) {
                    d = deskDAO.getByUnique(Desk.DESK_NAME, d.get(Desk.DESK_NAME)).get();
                }

                Desk desk = d;

                importEntitiesJSON(path, Location.class, locationDAO, (location) -> {
                    if(!location.hasValue(Location.LOCATION)) {
                        return null;
                    } else {
                        return location.put(Location.VERIFIED, true);
                    }
                }, (location -> {
                    try {
                        locationDeskDAO.link(location, desk);
                    } catch (DuplicateKeyException e) {
                        System.err.println(location);
                    }
                    return location;
                }));
                break;
            }
            case "Actor": {
                String deskName = args[2];

                Desk d = EntityVersions.get(Desk.class).current().put(Desk.DESK_NAME, deskName);
                try {
                    d = deskDAO.create(d);
                } catch (DuplicateKeyException e) {
                    d = deskDAO.getByUnique(Desk.DESK_NAME, d.get(Desk.DESK_NAME)).get();
                }

                Desk desk = d;

                importEntitiesJSON(path, Actor.class, actorDAO, (actro) -> actro.put(Actor.VERIFIED, true),
                    actor -> {
                        try {
                            actorDeskDAO.link(actor, desk);
                        } catch (DuplicateKeyException e) {
                            System.err.println(actor);
                        }
                        return actor;
                    }
                );
                break;
            }
        }
    }


    private <F extends VersionedEntity<F>, T extends VersionedEntity<T>>
    void linkEntities(String path, Class<F> fromKlass, Function<F,F> instanceGetter, T to, LinkDAO<F, T, ?> linkDAO) throws Exception {
        ObjectMapper om = AcledObjectMapper.get();
        try (
                BufferedReader reader = Files.newBufferedReader(Paths.get(path))
        ) {
            List<F> entities = om.readValue(reader, om.getTypeFactory()
                    .constructCollectionType(List.class, fromKlass));

            entities = entities.stream().map(instanceGetter).filter(e -> e != null && !e.isEmpty()).collect(Collectors.toList());

            System.out.println(entities);

            for(F from : entities) {

                try {
                    linkDAO.link(from, to);
                } catch (DuplicateKeyException e) {
                    System.err.println("Link already exists "  + from);
                }
            }
        }
    }


    public void importEntitiesCSV(String path) throws Exception {
        ObjectMapper om = AcledObjectMapper.get();

        try (
                Reader reader = Files.newBufferedReader(Paths.get(path));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)
                ;
        ) {

            List<String> headers = csvParser.getHeaderNames();
            for (CSVRecord csvRecord : csvParser) {

                // Accessing Values by Column Index
                String name = csvRecord.get(0);
                String email = csvRecord.get(1);
                String phone = csvRecord.get(2);
                String country = csvRecord.get(3);

                System.out.println("Record No - " + csvRecord.getRecordNumber());
                System.out.println("---------------");
                System.out.println("Name : " + name);
                System.out.println("Email : " + email);
                System.out.println("Phone : " + phone);
                System.out.println("Country : " + country);
                System.out.println("---------------\n\n");
            }
        }
    }


    private <T extends VersionedEntity<T>> List<T> importEntitiesJSON(String path, Class<T> klass, VersionedEntityDAO<T> dao, Function<T,T> preCreateMapper, Function<T,T> postCreateMapper) throws Exception {
        ObjectMapper om = AcledObjectMapper.get();

        try (
                BufferedReader reader = Files.newBufferedReader(Paths.get(path))
        ) {

            List<T> entities = om.readValue(reader, om.getTypeFactory()
                .constructCollectionType(List.class, klass));

            Set<String> uniques = EntityVersions.get(klass).current().spec().unique();
            Set<Set<Object>> seen = new HashSet<>();
            Set<T> remove = new HashSet<>();

            if(!uniques.isEmpty()) {
                for(T entity : entities) {
                    Set<Object> key = new HashSet<>();
                    for(String unique : uniques) {
                        key.add(entity.get(unique));
                    }
                    if(!seen.add(key)) {
//                    System.out.println(key);
                        remove.add(entity);
                    }
                }
            }


            entities = entities.stream()
                    .filter(e -> {
                        if(remove.contains(e)) {
                            System.err.println("---- DUPLICATE ENTRY ----" + e.toString());
                            return false;
                        } else {
                            return true;
                        }
                    })
                    .map(preCreateMapper)
                    .filter(Objects::nonNull)
                    .filter(e -> !e.isEmpty())
                    .collect(Collectors.toList());

//            System.out.println(entities);

            return dao.create(entities).stream().map(postCreateMapper).collect(Collectors.toList());
        }
    }

    private List<Integer> actorList(String names) {
        List<String> actorNames =  Arrays.asList(names.split(";"));

        List<Integer> actors = new ArrayList<>();
        for(String name : actorNames) {
            Optional<Actor> maybeActor = actorDAO.byName(name);
            if(maybeActor.isPresent()) {
                actors.add(maybeActor.get().id());
            }
        }

        return actors;
    }
    private Optional<Actor> actor(String name) {
        Optional<Actor> maybeActor = actorDAO.byName(name);
        return maybeActor;
    }

    private boolean equals(double d1, double d2){
        double d3 = d1-d2;
        return (Math.abs(d3) <= 0.00001);
    }

    private Optional<Location> location(Map<String,String> data) {

        List<Location> candidateLocations = locationDAO.byName(data.get("location"));
        if(candidateLocations.size() > 1) {
            String country = data.get("country");
            if(country != null) {

                candidateLocations = candidateLocations.stream().filter(l -> country.equals(l.get(Location.COUNTRY))).collect(Collectors.toList());
            }
            if(candidateLocations.size() == 1) {
                return Optional.of(candidateLocations.get(0));
            }
            double lat = Double.parseDouble(data.get("latitude"));
            double lon = Double.parseDouble(data.get("longitude"));
            candidateLocations = candidateLocations
                    .stream()
                    .filter(l->equals(l.get(Location.LATITUDE), lat) && equals(l.get(Location.LONGITUDE), lon))
                    .collect(Collectors.toList());

            if(candidateLocations.size() == 1) {
                return Optional.of(candidateLocations.get(0));
            } else {
                return Optional.empty();
            }

        } else if (candidateLocations.size() == 1) {
            return Optional.of(candidateLocations.get(0));
        } else {
            return Optional.empty();
        }

    }

    public final static BiMap<String,String> EVENT_TYPES = Maps.unmodifiableBiMap(HashBiMap.create(ImmutableMap
            .of("Battles", "BATTlE")
            .of("Explosions/Remote violence", "EXPLOSION_REMOTE_VIOLENCE")
            .of("Protests", "PROTEST")
            .of("Riots", "RIOT")
            .of("Strategic developments", "STRATEGIC_DEVELOPMENT")
            .of("Violence against civilians", "VIOLENCE_AGAINST_CIVILIANS")
    ));

    public final static BiMap<String,String> SUB_EVENT_TYPE = Maps.unmodifiableBiMap(HashBiMap.create(ImmutableMap
            .of("Abduction/forced disappearance", "ABDUCTION")
            .of("Agreement", "AGREEMENT")
            .of("Air/drone strike", "DRONE")
            .of("Riots", "RIOT")
            .of("Armed clash", "ARMED_CLASH")
            .of("Change to group/activity", "GRP_CHANGE")
            .of("Chemical weapon", "CHEM")
            .of("Disrupted weapons use", "WEAPON_DISRUPT")
            .of("Excessive force against protesters", "FORCE")
            .of("Government regains territory", "GOV_TER")
            .of("Grenade", "GRENADE")
            .of("Headquarters or base established", "HQ_BASE")
            .of("Looting/property destruction", "LOOTING")
            .of("Mob violence", "MOB")
            .of("Non-state actor overtakes territory", "NS_ACT_TER")
            .of("Non-violent transfer of territory", "NON_VIOLENT")
            .of("Other", "OTHER")
            .of("Peaceful protest", "PEACEFUL")
            .of("Protest with intervention", "INTERVENTION")
            .of("Remote explosive/landmine/IED", "IED")
            .of("Sexual violence", "SEXUAL_VIOLENCE")
            .of("Shelling/artillery/missile attack", "ARTILLERY")
            .of("Suicide bomb", "SUICIDE")
            .of("Violent demonstration", "VIOLENT")
            .of("Protest with intervention", "INTERVENTION")
    ));

    private Event event(Map<String,String> data) {

        Event event = EntityVersions.get(Event.class).current();

        LocalDate date = LocalDate.parse(data.get("event_date"));
        Integer apiID = Integer.parseInt(data.get("data_id"));
//        String iso = data.get("iso");
//        String iso3 = data.get("iso3");
        String eventIDCnty = data.get("event_id_cnty");
        Integer eventIDNoCnty = Integer.parseInt(data.get("event_id_no_cnty"));

        String timePrecision = data.get("time_precision");

        String type = EVENT_TYPES.get(data.get("event_type"));
        String subType = SUB_EVENT_TYPE.get(data.get("sub_event_type"));

        String geoPrecision = data.get("geo_precision");
        String sourceScale = data.get("source_scale");
        String notes = data.get("notes");
        Integer fatalities = Integer.parseInt(data.get("fatalities"));
        String fatalitiesPrecision = data.get("fatalities_precision");

        event = event
                .put(Event.API_ID, apiID)
                .put(Event.EVENT_DATE, date)
                .put(Event.EVENT_ID_NO_CNTY, eventIDNoCnty)
                .put(Event.EVENT_ID_CNTY, eventIDCnty)
                .put(Event.TIME_PRECISION, timePrecision)
//                .put(Event.EVENT_TYPE, type)
                .put(Event.SUB_EVENT_TYPE, subType)
                .put(Event.LOCATION_PRECISION, geoPrecision)
                .put(Event.NOTES, notes)
                .put(Event.FATALITIES, fatalities)
                .put(Event.FATALITIES_PRECISION, fatalitiesPrecision)
//                .put(Event.SOUCE_SCALE, sourceScale)
        ;

        Optional<Actor> actor1 = actor(data.get("actor1"));
        List<Integer> assocActor1 = actorList(data.get("assoc_actor1"));
        Optional<Actor> actor2 = actor(data.get("actor2"));
        List<Integer> assocActor2 = actorList(data.get("assoc_actor2"));

        Optional<Location> location = location(data);
        List<String> sources =  Arrays.asList(data.get("source").split(";"));

        if(actor1.isPresent()) {
            event = event.put(Event.ACTOR1, actor1.get().id());
        }

        if(!assocActor1.isEmpty()) {
            event = event.put(Event.ASSOC_ACTOR_1, assocActor1);
        }

        if(actor2.isPresent()) {
            event = event.put(Event.ACTOR2, actor2.get().id());
        }

        if(!assocActor2.isEmpty()) {
            event = event.put(Event.ASSOC_ACTOR_2, assocActor2);
        }

        if(location.isPresent()) {
            event = event.put(Event.EVENT_LOCATION, location.get().id());
        }

        if(!sources.isEmpty()) {
            event = event.put(Event.SOURCES, sources);
        }


        return event;
    }

    public Event event(String json) throws IOException {
        ObjectMapper om = AcledObjectMapper.get();
        JavaType type = om.getTypeFactory().constructMapType(Map.class, String.class, String.class);
        Map<String,String> data = om.readValue(json, type);

        Event event = event(data);

        return event;
    }

    public void importHistoricalEvents(Path jsonPath) throws IOException {
        ObjectMapper om = AcledObjectMapper.get();
        JavaType type = om.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
        Map<String,Object> data = om.readValue(jsonPath.toFile(), type);
        List<Map<String,String>> jsonEvents = (List<Map<String,String>>)data.get("data");
        for(Map<String,String> entry : jsonEvents) {
            Event event = event(entry);

            eventHistoryDAO.create(event);

            System.out.println(event);
        }
    }
}