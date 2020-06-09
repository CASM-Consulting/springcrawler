package com.casm.acled.crawler.spring;

import com.casm.acled.crawler.Crawl;
import com.casm.acled.crawler.scraper.ACLEDImporter;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;


@Component
public class CrawlService {

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private Reporter reporter;

    public CrawlService() {

    }

    public void run(int sourceListId, int sourceId, boolean skipKeywords) {
        run(sourceListId, sourceId, null, null, skipKeywords);
    }


    public void collectExamples(int sourceListId, int sourceId) {

        Optional<SourceList> maybesSourceList = sourceListDAO.getById(sourceListId);
        Optional<Source> maybeSource = sourceDAO.getById(sourceId);

        System.out.println(sourceListId);
        System.out.println(sourceId);

        if(maybesSourceList.isPresent() && maybeSource.isPresent()) {

            ACLEDImporter importer = new ACLEDImporter(articleDAO, maybeSource.get(), sourceListDAO, true);
            importer.setMaxArticles(10);

            Crawl crawl = new Crawl(maybesSourceList.get(), maybeSource.get(), null, null, true, importer, reporter);
            crawl.getConfig().crawler().setMaxDepth(3);
//            crawl.getConfig().crawler().setIgnoreSitemap(false);
            crawl.run();
        } else {

            throw new RuntimeException("source or source list not found!");
        }
    }

    public void run(int sourceListId, int sourceId, LocalDate from, LocalDate to, boolean skipKeywords) {

        Optional<SourceList> maybesSourceList = sourceListDAO.getById(sourceListId);
        Optional<Source> maybeSource = sourceDAO.getById(sourceId);

        System.out.println(sourceListId);
        System.out.println(sourceId);

        if(maybesSourceList.isPresent() && maybeSource.isPresent()) {

            ACLEDImporter importer = new ACLEDImporter(articleDAO, maybeSource.get(), sourceListDAO, true);

            Crawl crawl = new Crawl(maybesSourceList.get(), maybeSource.get(), from, to, skipKeywords, importer, reporter);

            crawl.run();
        } else {

            throw new RuntimeException("source or source list not found!");
        }
    }
}
