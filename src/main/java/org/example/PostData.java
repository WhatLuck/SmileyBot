package org.example;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PostData {
    private String htmlUrl;
    private double pointsPossible;
    private String unlockAt;
    private String lockAt;
    private String dueAt;
    private int courseId;
    private String name;
    private int assignmentId;
    private boolean hasSubmittedSubmissions;
    private String description; // Added field for description
    private String courseName; // Added field for course name

    public PostData(String htmlUrl, double pointsPossible, String unlockAt, String lockAt, String dueAt, int courseId, String name, int assignmentId, boolean hasSubmittedSubmissions, String description, String courseName) {
        this.htmlUrl = htmlUrl;
        this.pointsPossible = pointsPossible;
        this.unlockAt = unlockAt;
        this.lockAt = lockAt;
        this.dueAt = dueAt;
        this.courseId = courseId;
        this.name = name;
        this.assignmentId = assignmentId;
        this.hasSubmittedSubmissions = hasSubmittedSubmissions;
        this.description = description;
        this.courseName = courseName;
    }

    // Getters for existing fields

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public double getPointsPossible() {
        return pointsPossible;
    }

    public String getUnlockAt() {
        return unlockAt;
    }

    public String getLockAt() {
        return lockAt;
    }

    public String getDueAt() {
        if (dueAt == null || dueAt.isEmpty() || dueAt.equals("null")) {
            return "Soon";
        }

        Instant instant = Instant.parse(dueAt);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("UTC"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M-d-yyyy h:mm a");

        return zonedDateTime.format(formatter);
    }


    public int getCourseId() {
        return courseId;
    }

    public boolean hasSubmittedSubmissions() {
        return hasSubmittedSubmissions;
    }

    public String getName() {
        return name;
    }

    public int getAssignmentId() {
        return assignmentId;
    }


    public String getDescription() {
        if (description != null && !description.isEmpty()) {
            // Replace <p> tags with new lines
            description = description.replaceAll("<p>", "")
                    .replaceAll("</p>", "\n");

            // Replace <strong> tags with asterisks for bold effect
            description = description.replaceAll("<strong>", "**")
                    .replaceAll("</strong>", "**");

            // Remove <span> tags and their attributes
            description = description.replaceAll("<span[^>]*>", "")
                    .replaceAll("</span>", "");

            // Remove <li> tags and their attributes
            description = description.replaceAll("<li[^>]*>", " - ")
                    .replaceAll("</li>", "");

            // Remove style attribute
            description = description.replaceAll("style=\"[^\"]*\"", "");

            // Remove <ul> tags and their attributes
            description = description.replaceAll("<ul[^>]*>", "")
                    .replaceAll("</ul>", "");

            // Truncate to maximum length
            int maxLength = 500; // or 500, adjust as needed
            if (description.length() > maxLength) {
                description = description.substring(0, maxLength - 3) + "...";
            }

            return description;
        } else {
            return "";
        }
    }


    public String getCourseName() {
        return courseName;
    }

    public static List<PostData> parseXMLAssignments(String xmlContent) {
        List<PostData> assignments = new ArrayList<>();
        try {
            xmlContent = xmlContent.trim();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new ByteArrayInputStream(xmlContent.getBytes("UTF-8"))));

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();

            XPathExpression assignmentsExpr = xpath.compile("/feed/entry");
            NodeList assignmentNodes = (NodeList) assignmentsExpr.evaluate(doc, XPathConstants.NODESET);

            for (int i = 0; i < assignmentNodes.getLength(); i++) {
                Node assignmentNode = assignmentNodes.item(i);

                String htmlUrl = getNodeTextContent(assignmentNode, "link");
                double pointsPossible = getNodeDoubleContent(assignmentNode, "content");
                String unlockAt = getNodeTextContent(assignmentNode, "unlock_at");
                String lockAt = getNodeTextContent(assignmentNode, "lock_at");
                String dueAt = getNodeTextContent(assignmentNode, "due_at");
                int courseId = getNodeIntContent(assignmentNode, "course_id");
                String name = getNodeTextContent(assignmentNode, "title");
                int assignmentId = getNodeIntContent(assignmentNode, "id");
                boolean hasSubmittedSubmissions = getNodeBooleanContent(assignmentNode, "has_submitted_submissions");
                String description = getNodeTextContent(assignmentNode, "description"); // Get description if present
                String courseName = getNodeTextContent(assignmentNode, "context_name"); // Get course name if present

                assignments.add(new PostData(htmlUrl, pointsPossible, unlockAt, lockAt, dueAt, courseId, name, assignmentId, hasSubmittedSubmissions, description, courseName));
            }
        } catch (ParserConfigurationException | org.xml.sax.SAXException | IOException | XPathExpressionException e) {
            e.printStackTrace();
        }

        return assignments;
    }
    public static List<PostData> parseJSONAssignments(String jsonContent) {
        List<PostData> assignments = new ArrayList<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonContent);

            // Iterate over each JSON object in the array
            for (JsonNode assignmentNode : rootNode) {
                String htmlUrl = assignmentNode.path("assignment").path("html_url").asText();
                double pointsPossible = assignmentNode.path("assignment").path("points_possible").asDouble();
                String unlockAt = assignmentNode.path("assignment").path("unlock_at").asText();
                String lockAt = assignmentNode.path("assignment").path("lock_at").asText();
                String dueAt = assignmentNode.path("assignment").path("due_at").asText();
                int courseId = assignmentNode.path("assignment").path("course_id").asInt();
                String name = assignmentNode.path("assignment").path("name").asText();
                int assignmentId = assignmentNode.path("assignment").path("id").asInt();
                boolean hasSubmittedSubmissions = assignmentNode.path("assignment").path("has_submitted_submissions").asBoolean();
                String description = assignmentNode.path("assignment").path("description").asText(); // Get description if present
                String courseName = assignmentNode.path("context_name").asText(); // Get course name if present

                assignments.add(new PostData(htmlUrl, pointsPossible, unlockAt, lockAt, dueAt, courseId, name, assignmentId, hasSubmittedSubmissions, description, courseName));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return assignments;
    }


    private static String getNodeTextContent(Node parentNode, String tagName) {
        Node node = ((Element) parentNode).getElementsByTagName(tagName).item(0);
        return node != null ? node.getTextContent() : "";
    }

    private static double getNodeDoubleContent(Node parentNode, String tagName) {
        String textContent = getNodeTextContent(parentNode, tagName);
        return textContent != null && !textContent.isEmpty() ? Double.parseDouble(textContent) : 0.0;
    }

    private static int getNodeIntContent(Node parentNode, String tagName) {
        String textContent = getNodeTextContent(parentNode, tagName);
        return textContent != null && !textContent.isEmpty() ? Integer.parseInt(textContent) : 0;
    }

    private static boolean getNodeBooleanContent(Node parentNode, String tagName) {
        String textContent = getNodeTextContent(parentNode, tagName);
        return textContent != null && !textContent.isEmpty() && Boolean.parseBoolean(textContent);
    }

}
