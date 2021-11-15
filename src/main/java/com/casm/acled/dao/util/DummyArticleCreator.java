package com.casm.acled.dao.util;

import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.thedeanda.lorem.LoremIpsum;
//import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
//import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

//@EnableAutoConfiguration(exclude={CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class})
@Import(ObjectMapperConfiguration.class)
@ComponentScan(basePackages={"com.casm.acled.dao"})
public class DummyArticleCreator implements CommandLineRunner  {
    private static final Logger LOG = LoggerFactory.getLogger(DummyArticleCreator.class);
    private static final Article BASE_ARTICLE = EntityVersions.get(Article.class).current();
    private static final LoremIpsum LOREM_IPSUM = new LoremIpsum(0L);


    @Autowired private ArticleDAO articleDAO;

    @Override
    public void run(String... args) throws Exception {
        LOG.info("Starting.");
        LOG.info("DAO is {}", articleDAO);

        int paragraphs = 2;
        String articleText = LOREM_IPSUM.getParagraphs(paragraphs, paragraphs);

        Article theArticle = BASE_ARTICLE.put(Article.TEXT, articleText);

        Article updated  = articleDAO.create(theArticle);

        LOG.info("Created new Article: {}", updated);
        LOG.info("End.");
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DummyArticleCreator.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}
