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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.util.Xml.newPullParser;

class ConceptParser {

    private static final String RULE = "ZZ";
    private XmlPullParser parser;

    public ConceptParser() {
        this(newPullParser());
    }

    public ConceptParser(XmlPullParser parser) {
        try {
            if (parser != null) {
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            }
        } catch (XmlPullParserException e) {
            throw new ParseConceptException(e);
        }
        this.parser = parser;
    }

    public List<String> parse(String model) {
        try {
            if (StringUtils.isEmpty(model)) {
                return new ArrayList<>();
            }
            parser.setInput(new ByteArrayInputStream(model.getBytes()), null);
            parser.nextTag();
            return readConceptName(parser);
        } catch (Exception e) {
            throw new ParseConceptException(e);
        }
    }

    /**
     * @param parser
     * @return List of Concepts from the XML.
     * @throws XmlPullParserException
     * @throws IOException
     * @description XMLPullParser is a STAX parser implementation for Android. We identify concept, if a tag has got date and time as children.
     * Then we extract the value of the openmrs_attribute and add it as a concept.
     */
    private static List<String> readConceptName(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<String> conceptNames = new ArrayList<>();

        //A concept should have both date and time tags as children. This var stores the value of parent for
        // date or time and then it is verified with the other tag's parent.
        String tempParent = null;

        //Parses the contents within model tag.
        while (!endOfModelTag(parser)) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                    //A concept must have an openmrs_datatype which should not be a RULE = "ZZ"
                    String openmrsConcept = null;
                    String datatype = parser.getAttributeValue("", "openmrs_datatype");
                    if (datatype != null && !datatype.equals(RULE)) {
                        openmrsConcept = parser.getAttributeValue(null, "openmrs_concept");
                        conceptNames.add(getConceptName(openmrsConcept));
                    }
            }
        }
        return conceptNames;
    }

    private static String getConceptName(String peek) {
        if (!StringUtils.isEmpty(peek) && peek.split("\\^").length > 1) {
            return peek.split("\\^")[1].trim();
        }
        return "";
    }

    private static boolean endOfModelTag(XmlPullParser parser) throws XmlPullParserException, IOException {
        return parser.next() == XmlPullParser.END_TAG && "model".equals(parser.getName());
    }

    public class ParseConceptException extends RuntimeException {

        public ParseConceptException(Exception e) {
            super(e);
        }
    }
}
