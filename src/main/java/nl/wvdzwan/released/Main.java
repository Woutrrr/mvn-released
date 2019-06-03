package nl.wvdzwan.released;


import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.index.ArtifactInfo;
import spark.Request;

import static spark.Spark.get;
import static spark.Spark.halt;

public class Main {
    private static Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws Exception {

        final BasicIndexerService basicIndexerService = new BasicIndexerService();

        basicIndexerService.initialize();

            Gson gson = (new GsonBuilder())
                    .registerTypeAdapter(LocalDateTime.class, new Main.DateTimeSerializer() )
                .create();

        get("/updateIndex", (req, res) -> {

            boolean result = false;
            try {
                result = basicIndexerService.updateIndex();
            } catch (Exception e) {
                halt(500, "Error! " + e.toString());
            }

            return "Done";
        });


        get("/api/maven/:ga", (req, res) -> {

            logRequest(req);


            String ga = req.params("ga");
            String[] parts = ga.split(":");
            if (parts.length != 2) {
                halt(400, "Request should be /api/maven/group:artifact");
            }


            Set<ArtifactInfo> artifactInfos = basicIndexerService.search(parts[0], parts[1]);

            List<VersionDate> versions = artifactInfos.stream()
                    .map(artifactInfo -> new VersionDate(artifactInfo.getVersion(), artifactInfo.getLastModified()))
                    .collect(Collectors.toList());

            logger.info("Found {} versions for {}", versions.size(), ga);

            res.type("application/json");
            return new Project(ga, versions);

        }, gson::toJson );
    }

    private static void logRequest(Request req) {
        logger.info("{} [{}] \"{} {}\" \"{}\"",
                req.ip(),
                LocalDateTime.now(),
                req.requestMethod(),
                req.url(),
                req.userAgent());
    }

    private static class DateTimeSerializer implements JsonSerializer<LocalDateTime> {
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString() + "Z");
        }
    }
}
