# gcp-storage-wagon

[![Build Status](https://travis-ci.com/lahsivjar/gcp-storage-wagon.svg?branch=master)](https://travis-ci.com/lahsivjar/gcp-storage-wagon) [![Maven Central](https://img.shields.io/maven-central/v/com.lahsivjar/gcp-storage-wagon.svg)](https://img.shields.io/maven-central/v/com.google.cloud/google-cloud-nio.svg)

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
        <version>${gcp.storage.wagon.version}</version>
    </extension>
</extensions>
```
NOTE: Replace `gcp.storage.wagon.version` with the correct version.

3\. Setup distribution management in the target project to deploy artifacts to:

```xml
  <distributionManagement>
      <snapshotRepository>
          <id>gcp-bucket-snapshot</id>
          <url>gs://${gcp.project-id}#${gcp.maven.bucket}/snapshot</url>
      </snapshotRepository>
      <repository>
          <id>gcp-bucket-release</id>
          <url>gs://${gcp.project-id}#${gcp.maven.bucket}/release</url>
      </repository>
  </distributionManagement>
```
NOTE: the url must be of the form `gs://<gcp-poject-id>#<target-bucket>`

4\. To use the deployed modules in another project add extension as described in step `2` to the target project and specify a repository with the wagon configured:

```xml
  <repositories>
      <repository>
          <id>gcp-bucket-snapshot</id>
          <url>gs://${gcp.project-id}#${gcp.maven.bucket}/snapshot</url>
      </repository>
      <repository>
          <id>gcp-bucket-release</id>
          <url>gs://${gcp.project-id}#${gcp.maven.bucket}/release</url>
      </repository>
  </repositories>
```

## Issues

Report any issues or bugs to https://github.com/lahsivjar/gcp-storage-wagon/issues

## Changelog
### 0.1
* Add basic wagon for GCP storage
* Support resolving project id via the repository url

## License
The project is licensed under Apache-2.0 - see the [LICENSE](LICENSE) file for details.
