package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserData {

    private String internalID;
    private String name;
    private String shortName;

    public UserData(String internalID, String name, String shortName) {
        this.internalID = internalID;
        this.name = name;
        this.shortName = shortName;
    }

    public String getInternalID() {
        return internalID;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public static List<UserData> parseJSONUserData(String jsonContent) {
        List<UserData> profiles = new ArrayList<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonContent);

            // Iterate over each JSON object in the array
            for (JsonNode profileNode : rootNode) {
                String internalID = profileNode.path("internalID").asText();
                String name = profileNode.path("name").asText();
                String shortName = profileNode.path("shortName").asText();

                profiles.add(new UserData(internalID, name, shortName));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return profiles;
    }
}
