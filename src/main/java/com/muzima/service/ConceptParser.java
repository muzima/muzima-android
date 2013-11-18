package com.muzima.service;

import com.muzima.utils.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static android.util.Xml.newPullParser;

public class ConceptParser {

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
                return new ArrayList<String>();
            }
            parser.setInput(new ByteArrayInputStream(model.getBytes()), null);
            parser.nextTag();
            return readConceptName(parser);
        } catch (Exception e) {
            throw new ParseConceptException(e);
        }
    }

    private static List<String> readConceptName(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<String> conceptNames = new ArrayList<String>();
        Stack<String> parentConcept = new Stack<String>();
        String tempParent = null;
        while (!endOfModelTag(parser)) {
            switch (parser.getEventType()) {
                case XmlPullParser.START_TAG:
                    if (("date".equals(parser.getName()) || "time".equals(parser.getName())) && !parentConcept.isEmpty()) {
                        if (parentConcept.peek().equals(tempParent)) {
                            conceptNames.add(getConceptName(parentConcept.peek()));
                        }
                        tempParent = parentConcept.peek();
                    }
                    parentConcept.push(parser.getText());
                    break;
                case XmlPullParser.END_TAG:
                    if (!parentConcept.isEmpty()) {
                        parentConcept.pop();
                    }
                    break;
            }
        }
        return conceptNames;
    }

    private static String getConceptName(String peek) {
        if (!StringUtils.isEmpty(peek) && peek.split("\\^").length > 1) {
            return peek.split("\\^")[1];
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
