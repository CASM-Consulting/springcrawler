package com.casm.acled.crawler.springrunners;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Strings;
import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.management.CheckListService;
import com.casm.acled.crawler.management.CrawlArgs;
import com.casm.acled.crawler.management.CrawlArgsService;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import net.sf.extjwnl.data.Exc;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import org.springframework.core.MethodParameter;
import org.springframework.shell.*;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellOption;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import javax.validation.Valid;

@EnableAutoConfiguration(exclude={HibernateJpaAutoConfiguration.class, CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class, ValidationAutoConfiguration.class})
// We need the special object mapper, though.
//@Import({ObjectMapperConfiguration.class, CLIRunner.ShutdownConfig.class})
@Import({ObjectMapperConfiguration.class})
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao", "com.casm.acled.crawler"})
@ShellComponent
public class ShellRunner {

    protected static final Logger logger = LoggerFactory.getLogger(ShellRunner.class);

    @Autowired
    private CheckListService checkListService;

    @Autowired
    private Reporter reporter;

    @Autowired
    private CrawlArgsService argsService;

    private CrawlArgs crawlArgs;

    @ShellMethod(value = "run to check source list ", key = "check")
    // probably should give a hint of potential parameters;
    // the help command still not working:
    // Action: Correct the classpath of your application so that it contains a single, compatible version of com.beust.jcommander.JCommander
    public void checkSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) {
        reporter.randomRunId();

        crawlArgs = argsService.get();
        crawlArgs.raw = args;

        crawlArgs.raw.program = "check";

        crawlArgs.init();

        checkListService.checkSourceList(crawlArgs);

//        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));

    }

    @ShellMethod(value = "run to import source list", key = "import")
    public void importSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{
        reporter.randomRunId();

        crawlArgs = argsService.get();
        crawlArgs.raw = args;

        crawlArgs.raw.program = "import";

        crawlArgs.init();

        checkListService.importCrawlerSourceList(crawlArgs);

        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));

    }

    @ShellMethod(value = "run to export source list", key = "export")
    public void exportSourceList(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{
        reporter.randomRunId();

        crawlArgs = argsService.get();
        crawlArgs.raw = args;

        crawlArgs.raw.program = "export";

        crawlArgs.init();

        checkListService.exportCrawlerSourceList(crawlArgs);

        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));

    }

    @ShellMethod(value = "run to output example url ", key = "output")
    public void outputExampleURLCheck(@ShellOption(optOut = true) @Valid CrawlArgs.Raw args) throws Exception{
        reporter.randomRunId();

        crawlArgs = argsService.get();
        crawlArgs.raw = args;

        crawlArgs.raw.program = "example-urls";

        crawlArgs.init();

        checkListService.outputExampleURLCheck(crawlArgs);

        reporter.getRunReports().stream().forEach(r -> logger.info(r.toString()));

    }

    // generic set / get commands for sources and source lists, in the form
    // generic, only handle single instance
    @ShellMethod(value = "get specific value from the corresponding field; usage: get type id field", key = "get")
    public String getField(@ShellOption({"-t", "--type"}) String type,
                         @ShellOption({"-i", "--id"}) String id,
                         @ShellOption({"-f", "--field"}) String field) {
        crawlArgs = argsService.get();

        if (type.equals("source")) {
            Optional<Source> maybeSource = crawlArgs.getSourceDAO().byName(id);
            if (maybeSource.isPresent()) {
                Source source = maybeSource.get();
                String value = source.get(field);
                return String.format(value);
            }
            else {
                return String.format("source name does not exist");

            }
        }
        else if (type.equals("sourcelist")){
            Optional<SourceList> maybeSourceList = crawlArgs.getSourceListDAO().byName(id);
            if(maybeSourceList.isPresent()) {
                SourceList sourceList =  maybeSourceList.get();
                String value = sourceList.get(field);
                return String.format(value);
            }
            else {
                return String.format("source list name does not exist");
            }
        }
        else {
            return String.format("wrong type value, should be source or sourcelist");
        }

    }

    @ShellMethod(value = "set specific value to the corresponding field; usage: set type id field [value]", key = "set")
    // generic, only handle single instance
    // in the set method, probably need to update DAO???
    public String setField(@ShellOption({"-t", "--type"}) String type,
                         @ShellOption({"-i", "--id"}) String id,
                         @ShellOption({"-f", "--field"}) String field,
                         @ShellOption({"-v", "--value"}) String value) {

            crawlArgs = argsService.get();

        if (type.equals("source")) {
            Optional<Source> maybeSource = crawlArgs.getSourceDAO().byName(id);
            if(maybeSource.isPresent()) {
                Source source =  maybeSource.get();
                source = source.put(field, value);
                crawlArgs.getSourceDAO().upsert(source);

                return String.format("value set successfully");
            }
            else {
                return String.format("source name does not exist");

            }
        }
        else if (type.equals("sourcelist")){
            Optional<SourceList> maybeSourceList = crawlArgs.getSourceListDAO().byName(id);
            if(maybeSourceList.isPresent()) {
                SourceList sourceList =  maybeSourceList.get();
                sourceList = sourceList.put(field, value);
                crawlArgs.getSourceListDAO().upsert(sourceList);

                return String.format("value set successfully");

            }
            else {
                return String.format("source list name does not exist");
            }
        }
        else {
            return String.format("wrong type value, should be source or sourcelist");
        }
    }

    @Bean
    public ParameterResolver commandParameterResolver() {
        return new ParameterResolver(){

            @Override
            public boolean supports(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(List.class);
            }

            /**
             * This implementation simply returns all the words (arguments) present
             * 'Infinite arity'
             */
            @Override
            public ValueResult resolve(MethodParameter methodParameter, List<String> words) {
                return new ValueResult(methodParameter, words);
            }

            @Override
            public Stream<ParameterDescription> describe(MethodParameter parameter) {
                return Stream.of(ParameterDescription.outOf(parameter));
            }

            @Override
            public List<CompletionProposal> complete(MethodParameter parameter, CompletionContext context) {
                return Collections.emptyList();
            }
        };
    }



    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(ShellRunner.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        logger.info("Spring Boot application started");
        ctx.close();
    }



}
