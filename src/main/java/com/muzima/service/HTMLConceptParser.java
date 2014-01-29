package com.muzima.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HTMLConceptParser {
    public List<String> parse(String html) {
        Set<String> concepts = new HashSet<String>();
        Document htmlDoc = Jsoup.parse(html);
        Elements elements = htmlDoc.select(".record-concept");
        for (Element element : elements) {
            concepts.add(element.attr("name"));
        }
        return new ArrayList<String>(concepts);
    }
}
