package com.myorg.myproduct.user.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.myproduct.model.MyProductRelease;
import com.myorg.myproduct.model.MyProductReleaseList;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class DataRetriever {

    static public class Version {
        public String version;
        public String buildTime;
    }

    static public class ReleasedVersions {
        public Version latestReleaseSnapshot;
        public Version latestRc;
        public List<Version> finalReleases;
    }

    public static MyProductReleaseList retrieve() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            URL url = new URL("https://raw.githubusercontent.com/gradle/gradle/master/released-versions.json");
            ReleasedVersions versions = objectMapper.readValue(url, ReleasedVersions.class);
            return new MyProductReleaseList(versions.finalReleases.stream().map(r -> new MyProductRelease(
                    r.version, "https://docs.gradle.org/" + r.version + "/release-notes.html")).collect(Collectors.toList()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}