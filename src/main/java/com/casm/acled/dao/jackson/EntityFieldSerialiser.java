package com.casm.acled.dao.jackson;

import com.casm.acled.entities.EntityField;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.event.Event;
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

public class EntityFieldSerialiser {
    private static final Logger LOG = LoggerFactory.getLogger(EntitySpecificationSerialiser.class);

    private final StdDeserializer<EntityField> deserializer;
    private final StdSerializer<EntityField> serializer;


    public EntityFieldSerialiser(StdSerializer<EntityField> serializer, StdDeserializer<EntityField> deserializer) {
        this.deserializer = deserializer;
        this.serializer = serializer;
    }
    public EntityFieldSerialiser() {
        this(new StdSerializer<EntityField>(EntityField.class) {
            @Override
            public void serialize(EntityField field, JsonGenerator gen, SerializerProvider provider) throws IOException {
//                LOG.info("inside versionedentityserializer");
                gen.writeStartObject();

                gen.writeStringField("name", field.getName());
                gen.writeStringField("label", field.getLabel());
                gen.writeStringField("type", field.getKlass().getName());
                gen.writeStringField("displayType", field.getDisplayType());
                gen.writeBooleanField("required", field.isRequired());
                gen.writeObjectField("meta", field.getMeta());
                gen.writeObjectField("validators", field.getValidators().getFrontend());


                gen.writeEndObject();
            }
        }, new StdDeserializer<EntityField>(EntityField.class) {
            //Pretty much always a one way trip.
            @Override
            public EntityField deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

                JsonNode node = p.getCodec().readTree(p);
                Class<?> klass;

                try {
                    klass = Class.forName(node.get("type").asText());

                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                EntityField<?> entityField = new EntityField<>(node.get("name").asText(), klass);

                return entityField;
            }
        });
    }

    private static <T> void assignValue(EntityField<T> field, JsonGenerator gen, SerializerProvider provider) throws IOException {
//        LOG.info("i want to write {}", name);

        gen.writeStringField("name", field.getName());
        gen.writeStringField("label", field.getLabel());
        gen.writeStringField("type", field.getKlass().toString());
        gen.writeStringField("displayType", field.getDisplayType());
        gen.writeBooleanField("required", field.isRequired());
        gen.writeObjectField("meta", field.getMeta());

    }

    private static <T, V  extends VersionedEntity<V>> V assignValue(V entity, String name, JsonNode node, ObjectCodec oc) throws JsonProcessingException {
        if(entity.spec().has(name)) {
            EntityField<T> field = entity.spec().get(name);
            T value = oc.treeToValue(node, field.getKlass());
            if( value != null ){
                entity = entity.put(field.getName(), value);
            }
        }
        return entity;
    }

    public StdDeserializer<EntityField> deserializer() {
        return deserializer;
    }

    public StdSerializer<EntityField> serializer() {
        return serializer;
    }
    public static void main(String[] args ) throws Exception {

        ObjectMapper om = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        EntityFieldSerialiser d = new EntityFieldSerialiser();
        module.addDeserializer(EntityField.class, d.deserializer);
        module.addSerializer(EntityField.class, d.serializer);
        om.registerModule(module);

        String json = "{\"ISO\":\"UK\", \"_version\":\"v1\"}";

        Event ge = om.readValue(json, Event.class);

        System.out.println((String)ge.get("ISO"));

        System.out.println(om.writeValueAsString(ge));

    }


}
