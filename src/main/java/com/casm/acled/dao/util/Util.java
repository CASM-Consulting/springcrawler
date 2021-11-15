package com.casm.acled.dao.util;


//import com.casm.acled.camunda.BusinessKeys;
import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;
//import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
//import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
//import org.camunda.bpm.spring.boot.starter.webapp.CamundaBpmWebappAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


// We need the special object mapper, though.
@Import(ObjectMapperConfiguration.class)
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao"})
public class Util implements CommandLineRunner {

    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;



//    public void recoverArticleDates() {
//        processArticleDates((article, date)->{
//            article = article.put(Article.DATE, date);
//
//            if(article.hasValue(Article.SOURCE_ID)) {
//
//                int sourceId = article.get(Article.SOURCE_ID);
//
//                List<SourceList> sourceLists = sourceListDAO.bySource(sourceId);
//
//                for(SourceList sourceList : sourceLists) {
//
//                    article = article.businessKey(BusinessKeys.generate(sourceList.get(SourceList.LIST_NAME), date));
//                }
//            }
//
//            articleDAO.overwrite(article);
//        }, (article) -> articleDAO.delete(article) );
//    }
    public void processArticleDates(BiConsumer<Article, LocalDate> hasDateAction, Consumer<Article> noDateAction) {

        for(Article article : articleDAO.getAll() ) {


            String[] text = ((String)article.get(Article.TEXT)).split("\n");

            if(text.length <= 1) {
                continue;
            }

            Optional<LocalDate> localDate = DateUtil.getDate(text[0]);
            if (localDate.isPresent()) {
                hasDateAction.accept(article, localDate.get() );
            } else {
                noDateAction.accept(article);
            }

        }
    }

    private static final String matchingKeywords = "\\b(?:kill|massacre|death|died|dead|bomb|bombed|bombing|rebel|attack|attacked|riot|battle|protest|clash|demonstration|strike|wound|injure|casualty|displace|unrest|casualties|vigilante|torture|march|rape)\\b";

    private void deleteNonMatchingArticles() {
        Pattern pattern = Pattern.compile(matchingKeywords);
        for (Article article : articleDAO.getAll()) {
            String text = article.get(Article.TEXT);

            if(!pattern.matcher(text).find()) {
                System.out.println("REMOVE " + text);
                articleDAO.delete(article);
            } else {

                //System.out.println("KEEP " + text);
            }
        }
    }

    private void linkExisting() {
        Map<String, Source> sources = sourceDAO.getAll().stream().filter(s->s.get(Source.LINK)!=null).collect(Collectors.toMap(s->s.get(Source.LINK), s -> s, (o, o2) -> o));

        for(Article article : articleDAO.getAll()) {
            String url = article.get(Article.URL);
            if(url == null) {
                continue;
            }
            for (Map.Entry<String, Source> e : sources.entrySet()) {

                if (url.contains(e.getKey())) {
                    int sourceId = e.getValue().id();
                    article = article.put(Article.SOURCE_ID, sourceId);

                    List<SourceList> sourceLists = sourceListDAO.bySource(sourceId);

                    LocalDate localDate = article.get(Article.DATE);

                    for(SourceList sourceList : sourceLists) {

//                        article = article.businessKey(BusinessKeys.generate(sourceList.get(SourceList.LIST_NAME), localDate));
                    }

                    articleDAO.overwrite(article);
//                    System.out.println("updating " + article.get(Article.URL));
                    break;
                }
            }

        }
    }

    private void articlesWithDates2CSV() throws IOException
    {
        List<Article> hasDates = new ArrayList<>();
        processArticleDates((a,d)->{
            if(d.isAfter(LocalDate.now().minusDays(10))) {
                a = a.put(Article.DATE, d);
                hasDates.add(a);
            }
        }, (a)->{});
        ExportCSV.standardEntityToFile(Paths.get("dated-articles.csv"), Article.class, articleDAO);
    }


    public void run(String... args) throws Exception {
//        deleteNonMatchingArticles();
//        recoverArticleDates();
//        linkExisting();

        articlesWithDates2CSV();

//        insertDummySource();
    }

    private void insertDummySource() {
        String link = "http://www.0.com:5000";
        Source source = EntityVersions.get(Source.class).current()
                .put(Source.LINK, link)
                .put(Source.NAME, "fake net")
                .put(Source.STANDARD_NAME, "fake net")
                .put(Source.COUNTRY, "United Kingdom")
                ;

        sourceDAO.create(ImmutableList.of(source));

    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Util.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}