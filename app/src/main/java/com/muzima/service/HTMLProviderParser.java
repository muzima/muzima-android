/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
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

public class HTMLProviderParser {
    private static final String DATA_PROVIDER_TAG = "data-provider";
    public List<String> parse(String html) {
        Set<String> providers = new HashSet<>();
        Document htmlDoc = Jsoup.parse(html);
        //Select all elements containing data-providers attr and is not a div.
        Elements elements = htmlDoc.select("*:not(div)[" + DATA_PROVIDER_TAG + "]");
        for (Element element : elements) {
            providers.add((element.attr(DATA_PROVIDER_TAG)));
        }
        return new ArrayList<>(providers);
    }
}
