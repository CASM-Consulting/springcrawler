package com.casm.acled.crawler.management;

import com.casm.acled.crawler.ACLEDImporter;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;


@Service
public class CrawlService {

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private Reporter reporter;

    public void run(int sourceListId, int sourceId, LocalDate from, LocalDate to) {

        Optional<SourceList> maybesSourceList = sourceListDAO.getById(sourceListId);
        Optional<Source> maybeSource = sourceDAO.getById(sourceId);

        if(maybesSourceList.isPresent() && maybeSource.isPresent()) {

            ACLEDImporter importer = new ACLEDImporter(articleDAO, sourceDAO, sourceListDAO, true);

            Crawl crawl = new Crawl(maybesSourceList.get(), maybeSource.get(), from, to, importer);
            crawl.run();
        } else {

            throw new RuntimeException("source or source list not found!");
        }
    }
}
