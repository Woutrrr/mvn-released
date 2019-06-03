package nl.wvdzwan.released;

import java.util.List;

public class Project {

    private String name;

    private List<VersionDate> versions;

    public Project(String name, List<VersionDate> versions) {
        this.name = name;
        this.versions = versions;
    }
}
