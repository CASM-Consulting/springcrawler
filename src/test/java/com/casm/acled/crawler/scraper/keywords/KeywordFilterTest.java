package com.casm.acled.crawler.scraper.keywords;


import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

public class KeywordFilterTest {


    @Test
    public void test1() throws Exception {
        ExcludingKeywordFilter kwf = new ExcludingKeywordFilter("text", "hello +world");

        Assert.assertFalse(kwf.isMatched("no id doesn't"));
        Assert.assertTrue(kwf.isMatched("hello it does! world"));


    }

}