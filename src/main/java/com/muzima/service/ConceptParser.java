package com.muzima.service;

import com.muzima.utils.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.util.Xml.newPullParser;

public class ConceptParser {

    private XmlPullParser parser;

    public ConceptParser() {
        this(newPullParser());
    }

    public ConceptParser(XmlPullParser parser)  {
        try {
            if (parser!=null) {
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            }
        } catch (XmlPullParserException e) {
            throw new ParseConceptException(e);
        }
        this.parser = parser;
    }

    public List<String> parse(String model) {
        try {
            if(StringUtils.isEmpty(model)) {
                return new ArrayList<String>();
            }
            parser.setInput(new ByteArrayInputStream(model.getBytes()), null);
            parser.nextTag();
            return readConceptName(parser);
        } catch (Exception e){
            throw new ParseConceptException(e);
        }
    }

    private static List<String> readConceptName(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<String> conceptNames = new ArrayList<String>();
        while (!endOfModelTag(parser)) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String conceptInfo = parser.getAttributeValue(null, "openmrs_concept");
            if (!StringUtils.isEmpty(conceptInfo)) {
                String name = conceptInfo.split("\\^")[1];
                conceptNames.add(name);
            }
        }
        return conceptNames;
    }

    private static boolean endOfModelTag(XmlPullParser parser) throws XmlPullParserException, IOException {
        return parser.next() == XmlPullParser.END_TAG && "model".equals(parser.getName());
    }

    public class ParseConceptException extends RuntimeException{

        public ParseConceptException(Exception e) {
            super(e);
        }
    }
}
