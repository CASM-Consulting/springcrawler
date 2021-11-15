package com.casm.acled.dao.jackson;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntityLink;
import com.casm.acled.entities.VersionedEntityLinkSupplier;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import static com.casm.acled.entities.VersionedEntity.ID;
import static com.casm.acled.entities.VersionedEntity.VERSION;
import static com.casm.acled.entities.VersionedEntityLink.ID1;
import static com.casm.acled.entities.VersionedEntityLink.ID2;

public class VersionedEntityLinkSerialiser<V extends VersionedEntityLink<V>> extends VersionedEntitySerialiser<V> {
    private static final Logger LOG = LoggerFactory.getLogger(VersionedEntityLinkSerialiser.class);


    private VersionedEntityLinkSerialiser(VersionedEntityLinkSupplier<V> versionedEntitySupplier, StdSerializer<V> serializer, StdDeserializer<V> deserializer) {
        super(versionedEntitySupplier, serializer, deserializer);
    }

    public VersionedEntityLinkSerialiser(VersionedEntityLinkSupplier<V> entitySupplier) {
        this(entitySupplier, new StdSerializer<V>(entitySupplier.getBaseClass()) {
            @Override
            public void serialize(V value, JsonGenerator gen, SerializerProvider provider) throws IOException {
//                LOG.info("inside versionedentityserializer");
                EntitySpecification spec = value.spec();
                gen.writeStartObject();
                gen.writeStringField(VERSION, value.version());

                if(value.id1()!=null) {
                    gen.writeNumberField(ID1, value.id1());
                }
                if(value.id2()!=null) {
                    gen.writeNumberField(ID2, value.id2());
                }
                if(value.hasId()) {
                    gen.writeNumberField(ID, value.id());
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
                if(node.hasNonNull(VERSION)) {
                    version = node.get(VERSION).asText();
                }
                V entity = entitySupplier.get(version);

                int id1;
                int id2;
                if(node.hasNonNull(ID1)) {
                    id1 = node.get(ID1).asInt();
                    entity = entity.id1(id1);
                }

                if(node.hasNonNull(ID2)) {
                    id2 = node.get(ID2).asInt();
                    entity = entity.id2(id2);
                }

                while( itr.hasNext() ) {
                    Map.Entry<String, JsonNode> field = itr.next();
                    String name = field.getKey();
                    if(any(name, VERSION, ID, ID1, ID2)) {
                        continue;
                    }

                    try {

                        entity = assignValue(entity, field.getKey(), field.getValue(), oc);
                    } catch (NullPointerException | InvalidFormatException e) {
                        String id = entity.hasId() ? Integer.toString(entity.id()) : "";
                        String type = entity.getClass().getName();
                        LOG.error(type + " - " + id + " - " + field.getKey()+ " - " +field.getValue() + " - " + e.getMessage(), e);
                    }
                }

                return entity;
            }
        });
    }

    private static boolean any(String is, String... any) {
        for(String one : any) {
            if(is.equals(one)) {
                return true;
            }
        }
        return false;
    }


}
