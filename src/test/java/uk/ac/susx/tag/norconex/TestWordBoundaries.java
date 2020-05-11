package uk.ac.susx.tag.norconex;

import com.casm.acled.crawler.util.Util;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Pattern;

public class TestWordBoundaries {


    @Test
    public void TestBoundaries() {

        String shouldMatch = "A bomb has exploded outside of quatar. All were killed";
        String shoudNotMatch = "a nonbomb has pexploded outside of quatar. All were notkilled.";
        Assert.assertTrue(Pattern.matches(Util.KEYWORDS,shouldMatch));
        Assert.assertFalse(Pattern.matches(Util.KEYWORDS,shoudNotMatch));

        String shouldDemoMatch = "Protestors performed a demonstration outside parliament this morning.";
        String shouldNotDemoMatch = "Protestorsires redemonstration outside parliament this morning.";
        Assert.assertTrue(Pattern.matches(Util.KEYWORDS,shouldDemoMatch));
        Assert.assertFalse(Pattern.matches(Util.KEYWORDS,shouldNotDemoMatch));

    }

}
