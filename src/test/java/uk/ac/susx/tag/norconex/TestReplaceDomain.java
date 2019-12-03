package uk.ac.susx.tag.norconex;

import org.junit.Test;

public class TestReplaceDomain {

    @Test
    public void TestReplaceDomain() {
        String domain = "http://www.taglaboratory.org/";
        System.out.println(domain.replaceAll("\\.",""));
    }

}
