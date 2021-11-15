package com.casm.acled.dao.jackson;

import com.casm.acled.entities.EntityField;
import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.VersionedEntitySupplier;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.event.EventVersions;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class VersionedEntitySerialiser<V extends VersionedEntity<V>> {
    private static final Logger LOG = LoggerFactory.getLogger(VersionedEntitySerialiser.class);
    private final VersionedEntitySupplier<V> versionedEntitySupplier;

    private final StdDeserializer<? extends V> deserializer;
    private final StdSerializer<V> serializer;


    protected VersionedEntitySerialiser(VersionedEntitySupplier<V> versionedEntitySupplier, StdSerializer<V> serializer, StdDeserializer<V> deserializer) {
        this.versionedEntitySupplier = versionedEntitySupplier;
        this.deserializer = deserializer;
        this.serializer = serializer;
    }

    public VersionedEntitySerialiser(VersionedEntitySupplier<V> entitySupplier) {
        this(entitySupplier, new StdSerializer<V>(entitySupplier.getBaseClass()) {
            @Override
            public void serialize(V value, JsonGenerator gen, SerializerProvider provider) throws IOException {
//                LOG.info("inside versionedentityserializer");
                EntitySpecification spec = value.spec();
                gen.writeStartObject();
                gen.writeStringField("_version", value.version());

                if(value.hasId()) {
                    gen.writeNumberField("_id", value.id());
                }

//                LOG.info("spec.names = {}", spec.names());

                for(String name : spec.names()) {
                    assignValue(value, name, gen, provider);
                }
                gen.writeEndObject();
            }
        }, new StdDeserializer<V>(entitySupplier.getBaseClass()) {
            @Override
            public V deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

                ObjectCodec oc = p.getCodec();

                JsonNode node = p.getCodec().readTree(p);

                Iterator<Map.Entry<String, JsonNode>> itr = node.fields();

                String version = entitySupplier.currentVersion();
                if(node.hasNonNull("_version")) {
                    version = node.get("_version").asText();
                }
                V entity = entitySupplier.get(version);

                if(node.hasNonNull("_id")) {
                    entity = entity.id(node.get("_id").asInt());
                }

                while( itr.hasNext() ) {
                    Map.Entry<String, JsonNode> field = itr.next();
                    String name = field.getKey();
                    if(name.equals("_version")) {
                        continue;
                    }
                    try {

                        entity = assignValue(entity, field.getKey(), field.getValue(), oc);
                    } catch (NullPointerException | JsonProcessingException | IllegalStateException e) {
                        String id = entity.hasId() ? Integer.toString(entity.id()) : "";
                        String type = entity.getClass().getName();
                        LOG.error(type + " - " + id + " - " + field.getKey()+ " - " +field.getValue() + " - " + e.getMessage(), e);
                    }
                }

                return entity;
            }
        });
    }

    /**
     * Invention for subclasses to add more custom sersialisation stuff.
     * @param value
     * @param gen
     * @param provider
     * @throws IOException
     */
    protected void serialize(V value, JsonGenerator gen, SerializerProvider provider) throws IOException {

    }

    /**
     * Invention for subclasses to add more custom desersialisation stuff.
     * @param value
     * @param p
     * @param ctxt
     * @return
     * @throws IOException
     */
    public V deserialize(V value, JsonParser p, DeserializationContext ctxt) throws IOException {
        return value;
    }

    protected static <T, V  extends VersionedEntity<V>> void assignValue(VersionedEntity<V> entity, String name, JsonGenerator gen, SerializerProvider provider) throws IOException {
//        LOG.info("i want to write {}", name);
        T value = entity.get(name);
        if(value == null) {
            return;
        }
        EntityField<T> field = entity.spec().get(name);
        gen.writeFieldName(name);
        try {
            provider.findTypedValueSerializer(field.getKlass(), true, null).serialize(entity.get(name), gen, provider);
        } catch (IllegalStateException|ClassCastException|IOException e){
            LOG.warn("{} {}", name, e.getMessage());
            throw e;
        }
    }

    protected static <T, V  extends VersionedEntity<V>> V assignValue(V entity, String name, JsonNode node, ObjectCodec oc) throws JsonProcessingException {
        if(entity.spec().has(name)) {
            EntityField<T> field = entity.spec().get(name);
            T value;
            try {

                 value = oc.treeToValue(node, field.getKlass());
            } catch (JsonProcessingException e ) {
                try {

                    EncodingExceptionHandler<T> handler = field.encodingExceptionHandler();
                    if(handler != null) {
                        value = handler.handle(node, oc);
                    } else {
                        throw e;
                    }
                } catch (IOException ee) {
                    throw new RuntimeException(ee);
                }
            }
            if( value != null ){
                entity = entity.put(field.getName(), value);
            }
        }
        return entity;
    }

    public StdDeserializer<? extends V> deserializer() {
        return deserializer;
    }

    public StdSerializer<V> serializer() {
        return serializer;
    }
    public static void main(String[] args ) throws Exception {

        ObjectMapper om = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        VersionedEntitySerialiser<Event> d = new VersionedEntitySerialiser<>(new EventVersions());
        module.addDeserializer(Event.class, d.deserializer);
        module.addSerializer(Event.class, d.serializer);
        om.registerModule(module);

        String json = "{\"ISO\":\"UK\", \"_version\":\"v1\"}";

        Event ge = om.readValue(json, Event.class);

        System.out.println((String)ge.get("ISO"));

        System.out.println(om.writeValueAsString(ge));

    }


}
