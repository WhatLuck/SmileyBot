package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GradeData {
    private static final String JDBC_DATABASE_URL = "jdbc:sqlite:database.db";

    public static List<GradeData.EnrollmentData> parseEnrollmentData(String jsonContent, String discordId) {
        String studentAPI = null;
        String platformURL = null;

        try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL)) {
            String sql = "SELECT apiKey, studentId, platformURL FROM userData WHERE discordId = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, discordId);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    studentAPI = resultSet.getString("apiKey");
                    platformURL = resultSet.getString("platformURL");
                    System.out.println("Api key: " + studentAPI + "  studentID: " + resultSet.getString("studentId"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve data from the database.", e);
        }

        List<GradeData.EnrollmentData> enrollments = new ArrayList<>();

        if (studentAPI != null && platformURL != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(jsonContent);

                for (JsonNode enrollmentNode : rootNode) {
                    int courseId = enrollmentNode.path("course").path("id").asInt();
                    double finalScore = enrollmentNode.path("grades").path("final_score").asDouble();
                    double regularScore = enrollmentNode.path("grades").path("current_score").asDouble();

                    // Fetch course name from the Canvas API
                    String courseName = fetchCourseName(courseId, studentAPI, platformURL);

                    enrollments.add(new GradeData.EnrollmentData(courseName, courseId, finalScore, regularScore));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Failed to retrieve API key or platform URL from the database.");
        }

        return enrollments;
    }

    private static String fetchCourseName(int courseId, String studentAPI, String platformURL) {
        String courseName = "";
        try {
            String courseURL = platformURL + "/api/v1/courses/" + courseId + "?access_token=" + studentAPI;
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(courseURL))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(response.body());
                courseName = rootNode.path("name").asText();
            } else {
                System.out.println("Failed to retrieve course information. HTTP Status: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return courseName;
    }

    public static class EnrollmentData {
        private String courseName;
        private int courseId;
        private double finalScore;
        private double regularScore;

        public EnrollmentData(String courseName, int courseId, double finalScore, double regularScore) {
            this.courseName = courseName;
            this.courseId = courseId;
            this.finalScore = finalScore;
            this.regularScore = regularScore;
        }

        public String getCourseName() {
            return courseName;
        }

        public int getCourseId() {
            return courseId;
        }

        public double getFinalScore() {
            return finalScore;
        }

        public double getRegularScore() {
            return regularScore;
        }
    }
}
