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

    /**
     * @param parser
     * @return List of Concepts from the XML.
     * @throws XmlPullParserException
     * @throws IOException
     * @description XMLPullParser is a STAX parser implementation for Android. We identify concept, if a tag has got date and time as children.
     * Then we extract the value of the openmrs_attribute and add it as a concept.
     */
    private static List<String> readConceptName(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<String> conceptNames = new ArrayList<String>();
        Stack<String> parentConcept = new Stack<String>(); //Used to store the immediate parent.

        //A concept should have both date and time tags as children. This var stores the value of parent for
        // date or time and then it is verified with the other tag's parent.
        String tempParent = null;

        //Parses the contents within model tag.
        while (!endOfModelTag(parser)) {
            switch (parser.getEventType()) {
                case XmlPullParser.START_TAG:
                    //If the tag is either a date or time. The stack cannot be empty since date and time has to be children of concept.
                    if (("date".equals(parser.getName()) || "time".equals(parser.getName())) && !parentConcept.isEmpty()) {
                        //The tempParent holds the parent of either date or time. Here we are adding concept only if the both date and time has same parent.
                        if (parentConcept.peek().equals(tempParent)) {
                            conceptNames.add(getConceptName(parentConcept.peek()));
                        }
                        //Storing the parent of either date or time. Have to be used for next time while adding concept.
                        tempParent = parentConcept.peek();
                    }
                    //Adding the current tag as parent to the next one.
                    parentConcept.push(parser.getText());
                    break;
                case XmlPullParser.END_TAG:
                    if (!parentConcept.isEmpty()) {
                        //Removing the current tag from parent list. This can't be a parent anymore.
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
