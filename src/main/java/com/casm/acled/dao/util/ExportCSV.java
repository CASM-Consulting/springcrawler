package com.casm.acled.dao.util;

import com.casm.acled.dao.VersionedEntityDAO;
import com.casm.acled.dao.entities.*;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.VersionedEntity;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class ExportCSV {

    private final ArticleDAO articleDAO;
    private final ActorDAO actorDAO;
    private final ActorDeskDAO actorDeskDAO;
    private final LocationDAO locationDAO;
    private final LocationDeskDAO locationDeskDAO;
    private final SourceDAO sourceDAO;
    private final DeskDAO deskDAO;
    private final SourceListDAO sourceListDAO;
    private final SourceDeskDAO sourceDeskDAO;
    private final SourceSourceListDAO sourceSourceListDAO;


    @Autowired
    public ExportCSV(ArticleDAO articleDAO,
                     ActorDAO actorDAO,
                      LocationDAO locationDAO,
                      LocationDeskDAO locationDeskDAO,
                      SourceDAO sourceDAO,
                      SourceDeskDAO sourceDeskDAO,
                      DeskDAO deskDAO,
                      ActorDeskDAO actorDeskDAO,
                      SourceSourceListDAO sourceSourceListDAO,
                      SourceListDAO sourceListDAO) {
        this.articleDAO = articleDAO;
        this.actorDAO = actorDAO;
        this.actorDeskDAO = actorDeskDAO;
        this.locationDAO = locationDAO;
        this.sourceDAO = sourceDAO;
        this.sourceDeskDAO = sourceDeskDAO;
        this.deskDAO = deskDAO;
        this.sourceSourceListDAO = sourceSourceListDAO;
        this.sourceListDAO = sourceListDAO;
        this.locationDeskDAO = locationDeskDAO;
    }

    public static <T> void toStream(OutputStream outputStream, List<T> entities, List<String> headers,
                                    Function<T, List> getter) throws IOException {
        if (!entities.isEmpty()) {

            try (
                    final PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)), false);
                    final CSVPrinter csv = new CSVPrinter(writer, CSVFormat.EXCEL.withQuoteMode(QuoteMode.NON_NUMERIC));
            ) {
                csv.printRecord(headers);

                for (T e : entities) {
                    csv.printRecord( getter.apply(e) );
                }
            }
        }
    }

    public static <T> void toFile(Path file,  List<T> entities, List<String> headers,
                                   Function<T, List> getter) throws IOException {
        toStream(Files.newOutputStream(file, StandardOpenOption.CREATE_NEW), entities, headers, getter );
    }

    public static <V extends VersionedEntity<V>> void standardEntityToFile(Path file,
                                                                           Class<V> entityClass,
                                                                           VersionedEntityDAO<V> dao) throws IOException {
        List<V> articles = dao.getByBusinessKey("Levant-2020-14");

        V entity = EntityVersions.get(entityClass).current();

        List<String> headers = ImmutableList.copyOf(entity.spec().names());

        toFile(file, articles, headers, (V a) -> a.get(headers));
    }

    public static <V extends VersionedEntity<V>> void standardEntityToFile(Path file,
                                                                           Supplier<List<V>> getData,
                                                                           List<String> headers,
                                                                           Function<V,V> entityProcessor,
                                                                           Function<V, List> csvGetter
    ) throws IOException {

        List<V> entities = getData.get().stream().map(entityProcessor).collect(Collectors.toList());

        toFile(file, entities, headers, csvGetter);
    }


    public void articles(Path path, Supplier<List<Article>> supplier) throws IOException {

        Article article = EntityVersions.get(Article.class).current();

        List<String> articleHeaders = ImmutableList.copyOf(article.spec().names());

        List<String> headers = ImmutableList.<String>builder()
                .addAll(articleHeaders)
                .build();


        standardEntityToFile(path,
                supplier,
                headers,
                Function.identity(),
                (Article a) -> ImmutableList.<String>builder()
                        .addAll((List)a.get(articleHeaders).stream().map(e->e==null?"":e).collect(Collectors.toList())).build()
        );
    }

    public void articlesWithSource(Path path, Supplier<List<Article>> supplier) throws IOException {

//        Path file = Paths.get("balkans-KoSSev-2020-05-03-2020-05-09.csv");

        Source source = EntityVersions.get(Source.class).current();
        Article article = EntityVersions.get(Article.class).current();

        List<String> articleHeaders = ImmutableList.copyOf(article.spec().names());
        List<String> sourceHeaders = ImmutableList.copyOf(source.spec().names());

        List<String> headers = ImmutableList.<String>builder()
                .addAll(articleHeaders)
                .addAll(sourceHeaders)
                .build();


        Function<Article,Article>  postProcessor = (a) -> {
            Source s = sourceDAO.getById(a.get(Article.SOURCE_ID)).get();
            a = a.source(s);
            return a;
        };

        standardEntityToFile(path,
//                () -> articleDAO.bySource(sourceDAO.getById(2254).get()),
                supplier,
                headers,
                postProcessor,
                (Article a) -> ImmutableList.<String>builder()
                        .addAll((List)a.get(articleHeaders).stream().map(e->e==null?"":e).collect(Collectors.toList()))
                        .addAll((List)a.source().get(sourceHeaders).stream().map(e->e==null?"":e).collect(Collectors.toList())).build()
        );
    }


    private void sources() throws Exception {
        Path file = Paths.get("levant-sources1.csv");

        Source source = EntityVersions.get(Source.class).current();

        List<String> sourceHeaders = ImmutableList.copyOf(source.spec().names());

        List<String> headers = ImmutableList.<String>builder()
                .add("id")
                .addAll(sourceHeaders)
                .build();

        Function<Source,Source> postProcessor = (a) -> a;

        standardEntityToFile(file,
                () -> sourceDAO.byList(sourceListDAO.getBy(SourceList.LIST_NAME, "Levant").get(0))  ,
                headers,
                postProcessor,
                (Source s) -> ImmutableList.<String>builder()
                        .add( Integer.toString(s.id()) )
                        .addAll((List)s.get(sourceHeaders).stream().map(e->e==null?"":e).collect(Collectors.toList()))
                        .build()
        );
    }
}
