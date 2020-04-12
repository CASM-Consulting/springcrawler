package com.casm.acled.crawler.scraper.keywords;

import com.casm.acled.crawler.management.Reporting;
import com.casm.acled.crawler.scraper.dates.LocaleService;
import com.casm.acled.dao.entities.DeskDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.dao.entities.SourceSourceListDAO;
import com.casm.acled.entities.region.Desk;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.casm.acled.entities.sourcesourcelist.SourceSourceList;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.IndexWordSet;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.Word;
import net.sf.extjwnl.dictionary.Dictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KeywordsService {

    protected static final Logger logger = LoggerFactory.getLogger(KeywordsService.class);

    @Autowired
    private DeskDAO deskDAO;
    @Autowired
    private SourceSourceListDAO sourceSourceListDAO;
    @Autowired
    private SourceListDAO sourceListDAO;
    @Autowired
    private SourceDAO sourceDAO;

    public void determineKeywordsList() {


        for(Desk desk : deskDAO.getAll()) {

            List<SourceList> lists = sourceListDAO.byDesk(desk.id());

            for(SourceList list : lists) {

                List<Source> sources = sourceDAO.byList(list);

                for(Source source : sources) {

                    Optional<SourceSourceList> maybeLink = sourceSourceListDAO.get(source, list);

                    if(maybeLink.isPresent()) {
                        SourceSourceList link = maybeLink.get();
                        determineKeywords(list, link, list.get(SourceList.LIST_NAME));
                    }
                }
            }
        }
        
        Reporting reporting = Reporting.get();
    }

    private Set<String> determineKeywords(SourceList sourceList, SourceSourceList link, Source source) {

        Set<String> baseKeywords = sourceList.get(SourceList.KEYWORDS);
        List<String> keywordDiffs = link.get(SourceSourceList.KEYWORDS_DIFF);

        Set<String> keywords = new HashSet<>(baseKeywords);

        for(String keywordDiff : keywordDiffs) {
            String diff = keywordDiff.substring(0,1);
            if(diff.equals("-")) {
                String keyword = keywordDiff.substring(1);
                if(!keywords.remove(keyword)) {
                    logger.warn("Attempted to remove {} when not it list - source: {} ", keyword, source.get(Source.NAME));
                }
            } else if(diff.equals("+")) {
                String keyword = keywordDiff.substring(1);
                keywords.add(keyword);
            } else {
                keywords.add(keywordDiff);
            }
        }

        return keywords;
    }



    public static void main(String[] args) throws Exception {

//        MorphologicalProcessor
        Dictionary dictionary = Dictionary.getDefaultResourceInstance();

        IndexWordSet words = dictionary.lookupAllIndexWords("kill");
        for(IndexWord word : words.getIndexWordCollection()) {

            for(Synset synset : word.getSenses()){
                for(Word w2 : synset.getWords()){
//                    dictionary.getMorphologicalProcessor()
                    System.out.println(w2);
                }
            }
            System.out.println(word);
        }

    }
}
