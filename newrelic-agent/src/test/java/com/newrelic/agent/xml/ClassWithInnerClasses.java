package com.newrelic.agent.xml;

import org.xml.sax.Attributes;

/**
 * This class is simply used for testing inner classes with the command line validator.
 * 
 * @since Oct 11, 2012
 */
public class ClassWithInnerClasses {

    private int value;

    public ClassWithInnerClasses() {
        super();
    }

    public int getValue() {
        return value;
    }

    class ListBucketHandler {

        public ListBucketHandler() {
            super();
        }

        public void startElement(String one, String two, String three, Attributes atts) {
            System.out.println("In start element.");
        }

        public String getMarker() {
            return "hello";
        }

        public String getPrefix() {
            return "this is a test";
        }
    }

    static class ListAllMyBucketsHander {

        private void startDocument() {
            System.out.println("starting");
        }

        private void endDocument() {
            System.out.println("ending");
        }

        public void characters(char[] input, int one, int two) {
            System.out.println("characters");
        }
    }
}
