package com.norconex.collector.http.pipeline.importer;

import com.norconex.collector.http.doc.HttpDocument;
import com.norconex.collector.http.doc.HttpMetadata;

public class HttpImporterPipelineUtilProxy {

    public static void enhanceHTTPHeaders(HttpMetadata meta) {
        HttpImporterPipelineUtil.enhanceHTTPHeaders(meta);
    }

    public static void applyMetadataToDocument(HttpDocument doc) {
        HttpImporterPipelineUtil.applyMetadataToDocument(doc);
    }
}
