/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

import com.muzima.utils.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class HTMLConceptParser {
    private static final String DATA_CONCEPT_TAG = "data-concept";

    public List<String> parse(String html) {
        Set<String> concepts = new HashSet<>();
        Document htmlDoc = Jsoup.parse(html);
        //Select all elements containing data-concept attr and is not a div.
        Elements elements = htmlDoc.select("*:not(div)[" + DATA_CONCEPT_TAG + "]");
        for (Element element : elements) {
            concepts.add(getConceptName(element.attr(DATA_CONCEPT_TAG)));
        }
        return new ArrayList<>(concepts);
    }

    private static String getConceptName(String conceptName) {
        if (!StringUtils.isEmpty(conceptName) && conceptName.split("\\^").length > 1) {
            return conceptName.split("\\^")[1];
        }
        return "";
    }
}
