/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension.dom;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.extension.beans.Extension;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;

public class ExtensionDomParser {

    private static final ErrorHandler LOGGING_ERROR_HANDLER = new ErrorHandler() {
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            Agent.LOG.log(Level.FINEST, exception.toString(), exception);
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            Agent.LOG.log(Level.FINER, exception.toString(), exception);
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            Agent.LOG.log(Level.FINEST, exception.toString(), exception);
        }
    };

    private static final ErrorHandler IGNORE_ERROR_HANDLER = new ErrorHandler() {
        @Override
        public void warning(SAXParseException exception) throws SAXException {
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
        }
    };

    private static final String NAMESPACE = "https://newrelic.com/docs/java/xsd/v1.0";
    private static final DocumentBuilderFactory documentFactory = initializeDocumentFactory();
    private static final DocumentBuilderFactory schemaDocumentFactory = initializeSchemaDocumentFactory();

    private static DocumentBuilderFactory initializeDocumentFactory() {
        DocumentBuilderFactory factory;
        try {
            factory = getDocumentBuilderFactory();
        } catch (Throwable e) {
            factory = getAndSetupDocumentBuilderComSunFactory();
        }
        return factory;
    }

    private static DocumentBuilderFactory initializeSchemaDocumentFactory() {
        DocumentBuilderFactory factory = documentFactory;
        try {
            Schema schema = getSchema();
            factory.setSchema(schema);
        } catch (Throwable e) {
            // Return null factory if we couldn't create & set the schema
            factory = null;
            Agent.LOG.log(Level.FINE, "Unable to initialize schema", e);
        }
        return factory;
    }

    /**
     * Reads an XML file which is in the string format.
     *
     * @param xml The xml to be parsed.
     * @return The converted XML.
     */
    public static Extension readStringGatherExceptions(final String xml, List<Exception> exceptions) {
        if ((xml == null) || (xml.length() == 0)) {
            Agent.LOG.log(Level.FINE, "The input xml string is empty.");
            return null;
        } else {
            try {
                return parseDocument(xml, false);
            } catch (Exception e) {
                exceptions.add(e);
                return null;
            }
        }
    }

    /**
     * Reads an XML file which is in the string format.
     *
     * @param xml The xml to be parsed.
     * @return The converted XML.
     */
    public static Extension readStringCatchException(final String xml) {
        if ((xml == null) || (xml.length() == 0)) {
            Agent.LOG.log(Level.FINE, "The input xml string is empty.");
            return null;
        } else {
            try {
                return parseDocument(xml, false);
            } catch (Exception e) {
                Agent.LOG.log(Level.WARNING, MessageFormat.format("Failed to read extension {0}."
                        + " Skipping the extension. Reason: {1}", xml, e.getMessage()));
                if (Agent.LOG.isFinerEnabled()) {
                    Agent.LOG.log(Level.FINER, "Reason For Failure: " + e.getMessage(), e);
                }
                return null;
            }
        }
    }

    public static Extension readFileCatchException(final File file) {
        try {
            return readFile(file);
        } catch (Exception e) {
            Agent.LOG.log(Level.WARNING, MessageFormat.format("Failed to read extension {0}."
                    + " Skipping the extension. Reason: {1}", file.getName(), e.getMessage()));
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.log(Level.FINER, "Reason For Failure: " + e.getMessage(), e);
            }
            return null;
        }
    }

    public static Extension readFile(final File file) throws SAXException, IOException, ParserConfigurationException,
            NoSuchMethodException, SecurityException {
        return parseDocument(inputStreamToString(new FileInputStream(file)), true);
    }

    /**
     * Reads an extension from a stream. The stream will not be closed.
     *
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public static Extension readFile(InputStream inputStream) throws SAXException, IOException,
            ParserConfigurationException, NoSuchMethodException, SecurityException {
        return parseDocument(inputStreamToString(inputStream), true);
    }

    /**
     * Reads in the XML file and returns an extension.
     *
     * @param extensionXML The input xml
     * @param setSchema whether or not to set the schema
     */
    public static Extension parseDocument(String extensionXML, boolean setSchema) throws SAXException, IOException, ParserConfigurationException,
            NoSuchMethodException, SecurityException {
        Document doc = getDocument(extensionXML, setSchema);

        trimTextNodeWhitespace(doc.getDocumentElement());
        doc = fixNamespace(doc);

        // validate the document

        Schema schema = schemaDocumentFactory.getSchema();
        Validator validator = schema.newValidator();
        validator.validate(new DOMSource(doc));

        try {
            Extension extension = new Extension();
            NodeList extensionElements = doc.getElementsByTagNameNS("*", "extension");
            if (extensionElements != null && extensionElements.getLength() == 1) {
                Node extensionElement = extensionElements.item(0);
                extension.setName(getAttribute("name", extensionElement, null));
                extension.setVersion(Double.parseDouble(getAttribute("version", extensionElement, "1.0")));
                extension.setEnabled(Boolean.valueOf(getAttribute("enabled", extensionElement, "true")));

                NodeList extensionChildNodes = extensionElement.getChildNodes();
                Node instrumentationElement = getFirstInstrumentationNode(extensionChildNodes);
                if (instrumentationElement != null) {
                    Extension.Instrumentation instrumentation = new Extension.Instrumentation();
                    instrumentation.setMetricPrefix(getAttribute("metricPrefix", instrumentationElement, null));
                    extension.setInstrumentation(instrumentation);

                    List<Extension.Instrumentation.Pointcut> pointcuts = instrumentation.getPointcut();

                    NodeList instrumentationChildNodes = instrumentationElement.getChildNodes();
                    for (int i = 0; i < instrumentationChildNodes.getLength(); i++) {
                        Node instrumentationChildNode = instrumentationChildNodes.item(i);
                        if (instrumentationChildNode.getNodeName().equals("pointcut") || instrumentationChildNode.getNodeName().endsWith(":pointcut")) {
                            Extension.Instrumentation.Pointcut pointcut = new Extension.Instrumentation.Pointcut();
                            pointcut.setExcludeFromTransactionTrace(
                                    Boolean.valueOf(getAttribute("excludeFromTransactionTrace", instrumentationChildNode, "false")));
                            pointcut.setLeaf(
                                    Boolean.valueOf(getAttribute("leaf", instrumentationChildNode, "false")));
                            pointcut.setIgnoreTransaction(Boolean.valueOf(getAttribute("ignoreTransaction", instrumentationChildNode, "false")));
                            pointcut.setMetricNameFormat(getAttribute("metricNameFormat", instrumentationChildNode, null));
                            pointcut.setTransactionStartPoint(Boolean.valueOf(getAttribute("transactionStartPoint", instrumentationChildNode, "false")));
                            pointcut.setTransactionType(getAttribute("transactionType", instrumentationChildNode, null));

                            List<Extension.Instrumentation.Pointcut.Method> methods = pointcut.getMethod();
                            List<String> traceReturnTypeDescriptors = pointcut.getTraceReturnTypeDescriptors();

                            NodeList pointcutChildNodes = instrumentationChildNode.getChildNodes();
                            for (int p = 0; p < pointcutChildNodes.getLength(); p++) {
                                Node node = pointcutChildNodes.item(p);
                                if (node.getNodeName().equals("className") || node.getNodeName().endsWith(":className")) {
                                    Extension.Instrumentation.Pointcut.ClassName className = new Extension.Instrumentation.Pointcut.ClassName();
                                    className.setIncludeSubclasses(Boolean.valueOf(getAttribute("includeSubclasses", node, "false")));
                                    className.setValue(node.getTextContent());
                                    pointcut.setClassName(className);
                                } else if (node.getNodeName().equals("interfaceName") || node.getNodeName().endsWith(":interfaceName")) {
                                    pointcut.setInterfaceName(node.getTextContent());
                                } else if (node.getNodeName().equals("methodAnnotation") || node.getNodeName().endsWith(":methodAnnotation")) {
                                    pointcut.setMethodAnnotation(node.getTextContent());
                                } else if (node.getNodeName().equals("traceLambda") || node.getNodeName().endsWith(":traceLambda")) {
                                    pointcut.setTraceLambda(Boolean.valueOf(node.getTextContent()));
                                    pointcut.setPattern(getAttribute("pattern", node, "^\\$?(lambda|anonfun)\\$(?<name>.*)"));
                                    pointcut.setIncludeNonstatic(Boolean.parseBoolean(getAttribute("includeNonstatic", node, "false")));
                                } else if (node.getNodeName().equals("traceByReturnType") || node.getNodeName().endsWith(":traceByReturnType")) {
                                    traceReturnTypeDescriptors.add(node.getTextContent());
                                } else if (node.getNodeName().equals("method") || node.getNodeName().endsWith(":method")) {
                                    NodeList methodChildNodes = node.getChildNodes();
                                    Extension.Instrumentation.Pointcut.Method method = new Extension.Instrumentation.Pointcut.Method();
                                    for (int m = 0; m < methodChildNodes.getLength(); m++) {
                                        Node methodChildNode = methodChildNodes.item(m);
                                        if (methodChildNode.getNodeName().equals("name") || methodChildNode.getNodeName().endsWith(":name")) {
                                            method.setName(methodChildNode.getTextContent());
                                        } else if (methodChildNode.getNodeName().equals("returnType") ||
                                                methodChildNode.getNodeName().endsWith(":returnType")) {
                                            method.setReturnType(methodChildNode.getTextContent());
                                        } else if (methodChildNode.getNodeName().equals("parameters") ||
                                                methodChildNode.getNodeName().endsWith(":parameters")) {
                                            Extension.Instrumentation.Pointcut.Method.Parameters parameters
                                                    = new Extension.Instrumentation.Pointcut.Method.Parameters();
                                            List<Extension.Instrumentation.Pointcut.Method.Parameters.Type> types = parameters.getType();

                                            NodeList parametersChildNodes = methodChildNode.getChildNodes();
                                            for (int p1 = 0; p1 < parametersChildNodes.getLength(); p1++) {
                                                Node typeNode = parametersChildNodes.item(p1);
                                                if (typeNode.getNodeName().equals("type") || typeNode.getNodeName().endsWith(":type")) {
                                                    Extension.Instrumentation.Pointcut.Method.Parameters.Type type
                                                            = new Extension.Instrumentation.Pointcut.Method.Parameters.Type();
                                                    type.setAttributeName(getAttribute("attributeName", typeNode, null));
                                                    type.setValue(typeNode.getTextContent());

                                                    types.add(type);
                                                }
                                            }

                                            method.setParameters(parameters);
                                        }
                                    }

                                    methods.add(method);
                                } else if (node.getNodeName().equals("nameTransaction") || node.getNodeName().endsWith(":nameTransaction")) {
                                    pointcut.setNameTransaction(new Extension.Instrumentation.Pointcut.NameTransaction());
                                }
                            }

                            pointcuts.add(pointcut);
                        }
                    }
                }
            }

            return extension;
        } catch (Exception ex) {
            try {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                // initialize StreamResult with File object to save to file
                StreamResult result = new StreamResult(new StringWriter());
                DOMSource source = new DOMSource(doc);
                transformer.transform(source, result);
                String xmlString = result.getWriter().toString();
                System.out.println(xmlString);
            } catch (Exception e) {
                e.printStackTrace();
            }
            throw new IOException(ex);
        }
    }

    private static Node getFirstInstrumentationNode(NodeList extensionChildNodes) {
        for (int i = 0; i < extensionChildNodes.getLength(); i++) {
            Node node = extensionChildNodes.item(i);
            if (node.getNodeName().equals("instrumentation") || node.getNodeName().endsWith(":instrumentation")) {
                return node;
            }
        }
        return null;
    }

    private static String getAttribute(String attributeName, Node element, String defaultValue) {
        Node nameElement = element.getAttributes().getNamedItem(attributeName);
        if (nameElement != null) {
            String value = nameElement.getNodeValue();
            if (value == null) {
                return defaultValue;
            }
            return value;
        }
        return defaultValue;
    }

    /**
     * Reads in the XML string and returns a document.
     *
     * @param pXml The input xml string.
     * @param setSchema whether or not to set the schema
     * @return The parsed document.
     */
    private static Document getDocument(String pXml, boolean setSchema) throws SAXException, IOException,
            ParserConfigurationException, NoSuchMethodException, SecurityException {
        try (ByteArrayInputStream baos = new ByteArrayInputStream(pXml.getBytes(StandardCharsets.UTF_8))) {
            return getDocument(new InputSource(baos), setSchema);
        }
    }

    /**
     * Reads in the file, parses it, and returns a document.
     *
     * @param file The input file to be parsed.
     * @return The document with the xml nodes.
     */
    private static Document getDocument(File file) throws SAXException, IOException, ParserConfigurationException, NoSuchMethodException, SecurityException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return getDocument(new InputSource(new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))), true);
        }
    }

    private static Schema getSchema() throws IOException, SAXException, ParserConfigurationException, NoSuchMethodException, SecurityException {
        URL schemaFile = AgentBridge.getAgent().getClass().getClassLoader().getResource("META-INF/extensions/extension.xsd");
        if (schemaFile == null) {
            throw new IOException("Unable to load the extension schema");
        }

        Agent.LOG.finest("Loading extension schema from " + schemaFile);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        DocumentBuilderFactory factory = documentFactory;

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(LOGGING_ERROR_HANDLER);
        Document schemaDoc = builder.parse(
                new InputSource(new BufferedReader(new InputStreamReader(schemaFile.openStream(), StandardCharsets.UTF_8))));

        // I tried to load the schema without using a document builder (just a StreamSource) and that worked in the
        // builds but failed in Tomcat 7 with an NPE
        // at com.sun.org.apache.xerces.internal.impl.XMLEntityScanner.load(XMLEntityScanner.java:1738) ~[na:1.6.0_51]
        // at com.sun.org.apache.xerces.internal.impl.XMLEntityScanner.load(XMLEntityScanner.java:1770) ~[na:1.6.0_51]
        // at com.sun.org.apache.xerces.internal.impl.XMLEntityScanner.skipSpaces(XMLEntityScanner.java:1492)
        // ~[na:1.6.0_51]
        // The Document seems to work okay
        return schemaFactory.newSchema(new DOMSource(schemaDoc));
    }

    private static Document getDocument(InputSource inputSource, boolean setSchema) throws SAXException, IOException,
            ParserConfigurationException, NoSuchMethodException, SecurityException {
        DocumentBuilderFactory factory = documentFactory;
        if (setSchema) {
            if (schemaDocumentFactory == null) {
                throw new IOException("Unable to initialize schema document factory");
            }
            factory = schemaDocumentFactory;
        }
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(IGNORE_ERROR_HANDLER);

        return builder.parse(inputSource);
    }

    /**
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @see DocumentBuilderFactory#setSchema(Schema)
     */
    private static DocumentBuilderFactory getDocumentBuilderFactory() throws ParserConfigurationException, NoSuchMethodException, SecurityException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            setupDocumentFactory(factory);

            /*
             * The method setFeature was added to the abstract class DocumentBuilderFactory in Java version 1.5. If a
             * customer is using a DocumentBuilderFactory implementation that was compiled with 1.4 or below, then it
             * will not have the setFeature method and an AbstractMethodError will be thrown. AbstractMethodError does
             * not extend Exception (it extends Throwable). Therefore the exception leaks out. Try to load the 1.5+
             * version, otherwise error out.
             */
        } catch (AbstractMethodError e) {
            return getAndSetupDocumentBuilderComSunFactory();
        }

        return factory;
    }

    private static void setupDocumentFactory(DocumentBuilderFactory factory) throws ParserConfigurationException {
        factory.setNamespaceAware(true);
        // The following 4 properties are to prevent XML entity extension attacks - do not remove them
        factory.setExpandEntityReferences(false);
        // do not include external general entities
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        // do not include external parameter entitites or the external DTD subset
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        // when true, A fatal error is thrown if the incoming document contains a DOCTYPE declaration.
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        // we call setSchema so validating should be false
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);
    }

    private static DocumentBuilderFactory getAndSetupDocumentBuilderComSunFactory() throws NoSuchMethodError {
        try {
            Class<?> clazz = AgentBridge.getAgent().getClass().getClassLoader().loadClass(
                    "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory factory = (DocumentBuilderFactory) clazz.newInstance();
            setupDocumentFactory(factory);
            return factory;
        } catch (Throwable e) {
            Agent.LOG.info("Your application has loaded a Java 1.4 or below implementation of the class DocumentBuilderFactory." +
                    " Please upgrade to a 1.5 version if you want to use Java agent XML instrumentation.");
            throw new NoSuchMethodError("The method setFeature can not be called.");
        }
    }

    /**
     * Trim whitespace from all text nodes.
     */
    public static void trimTextNodeWhitespace(Node e) {
        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Text) {
                Text text = (Text) child;
                text.setData(text.getData().trim());
            }
            trimTextNodeWhitespace(child);
        }
    }

    /**
     * This is brutal, but we need to fix the namespace of the document and the dom doesn't allow this.
     */
    private static Document fixNamespace(Document doc) {
        try {
            Transformer transformer = getTransformerFactory().newTransformer();
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);
            String xmlString = result.getWriter().toString();
            xmlString = xmlString.replace("xmlns:urn=\"newrelic-extension\"", "xmlns:urn=\"" + NAMESPACE + "\"");
            return getDocument(xmlString, true);
        } catch (Exception ex) {
            return doc;
        }
    }

    /**
     * If the "javax.xml.transform.TransformerFactory" system property is set to a class that's not on the app classpath
     * our call to get a transformerfactory will fail. Fall back to trying to load the class that should be packaged in
     * the JVM.
     *
     * First we will try to use the "newTransformerFactoryNoServiceLoader" method which existed in JDKs < 1.8.0_161. If
     * that fails we will just try to use the constructor as a secondary fallback.
     *
     * @throws TransformerFactoryConfigurationError
     */
    private static TransformerFactory getTransformerFactory() throws TransformerFactoryConfigurationError {
        try {
            return TransformerFactory.newInstance();
        } catch (TransformerFactoryConfigurationError ex) {
            try {
                Class<?> clazz = AgentBridge.getAgent().getClass().getClassLoader().loadClass(
                        "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
                Method method = clazz.getMethod("newTransformerFactoryNoServiceLoader");
                return (TransformerFactory) method.invoke(null);
            } catch (Exception e1) {
                try {
                    Class<?> clazz = AgentBridge.getAgent().getClass().getClassLoader().loadClass(
                            "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
                    return (TransformerFactory) clazz.newInstance();
                } catch (Exception e2) {
                    throw ex;
                }
            }
        }
    }

    private static String inputStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }
}
