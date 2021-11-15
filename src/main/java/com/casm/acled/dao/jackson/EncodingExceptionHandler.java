package com.casm.acled.dao.jackson;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public interface EncodingExceptionHandler<T> {

    T handle(JsonNode node, ObjectCodec oc) throws IOException;
}
