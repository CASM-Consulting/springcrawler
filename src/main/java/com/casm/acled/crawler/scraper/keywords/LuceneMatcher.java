package com.casm.acled.crawler.scraper.keywords;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;

import java.io.IOException;

public class LuceneMatcher {

    private String queryConfig;
    private final Analyzer analyzer;
    private final Query query;
    private final String FIELD = "field";

    public LuceneMatcher(String queryConfig) {
        analyzer = new SimpleAnalyzer();

        QueryParser parser = new QueryParser(FIELD, analyzer);

        try {
            query = parser.parse(queryConfig);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
//
//        QueryTermExtractor
//        IndexSearcher searcher = new IndexSearcher(directory);
//        TopDocs topDocs = searcher.search(query, 10);
//        for (int i = 0; i < topDocs.totalHits; i++) {
//            ScoreDoc match = topDocs.scoreDocs[i];
//            Explanation explanation = searcher.explain(query, match.doc);
//            System.out.println("----------");
//            Document doc = searcher.doc(match.doc);
//            System.out.println(doc.get("title"));
//            System.out.println(explanation.toString());
//        }

    public boolean isMatched(String text) {

        MemoryIndex index = new MemoryIndex();
        index.addField(FIELD, text, analyzer);
        float score = index.search(query);


        return score > 0.0f;
    }

}
