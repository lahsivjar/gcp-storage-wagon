# Maven GCP Storage Wagon [![Build Status](https://travis-ci.com/lahsivjar/gcp-storage-wagon.svg?branch=master)](https://travis-ci.com/lahsivjar/gcp-storage-wagon) [![Maven Central](https://img.shields.io/maven-central/v/com.lahsivjar/gcp-storage-wagon.svg)](https://search.maven.org/artifact/com.lahsivjar/gcp-storage-wagon) [![Coverage Status](https://coveralls.io/repos/github/lahsivjar/gcp-storage-wagon/badge.svg?branch=master)](https://coveralls.io/github/lahsivjar/gcp-storage-wagon?branch=master)

[Maven wagon](https://maven.apache.org/wagon/) provider for [Google Cloud Storage](https://cloud.google.com/storage/). It allows publishing and downloading of artifacts from Google Cloud Storage bucket.

## Usage

1\. To allow the plugin to access the bucket configure access to the GCP by following instructions [here](https://cloud.google.com/docs/authentication/getting-started). The easiest way to do so is by using [IAM Service Accounts](https://cloud.google.com/iam/docs/understanding-service-accounts) and configuring environment variable `GOOGLE_APPLICATION_CREDENTIALS`.

2\. Configure the target project to use maven wagon for handling transport via `pom.xml`:

```xml
  <build>
      <extensions>
          <extension>
              <groupId>com.lahsivjar</groupId>
              <artifactId>gcp-storage-wagon</artifactId>
              <!-- Replace gcp.storage.wagon.version with the correct version -->
              <version>${gcp.storage.wagon.version}</version>
          </extension>
      </extensions>
  </build>
```
Maven loads parent pom before loading any wagon thus if the parent pom also relies on wagon plugin we have to use `.mvn/extensions.xml` file released in [Maven 3.3.1](https://maven.apache.org/docs/3.3.1/release-notes.html) in the root of the target project. Example:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
    <extension>
        <groupId>com.lahsivjar</groupId>
        <artifactId>gcp-storage-wagon</artifactId>
        <!-- Replace gcp.storage.wagon.version with the correct version -->
        <version>${gcp.storage.wagon.version}</version>
    </extension>
</extensions>
```

3\. Setup distribution management in the target project to deploy artifacts to:

```xml
  <distributionManagement>
      <snapshotRepository>
          <id>gcp-bucket-snapshot</id>
          <!-- Replace gcp.project-id and gcp.maven.bucket with correct values -->
          <url>gs://${gcp.project-id}#${gcp.maven.bucket}/snapshot</url>
      </snapshotRepository>
      <repository>
          <id>gcp-bucket-release</id>
          <!-- Replace gcp.project-id and gcp.maven.bucket with correct values -->
          <url>gs://${gcp.project-id}#${gcp.maven.bucket}/release</url>
      </repository>
  </distributionManagement>
```
__NOTE__: Check [Project Id Resolution](#gcp-project-id-resolution) section to find all supported ways of resolving GCP project id.

4\. To use the deployed modules in another project add extension as described in step `2` to the target project and specify a repository with the wagon configured:

```xml
  <repositories>
      <repository>
          <id>gcp-bucket-snapshot</id>
          <!-- Replace gcp.project-id and gcp.maven.bucket with correct values -->
          <url>gs://${gcp.project-id}#${gcp.maven.bucket}/snapshot</url>
      </repository>
      <repository>
          <id>gcp-bucket-release</id>
          <!-- Replace gcp.project-id and gcp.maven.bucket with correct values -->
          <url>gs://${gcp.project-id}#${gcp.maven.bucket}/release</url>
      </repository>
  </repositories>
```
__NOTE__: Check [Project Id Resolution](#gcp-project-id-resolution) section to find all supported ways of resolving GCP project id.

## GCP Project Id resolution
Project id of GCP can be resolved in the following two ways(in decreasing priority):

1. By specifying it as part of the repository url. For example: `gs://${gcp.project-id}#${gcp.maven.bucket}/`
2. By setting environment variable `WAGON_GCP_PROJECT_ID`. In this case the url must be of the form `gs://${gcp.maven.bucket}/...`

__NOTE__: It is priority based so if project id can be resolved via a higher priority resolver then the lower priority won't be considered

## Environment variable interpolation
Hash based project id resolution scheme supports environment variable interpolation by specifying the repository url as `gs://${env.GCP_PROJECT_ID}#${env.BUCKET_NAME}` with `GCP_PROJECT_ID` and `BUCKET_NAME` exported as environment variables.

## Issues

Report any issues or bugs to https://github.com/lahsivjar/gcp-storage-wagon/issues

## Changelog
### 2.1
* (Bugfix #19) Handle small files in single blob request

### 2.0
* Environment variable interpolation for hash separated project id resolution

### 1.0
* Add support for environment variable based project id resolution

### 0.1
* Add basic wagon for GCP storage
* Support resolving project id via the repository url

## License
The project is licensed under Apache-2.0 - see the [LICENSE](LICENSE) file for details.
