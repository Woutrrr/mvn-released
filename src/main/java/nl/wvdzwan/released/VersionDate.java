package nl.wvdzwan.released;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.google.gson.annotations.SerializedName;

public class VersionDate {

    @SerializedName("number")
    private final String version;

    @SerializedName("published_at")
    private final LocalDateTime lastModified;

    public VersionDate(String version, long lastModified) {
        this.version = version;
        this.lastModified = LocalDateTime.ofEpochSecond(lastModified/1000, 0, ZoneOffset.UTC);
    }
}
