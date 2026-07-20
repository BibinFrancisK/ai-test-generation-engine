package com.testgen.healing;

import com.testgen.model.TestFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static com.testgen.util.Constants.FAILURE_TAGS;

@Component
public class JUnitXmlReportParser {

    private static final Logger log = LoggerFactory.getLogger(JUnitXmlReportParser.class);

    public List<TestFailure> parse(String xmlContent) {
        if (xmlContent == null || xmlContent.isBlank()) {
            return List.of();
        }

        try {
            Document document = parseDocument(xmlContent);
            return extractFailures(document);
        } catch (Exception e) {
            log.warn("Failed to parse Surefire XML report, returning no failures", e);
            return List.of();
        }
    }

    private Document parseDocument(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // XXE hardening — this report can originate from a CI artifact, not a trusted source
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlContent)));
    }

    private List<TestFailure> extractFailures(Document document) {
        List<TestFailure> failures = new ArrayList<>();
        NodeList testCases = document.getElementsByTagName("testcase");

        for (int i = 0; i < testCases.getLength(); i++) {
            Element testCase = (Element) testCases.item(i);
            Element failureElement = findFailureElement(testCase);
            if (failureElement == null) {
                continue;
            }

            failures.add(new TestFailure(
                    testCase.getAttribute("classname"),
                    testCase.getAttribute("name"),
                    failureElement.getAttribute("message"),
                    failureElement.getTextContent()
            ));
        }

        return List.copyOf(failures);
    }

    private Element findFailureElement(Element testCase) {
        for (String tag : FAILURE_TAGS) {
            NodeList children = testCase.getElementsByTagName(tag);
            if (children.getLength() > 0) {
                return (Element) children.item(0);
            }
        }
        return null;
    }
}
