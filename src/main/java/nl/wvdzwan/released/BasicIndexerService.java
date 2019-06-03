package nl.wvdzwan.released;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.Field;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.index.updater.WagonHelper;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public class BasicIndexerService {

    private final PlexusContainer plexusContainer;

    private final Indexer indexer;

    private final IndexUpdater indexUpdater;

    private final Wagon httpWagon;

    private IndexingContext centralContext;

    private static File centralLocalCache = new File("index-data/central-cache");
    private static File centralIndexDir = new File("index-data/central-index");

    public BasicIndexerService()
            throws PlexusContainerException, ComponentLookupException {
        // here we create Plexus container, the Maven default IoC container
        // Plexus falls outside of MI scope, just accept the fact that
        // MI is a Plexus component ;)
        // If needed more info, ask on Maven Users list or Plexus Users list
        // google is your friend!
        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();
        config.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
        this.plexusContainer = new DefaultPlexusContainer(config);

        // lookup the indexer components from plexus
        this.indexer = plexusContainer.lookup(Indexer.class);
        this.indexUpdater = plexusContainer.lookup(IndexUpdater.class);
        // lookup wagon used to remotely fetch index
        this.httpWagon = plexusContainer.lookup(Wagon.class, "http");

    }

    public void initialize()
            throws IOException, ComponentLookupException {
        // Files where local cache is (if any) and Lucene Index should be located


        // Creators we want to use (search for fields it defines)
        List<IndexCreator> indexers = new ArrayList<>();
        indexers.add(plexusContainer.lookup(IndexCreator.class, "min"));
        indexers.add(plexusContainer.lookup(IndexCreator.class, "jarContent"));
        indexers.add(plexusContainer.lookup(IndexCreator.class, "maven-plugin"));

        // Create context for central repository index
        centralContext =
                indexer.createIndexingContext("central-context", "central", centralLocalCache, centralIndexDir,
                        "http://repo1.maven.org/maven2", null, true, true, indexers);

    }

    public boolean updateIndex() throws IOException {

        System.out.println("Updating Index...");
        System.out.println("This might take a while on first run, so please be patient!");
        // Create ResourceFetcher implementation to be used with IndexUpdateRequest
        // Here, we use Wagon based one as shorthand, but all we need is a ResourceFetcher implementation
        TransferListener listener = new AbstractTransferListener() {
            public void transferStarted(TransferEvent transferEvent) {
                System.out.print("  Downloading " + transferEvent.getResource().getName());
            }

            public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length) {
            }

            public void transferCompleted(TransferEvent transferEvent) {
                System.out.println(" - Done");
            }
        };
        ResourceFetcher resourceFetcher = new WagonHelper.WagonFetcher(httpWagon, listener, null, null);

        Date centralContextCurrentTimestamp = centralContext.getTimestamp();
        IndexUpdateRequest updateRequest = new IndexUpdateRequest(centralContext, resourceFetcher);
        IndexUpdateResult updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest);
        if (updateResult.isFullUpdate()) {
            System.out.println("Full update happened!");
        } else if (updateResult.getTimestamp().equals(centralContextCurrentTimestamp)) {
            System.out.println("No update needed, index is up to date!");
        } else {
            System.out.println(
                    "Incremental update happened, change covered " + centralContextCurrentTimestamp + " - "
                            + updateResult.getTimestamp() + " period.");
        }

        System.out.println();
        return true;
    }

    public Set<ArtifactInfo> search(String group, String artifact) throws IOException {

        Query gidQ =
                indexer.constructQuery(MAVEN.GROUP_ID, new SourcedSearchExpression(group));
        Query aidQ = indexer.constructQuery(MAVEN.ARTIFACT_ID, new SourcedSearchExpression(artifact));

        Query classifierQ = indexer.constructQuery( MAVEN.CLASSIFIER, new SourcedSearchExpression(Field.NOT_PRESENT) );


        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        BooleanQuery bq = builder
                .add(gidQ, Occur.MUST)
                .add(aidQ, Occur.MUST)
                .add(classifierQ, Occur.MUST_NOT)
                .build();
        Set<ArtifactInfo> result = searchAndDump(indexer, bq);

        return result;
    }

    public void close() throws IOException {
        // close cleanly
        indexer.closeIndexingContext(centralContext, false);
    }

    private Set<ArtifactInfo> searchAndDump(Indexer nexusIndexer, Query q)
            throws IOException {

        FlatSearchResponse response = nexusIndexer.searchFlat(new FlatSearchRequest(q, centralContext));

        return response.getResults();
    }


}
