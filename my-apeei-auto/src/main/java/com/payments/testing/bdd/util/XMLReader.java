package com.payments.testing.bdd.util;

import java.util.Vector;

public class XMLReader {
    private String defaultNodeValue = null;

    /** XML string to parse */
    private String strXML;


    /**
     * Constructor
     *
     * @param xml
     *            XML string to parse
     */
    public XMLReader(String xml) {
        this.strXML = xml;
    }

    /**
     * Retrive string value between tags from given path. XML file was given in
     * constructor, default value is null.
     *
     * @param XMLpath
     *            path to string
     * @return value in given path or default value if path not found
     */
    public String getText(String XMLpath) {
        return getText(XMLpath, null, null);
    }

    /**
     * Retrive string value between tags from given path.
     *
     * @param XMLpath
     *            path to string
     * @param defaultValue
     *            default value to return if path not found
     * @return value in given path or default value if path not found
     */
    public String getText(String XMLpath, String defaultValue) {
        this.defaultNodeValue = defaultValue;
        return getText(XMLpath, defaultValue, null);
    }

    /**
     * Retrive string value between tags from given path.
     *
     * @param XMLpath
     *            path to string
     * @param defaultValue
     *            default value to return if path not found
     * @param XMLFile
     *            xml string to parse instead of given in constructor
     * @return value in given path or default value if path not found
     */
    public String getText(String XMLpath, String defaultValue, String XMLFile) {
        if (strXML == null)
            return null;
        try {
            /** Disasembly path */
            Object[][] path = dissasemblyPath(XMLpath);
            /** XML string to parse */
            String result = strXML;
            /** If given xml string is null use xml given in constructor */
            if (XMLFile != null)
                result = XMLFile;
            /** Extract value from path */
            for (int i = 0; i < path.length; i++) {
                /** Get element from path */
                String element = (String) path[i][0];
                /** Get occurence we search */
                int occurence = ((Integer) path[i][1]).intValue();
                /** Extract element */
                result = extractElement(element, occurence, result);
            }
            /** Return extracted value */
            return result;
        } catch (Exception e) {
        }
        return defaultValue;
    }

    /**
     * Search for given element and return its body.
     *
     * @param element
     * @param occurence
     * @param xmlStr
     *            string where we look for element
     * @return
     */
    private String extractElement(String element, int occurence, String xmlStr) {
        int index = 0;
        String elString = null;
        do {
            int elNameLength = element.length() + 2;
            int elStart = xmlStr.indexOf("<" + element , index);
            int elEnd = xmlStr.indexOf("</" + element + ">", elStart
                    + elNameLength);
            // returns 'defaultNodeValue' if the specifed child node(eg:
            // books/book[4]) not found or none of childs are present
            if (elStart == -1 || elEnd == -1)
                return defaultNodeValue;
            elString = xmlStr.substring(elStart + elNameLength, elEnd);
            occurence--;
            index = elEnd + elNameLength;
        }
        /** Repeat until occurence is 0 or null is given */
        while (occurence > 0 && elString != null);
        return elString;
    }

    /**
     * Disassembly path to elements and its occurences
     *
     * @param XMLpath
     *            path to disassembly
     * @return Object[][] array, first element in array is path element, second
     *         is occurence.
     */
    private Object[][] dissasemblyPath(String XMLpath) {
        /** Split path */
        String[] str = split(XMLpath, "/");
        /** Create array to keep elements and occurences */
        Object[][] resultArray = new Object[str.length][2];
        /** Fill array with elements from path */
        for (int i = 0; i < str.length; i++) {
            /** Get path piece */
            String s = str[i];
            String occurence = "1";
            /** Search for occurence */
            int occur1 = s.indexOf("[");
            int occur2 = s.indexOf("]", occur1 + 1);
            if (occur1 != -1 && occur2 != -1) {
                /** Occurence defined */
                occurence = s.substring(occur1 + 1, occur2);
                resultArray[i][0] = s.substring(0, occur1);
                resultArray[i][1] = new Integer(Integer.parseInt(occurence));
            } else {
                /** No occurence defined, using default */
                resultArray[i][0] = s;
                resultArray[i][1] = new Integer(1);
            }
        }
        return resultArray;
    }

    public static String[] split(String source, String separator) {
        Vector<String> vector = new Vector<String>();
        String[] strings = null;
        while (true) {
            int index = source.indexOf(separator);

            if (index == -1) {
                vector.addElement(source);
                break;
            }
            vector.addElement(source.substring(0, index));
            source = source.substring(index + separator.length());
        }

        strings = new String[vector.size()];
        for (int i = 0; i < vector.size(); ++i) {
            strings[i] = ((String) vector.elementAt(i));
        }

        return strings;
    }

}