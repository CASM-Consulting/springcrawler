package com.casm.acled.crawler.management;

import com.casm.acled.crawler.Crawl;

import com.casm.acled.crawler.reporting.Reporter;

import com.casm.acled.crawler.util.Util;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.dao.entities.SourceSourceListDAO;

import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;

import org.jline.reader.LineReader;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;


@Service
public class UtilService {

    protected static final Logger logger = LoggerFactory.getLogger(UtilService.class);

    @Autowired
    private UtilService utilService;

    @Autowired
    private Reporter reporter;

    @Autowired
    LineReader reader;

    @Autowired
    private SourceDAO sourceDAO;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceSourceListDAO sourceSourceListDAO;

    @Autowired
    private ArticleDAO articleDAO;

    public String jsoupSearch(String url, String pattern) {
        org.jsoup.nodes.Document doc;
        try {
            doc = Jsoup.connect(url).get();
        }
        catch (IOException e) {
            return e.getMessage();
        }

        if (doc!=null) {
            Elements matched = doc.select(pattern);
            List<String> matchedText = matched.eachText();
            return String.join("\n", matchedText);

        }
        else {
            return String.format("doc is null");
        }
    }


    public String jef(CrawlArgs crawlArgs) {
        if (crawlArgs.source != null) {
            Source source = crawlArgs.source;
            Path outputPath = crawlArgs.path.resolve(Crawl.id(source)+"-jef.xml");

            generateDom(crawlArgs.workingDir, Arrays.asList(source), outputPath);

            return String.format("JEF configuration generated to %s successfully", outputPath.toString());

        } else if (!crawlArgs.sourceLists.isEmpty()) {
            SourceList sourceList = crawlArgs.sourceLists.get(0);
            String name = sourceList.get(SourceList.LIST_NAME);
            name = name.toLowerCase().replaceAll(" ", "-");
            List<Source> sources = sourceDAO.byList(sourceList);
            Path outputPath = crawlArgs.path.resolve(Util.getID(name)+"-jef.xml");
            generateDom(crawlArgs.workingDir, sources, outputPath);

            return String.format("JEF configuration generated to %s successfully", outputPath.toString());
        }
        else {
            return String.format("source or sourcelist should be provided");
        }
    }

    public void generateDom(Path dir, List<Source> sources, Path outputDir) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();

            Element root = doc.createElement("jefmon-config");
            doc.appendChild(root);

            Element instanceName = doc.createElement("instance-name");
            instanceName.appendChild(doc.createTextNode("ACLED"));
            root.appendChild(instanceName);

            Element interval = doc.createElement("default-refresh-interval");
            interval.appendChild(doc.createTextNode("5"));
            root.appendChild(interval);

            Element paths = doc.createElement("monitored-paths");

            for (Source source: sources) {
                Element path = doc.createElement("path");
                String id = Crawl.id(source);
                Path combinedPath = dir.resolve(Paths.get( id, "progress", "latest"));
                path.appendChild(doc.createTextNode(combinedPath.toString()));
                paths.appendChild(path);

            }

            root.appendChild(paths);

            Element jobActions = doc.createElement("job-actions");

            Element action1 = doc.createElement("action");
            action1.appendChild(doc.createTextNode("com.norconex.jefmon.instance.action.impl.ViewJobSuiteLogAction"));
            Element action2 = doc.createElement("action");
            action2.appendChild(doc.createTextNode("com.norconex.jefmon.instance.action.impl.ViewJobLogAction"));

            jobActions.appendChild(action1);
            jobActions.appendChild(action2);

            root.appendChild(jobActions);

            TransformerFactory transformerFactory =  TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            DOMSource source = new DOMSource(doc);

            StreamResult result =  new StreamResult(outputDir.toFile());
            transformer.transform(source, result);
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }


    }


}
