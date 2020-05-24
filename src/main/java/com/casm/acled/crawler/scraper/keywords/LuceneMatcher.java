package com.casm.acled.crawler.scraper.keywords;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CompositeReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LuceneMatcher {

    public class Match {
        List<String> terms = new ArrayList<>();
        List<int[]> offsets = new ArrayList<>();
    }

    private String queryConfig;
    private final Analyzer analyzer;
    private final Query query;
    private final String FIELD = "field";

    public LuceneMatcher(String queryConfig) {
        analyzer = new SimpleAnalyzer();

        QueryParser parser = new QueryParser(FIELD, analyzer);

        parser.createPhraseQuery(FIELD, queryConfig);
        try {
            query = parser.parse(queryConfig);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getMatches(String text) {

        MemoryIndex index = new MemoryIndex();
        index.addField(FIELD, text, analyzer);
        float score = index.search(query);

        try {
//            WeightedSpanTermExtractor extractor =  new WeightedSpanTermExtractor(FIELD);
//            Map<String,WeightedSpanTerm> terms = extractor.getWeightedSpanTerms(query, 0, analyzer.tokenStream(FIELD, text));
//            for(WeightedSpanTerm term : terms.values()) {
//                if (term.getWeight() > 0) {
//                    System.out.println(term.getTerm());
//                }
//            }


        List<String> matched = new ArrayList<>();
        WeightedTerm[] terms = QueryTermExtractor.getTerms(query);
        for(WeightedTerm term : terms) {
            if (term.getWeight() > 0) {
                matched.add(term.getTerm());
            }
        }

        IndexSearcher searcher = index.createSearcher();

        //Search the lucene documents
        TopDocs hits = searcher.search(query, 10);

        /** Highlighter Code Start ****/

        //Uses HTML &lt;B&gt;&lt;/B&gt; tag to highlight the searched terms
        Formatter formatter = new SimpleHTMLFormatter();

        //It scores text fragments by the number of unique query terms found
        //Basically the matching score in layman terms
        QueryScorer scorer = new QueryScorer(query);

        //used to markup highlighted terms found in the best sections of a text
        Highlighter highlighter = new Highlighter(formatter, scorer);

        //It breaks text up into same-size texts but does not split up spans
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 100);

        //breaks text up into same-size fragments with no concerns over spotting sentence boundaries.
        //Fragmenter fragmenter = new SimpleFragmenter(10);

        //set fragmenter to highlighter
        highlighter.setTextFragmenter(fragmenter);

        //Iterate over found results
        for (int i = 0; i < hits.scoreDocs.length; i++)
        {

            //Create token stream
            TokenStream stream = analyzer.tokenStream(FIELD, text);

            //Get highlighted text fragments
            String[] frags = highlighter.getBestFragments(stream, text, 10);
            for (String frag : frags)
            {
                System.out.println("=======================");
                System.out.println(frag);
            }
        }

        return matched;
    } catch (IOException | InvalidTokenOffsetsException e) {
        throw new RuntimeException(e);
    }
    }

    public boolean isMatched(String text) {

        MemoryIndex index = new MemoryIndex();
        index.addField(FIELD, text, analyzer);
        float score = index.search(query);


        return score > 0.0f;
    }
}
