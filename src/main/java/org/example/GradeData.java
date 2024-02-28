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


    static List<GradeData.EnrollmentData> fetchCourseData(String discordId, Boolean favoriteFilter) {

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
        int highestTermId = 0; // Initialize highest term ID

        try {
            // Retrieve course data from the platform
            String courseURL = platformURL + "/api/v1/users/self/courses?include[]=total_scores&include[]=term&include[]=favorites&access_token=" + studentAPI;
            System.out.println(courseURL);
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(courseURL))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(response.body());

                // Find the highest term ID across all courses
                for (JsonNode courseNode : rootNode) {
                    JsonNode termNode = courseNode.get("term");
                    if (termNode != null && termNode.has("id")) {
                        int termId = termNode.get("id").asInt();
                        highestTermId = Math.max(highestTermId, termId);
                    }
                }

                // Iterate over each course in the JSON response
                for (JsonNode courseNode : rootNode) {
                    JsonNode termNode = courseNode.get("term");
                    if (termNode != null && termNode.has("id")) {
                        int termId = termNode.get("id").asInt();
                        if (termId < highestTermId) {
                            continue; // Skip this course if its term ID is less than the highest term ID
                        }
                    }

                    favoriteFilter = (favoriteFilter == null) ? false : favoriteFilter;
                    // Check if the favorite filter is active and if the course is a favorite
                    if ((favoriteFilter) && !(courseNode.has("is_favorite") && courseNode.get("is_favorite").asBoolean())) {
                        continue; // Skip this course if the favorite filter is true (or null) and it's not a favorite
                    }

                    // Extract course data
                    String courseName = courseNode.path("name").asText();
                    int courseId = courseNode.path("id").asInt();
                    String courseCode = courseNode.path("course_code").asText();

                    // Check if enrollments exist
                    JsonNode enrollmentsNode = courseNode.get("enrollments");
                    if (enrollmentsNode != null && enrollmentsNode.isArray() && enrollmentsNode.size() > 0) {
                        double courseGrade = enrollmentsNode.get(0).get("computed_current_score").asDouble();
                        enrollments.add(new GradeData.EnrollmentData(courseName, courseId, courseCode, courseGrade));
                    } else {
                        System.out.println("No enrollment data found for course: " + courseName);
                    }
                }
            } else {
                System.out.println("Failed to retrieve course information. HTTP Status: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return enrollments;
    }
    static List<GradeData.CourseData> fetchCourseName(String discordId, Boolean favoriteFilter) {
        String studentAPI = null;
        String platformURL = null;

        try (Connection connection = DriverManager.getConnection(JDBC_DATABASE_URL)) {
            String sql = "SELECT apiKey, platformURL FROM userData WHERE discordId = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, discordId);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    studentAPI = resultSet.getString("apiKey");
                    platformURL = resultSet.getString("platformURL");
                    System.out.println("Api key: " + studentAPI);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve data from the database.", e);
        }

        List<GradeData.CourseData> courses = new ArrayList<>();

        try {
            // Retrieve course data from the platform
            String courseURL = platformURL + "/api/v1/users/self/courses?include[]=total_scores&include[]=term&include[]=favorites&access_token=" + studentAPI;
            System.out.println(courseURL);
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(courseURL))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(response.body());

                // Iterate over each course in the JSON response
                for (JsonNode courseNode : rootNode) {
                    // Extract course data

                    favoriteFilter = (favoriteFilter == null) ? false : favoriteFilter;
                    // Check if the favorite filter is active and if the course is a favorite
                    if ((favoriteFilter) && !(courseNode.has("is_favorite") && courseNode.get("is_favorite").asBoolean())) {
                        continue; // Skip this course if the favorite filter is true (or null) and it's not a favorite
                    }

                    String courseName = courseNode.path("name").asText();
                    int courseId = courseNode.path("id").asInt();
                    courses.add(new CourseData(courseName, courseId));
                }
            } else {
                System.out.println("Failed to retrieve course information. HTTP Status: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return courses;
    }


    public static class EnrollmentData {
        private String courseName;
        private int courseId;
        private String courseCode;
        private double courseGrade;

        public EnrollmentData(String courseName, int courseId, String courseCode, double courseGrade) {
            this.courseName = courseName;
            this.courseId = courseId;
            this.courseCode = courseCode;
            this.courseGrade = courseGrade;
        }

        public String getCourseName() {
            return courseName;
        }

        public int getCourseId() {
            return courseId;
        }

        public String getCourseCode() { return courseCode; }

        public double getCourseGrade() {
            return courseGrade;
        }
    }

    public static class CourseData {
        private String courseName;
        private int courseId;

        public CourseData(String courseName, int courseId) {
            this.courseName = courseName;
            this.courseId = courseId;
        }
        public String getCourseName() {
            return courseName;
        }

        public int getCourseId() {
            return courseId;
        }
    }
}
