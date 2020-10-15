package com.casm.acled.crawler.management;

import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CrawlArgsService {


    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private SourceListDAO sourceListDAO;

    public CrawlArgs get() {
        return new CrawlArgs(sourceDAO, sourceListDAO);
    }

    public CrawlArgs get(CrawlArgs.Raw raw) {
        CrawlArgs args = new CrawlArgs(sourceDAO, sourceListDAO);
        args.raw = raw;
        return args;
    }
}
