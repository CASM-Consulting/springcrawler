package com.casm.acled.dao.jackson;

import com.casm.acled.entities.EntityField;
import com.casm.acled.entities.EntitySpecification;
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
import java.util.Iterator;

public class EntitySpecificationSerialiser {
    private static final Logger LOG = LoggerFactory.getLogger(EntitySpecificationSerialiser.class);

    private final StdDeserializer<EntitySpecification> deserializer;
    private final StdSerializer<EntitySpecification> serializer;


    public EntitySpecificationSerialiser(StdSerializer<EntitySpecification> serializer, StdDeserializer<EntitySpecification> deserializer) {
        this.deserializer = deserializer;
        this.serializer = serializer;
    }
    public EntitySpecificationSerialiser() {
        this(new StdSerializer<EntitySpecification>(EntitySpecification.class) {
            @Override
            public void serialize(EntitySpecification spec, JsonGenerator gen, SerializerProvider provider) throws IOException {
//                LOG.info("inside versionedentityserializer");
//                gen.writeStartObject();
                gen.writeStartArray();
//                LOG.info("spec.names = {}", spec.names());

                for( Object object : spec.fields()) {
                    EntityField field = (EntityField)object;
//                    gen.writeFieldName(field.getName());
//                    gen.writeStartObject();
                    gen.writeObject(field);
//                    assignValue(field, gen, provider);
//                    gen.writeEndObject();

                }
//                gen.writeEndObject();
                gen.writeEndArray();
            }
        }, new StdDeserializer<EntitySpecification>(EntitySpecification.class) {
            @Override
            public EntitySpecification deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

                ObjectCodec oc = p.getCodec();

                JsonNode node = p.getCodec().readTree(p);

//                Iterator<Map.Entry<String, JsonNode>> itr = node.fields();
                Iterator<JsonNode> itr = node.elements();
                EntitySpecification entity = new EntitySpecification();

                while( itr.hasNext() ) {
//                    Map.Entry<String, JsonNode> field = itr.next();
                    JsonNode field = itr.next();

//                    EntityField<?> entityField = oc.treeToValue(field.getValue(), EntityField.class);
                    EntityField<?> entityField = oc.treeToValue(field, EntityField.class);

                    entity.add(entityField);
                }

                return entity;
            }
        });
    }

    private static <T> void assignValue(EntityField<T> field, JsonGenerator gen, SerializerProvider provider) throws IOException {
//        LOG.info("i want to write {}", name);
        String name = field.getName();

        gen.writeStringField("name", name);
        gen.writeStringField("type", field.getKlass().toString());

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

    public StdDeserializer<EntitySpecification> deserializer() {
        return deserializer;
    }

    public StdSerializer<EntitySpecification> serializer() {
        return serializer;
    }
    public static void main(String[] args ) throws Exception {

        ObjectMapper om = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        EntitySpecificationSerialiser d = new EntitySpecificationSerialiser();
        module.addDeserializer(EntitySpecification.class, d.deserializer);
        module.addSerializer(EntitySpecification.class, d.serializer);
        om.registerModule(module);

        String json = "{\"ISO\":\"UK\", \"_version\":\"v1\"}";

        Event ge = om.readValue(json, Event.class);

        System.out.println((String)ge.get("ISO"));

        System.out.println(om.writeValueAsString(ge));

    }


}
