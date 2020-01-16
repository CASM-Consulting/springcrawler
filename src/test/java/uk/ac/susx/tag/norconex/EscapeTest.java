package uk.ac.susx.tag.norconex;

import org.apache.commons.lang.StringEscapeUtils;
import org.junit.Test;

public class EscapeTest {

    @Test
    public void testEscape(){
        String domain = "tasnimnews.com";
        System.out.println(StringEscapeUtils.unescapeJava("."));
        System.out.println(domain.replaceAll("\\.",""));
    }


}
