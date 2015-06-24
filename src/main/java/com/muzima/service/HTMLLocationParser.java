/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class HTMLLocationParser {

    public static final String DATA_LOCATION_TAG = "data-location";
    public List<String> parse(String html) {
        Set<String> locations = new HashSet<String>();
        Document htmlDoc = Jsoup.parse(html);
        //Select all elements containing data-locations attr and is not a div.
        Elements elements = htmlDoc.select("*:not(div)[" + DATA_LOCATION_TAG + "]");
        for (Element element : elements) {
            locations.add((element.attr(DATA_LOCATION_TAG)));
        }
        return new ArrayList<String>(locations);
    }
}