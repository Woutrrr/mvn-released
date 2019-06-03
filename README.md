# MVN-Released
MVN-Released is a small service that can be queried for the release dates of a Maven Artifact.
It is based on the example code provided by the [maven-indexer](https://maven.apache.org/maven-indexer/) package.

## Setup

- First compile the code to make a jar file: `mvn package`
- Run the just generated jar file: `java -jar released-1.0-jar-with-dependencies.jar`
  The first time the service is started the local index has to be created and an initial update is triggered. 
  This could take a several minutes. Once imported the disk space used is around 2 GB.
- After importing the service is running on port 4567 and is ready to be queried.


## Usage
The service provides two end points. 
One get the release dates of a Maven artifact, and one to update the local index with the latest information for the Maven Central Index.

### Request release dates of packages
Usage:
```
/api/maven/{group:artifact}
```

Where `{artifact name}` should be replaced by the group name and artifact name of an project.
For example the [SLF4J Api module](https://mvnrepository.com/artifact/org.slf4j/slf4j-api) has `org.slf4j` as group name and as artifact name `slf4j-api`. 

_Example_

Request:
```
/api/maven/org.slf4j:slf4j-api
```

Response:
```json
{
  "name":"org.slf4j:slf4j-api",
  "versions":[
      {"number":"1.8.0-beta4","published_at":"2019-02-19T17:29:57Z"},
      {"number":"1.8.0-beta2","published_at":"2018-03-21T22:01:42Z"},
      ....
  ]
}
```


### Update index
Usage:
```
/updateIndex
```

This triggers an update of the local index. 
The Cental Maven index is queried and updates are downloaded and imported into the local index.
This can take a long time, depending on when the local index was last updated.

_Example:_

Request:
```
/updateIndex
```

Response:
```
Done
```
