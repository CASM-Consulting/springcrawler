package uk.ac.susx.tag.norconex;

import jdk.nashorn.internal.runtime.regexp.joni.Regex;
import org.junit.Test;

import java.util.regex.Pattern;

public class MatchQuery {

    @Test
    public void TestMatchQuery() {

        String regex = ".+(kill|massacre|death|died|dead|bomb|bombed|bombing|rebel|attack|attacked|riot|battle|protest|clash|demonstration|strike|wound|injure|casualty|displace|unrest|casualties|vigilante|torture|march|rape).+";
        String article = "There was a death table in myanmar.";
        System.out.println(Pattern.matches(regex,article));

    }
}
