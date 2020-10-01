package com.casm.acled.crawler.management;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class PathService {


    public Path workingDir() {
        return Paths.get("test");
    }

    public Path scraperDir() {
        return Paths.get("/home/sw206/git/alced-scrapers");
    }
}
