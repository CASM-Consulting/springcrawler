package com.casm.acled.entities.validation;

import com.casm.acled.camunda.variables.Entities;
import com.casm.acled.entities.VersionedEntity;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Validation {

    public static <V extends VersionedEntity<V>> Response validate(List<V> entities, V baseEntity) {

        List<Map<String, List<ValidationMessage>>> batchMsg = new ArrayList<>();

        EntityValidator<V> entityValidator = baseEntity.getValidators();
        List<List<ValidationMessage>> entityMessages = entityValidator.validate(entities);


        for(int i = 0; i < entities.size(); ++i) {
            V entity = entities.get(i);
            Map<String, List<ValidationMessage>> entityMsg = new HashMap<>();

            for(String fieldName : entity.spec().names()) {

                //long method call to allow field generics to line up
                List<ValidationMessage> fieldMsg = entity.spec().get(fieldName).getValidators().validate(entity, entity.spec().get(fieldName), entity.get(fieldName));

                entityMsg.put(fieldName, fieldMsg);
            }

            if(entityMessages.size() == entities.size()){
                entityMsg.put(Entities.ENTITY, entityMessages.get(i));
            }

            batchMsg.add(entityMsg);
        }

        return Response.ok(batchMsg).build();
    }
}
