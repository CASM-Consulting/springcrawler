package com.casm.acled;

import com.casm.acled.dao.jackson.EntityFieldSerialiser;
import com.casm.acled.dao.jackson.EntitySpecificationSerialiser;
import com.casm.acled.dao.jackson.VersionedEntityLinkSerialiser;
import com.casm.acled.dao.jackson.VersionedEntitySerialiser;
import com.casm.acled.entities.*;
import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.actordesk.ActorDesk;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.articleevent.ArticleEvent;
import com.casm.acled.entities.change.Change;
import com.casm.acled.entities.crawlreport.CrawlReport;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.location.Location;
import com.casm.acled.entities.locationdesk.LocationDesk;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcedesk.SourceDesk;
import com.casm.acled.entities.sourcelist.SourceList;
import com.casm.acled.entities.sourcesourcelist.SourceSourceList;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

// This object mapper configuration works against the endpoints.
public class AcledObjectMapper {

    private static ObjectMapper OBJECT_MAPPER;

    public static SimpleModule getAcledModule() {
        SimpleModule module = new SimpleModule();
        addToModule(Actor.class, module);
        addToModule(Article.class, module);
        addToModule(Event.class, module);
        addToModule(Desk.class, module);
        addToModule(CrawlReport.class, module);
        addToModule(Source.class, module);
        addToModule(SourceList.class, module);
        addToModule(Location.class, module);
        addToModule(Change.class, module);

        addLinkToModule(SourceSourceList.class, module);
        addLinkToModule(SourceDesk.class, module);
        addLinkToModule(ActorDesk.class, module);
        addLinkToModule(LocationDesk.class, module);
        addLinkToModule(ArticleEvent.class, module);

        EntitySpecificationSerialiser entitySpecificationSerialiser = new EntitySpecificationSerialiser();
        module.addSerializer(EntitySpecification.class, entitySpecificationSerialiser.serializer());
        module.addDeserializer(EntitySpecification.class, entitySpecificationSerialiser.deserializer());

        EntityFieldSerialiser entityFieldSerialiser = new EntityFieldSerialiser();
        module.addSerializer(EntityField.class, entityFieldSerialiser.serializer());
        module.addDeserializer(EntityField.class, entityFieldSerialiser.deserializer());

//        module.addDeserializer(QuerySpecification.class, new QuerySpecificationDeserializer());

        return module;
    }

    public static ObjectMapper configure(ObjectMapper om) {
        SimpleModule module = getAcledModule();

        om = om.configure(MapperFeature.USE_STD_BEAN_NAMING, true)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(getJavaTimeModule())
                .registerModule(new Jdk8Module())
                .registerModule(module);
        return om;
    }

    synchronized public static ObjectMapper get() {
        if(OBJECT_MAPPER == null){

            OBJECT_MAPPER = configure(new ObjectMapper());
        }

        return OBJECT_MAPPER;
    }

    private static <V extends VersionedEntityLink<V>> void addLinkToModule(Class<V> baseClass, SimpleModule module) {
        VersionedEntityLinkSupplier<V> supplier = EntityVersions.getLink(baseClass);
        VersionedEntityLinkSerialiser<V> serDeser = new VersionedEntityLinkSerialiser<>(supplier);

        module.addSerializer(baseClass, serDeser.serializer());
        module.addDeserializer(baseClass, serDeser.deserializer());
    }

    private static <V extends VersionedEntity<V>> void addToModule(Class<V> baseClass, SimpleModule module) {
        VersionedEntitySupplier<V> supplier = EntityVersions.get(baseClass);
        VersionedEntitySerialiser<V> serDeser = new VersionedEntitySerialiser<>(supplier);

        module.addSerializer(baseClass, serDeser.serializer());
        module.addDeserializer(baseClass, serDeser.deserializer());
    }

    public static JavaTimeModule getJavaTimeModule() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();


        DateTimeFormatter dtf = new DateTimeFormatterBuilder()
                .appendPattern("[yyyy-MM-dd][dd-MM-yyyy][dd/MM/yyyy][dd/M/yyyy]")
                .toFormatter();

        javaTimeModule.addDeserializer(LocalDate.class, new CustomLocalDateDeserializer(dtf));

        return javaTimeModule;
    }


    private static class CustomLocalDateDeserializer extends LocalDateDeserializer {

        private static final LocalDateDeserializer DEFAULT = LocalDateDeserializer.INSTANCE;

        public CustomLocalDateDeserializer(DateTimeFormatter dtf) {
            super(dtf);
        }

        @Override
        public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            try {
                return DEFAULT.deserialize(parser, context);
            } catch (IOException e) {
                return super.deserialize(parser, context);
            }
        }
    }
}
