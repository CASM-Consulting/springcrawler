package uk.ac.susx.tag.norconex;

import com.casm.acled.crawler.utils.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Pattern;

public class TestWordBoundaries {


    @Test
    public void TestBoundaries() {

        String shouldMatch = "A bomb has exploded outside of quatar. All were killed";
        String shoudNotMatch = "a nonbomb has pexploded outside of quatar. All were notkilled.";
        Assert.assertTrue(Pattern.matches(Utils.KEYWORDS,shouldMatch));
        Assert.assertFalse(Pattern.matches(Utils.KEYWORDS,shoudNotMatch));

        String shouldDemoMatch = "Protestors performed a demonstration outside parliament this morning.";
        String shouldNotDemoMatch = "Protestorsires redemonstration outside parliament this morning.";
        Assert.assertTrue(Pattern.matches(Utils.KEYWORDS,shouldDemoMatch));
        Assert.assertFalse(Pattern.matches(Utils.KEYWORDS,shouldNotDemoMatch));

    }

}
