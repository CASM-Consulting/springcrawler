package com.casm.acled.crawler.scraper.keywords;


import com.google.common.collect.ImmutableList;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class KeywordFilterTest {


    @Test
    public void test1() throws Exception {
        KeywordFilter kwf = new KeywordFilter("text", ImmutableList.of("hello", "+world"));

        Assert.assertFalse(kwf.isMatched("no id doesn't"));
        Assert.assertTrue(kwf.isMatched("hello it does! world"));


    }

}