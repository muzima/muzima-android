package com.muzima.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HTMLProviderParser {
    public static final String DATA_PROVIDER_TAG = "data-provider";
    public List<String> parse(String html) {
        Set<String> providers = new HashSet<String>();
        Document htmlDoc = Jsoup.parse(html);
        //Select all elements containing data-providers attr and is not a div.
        Elements elements = htmlDoc.select("*:not(div)[" + DATA_PROVIDER_TAG + "]");
        for (Element element : elements) {
            providers.add((element.attr(DATA_PROVIDER_TAG)));
        }
        return new ArrayList<String>(providers);
    }
}
