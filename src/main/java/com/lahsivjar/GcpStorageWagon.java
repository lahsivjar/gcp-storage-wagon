package com.lahsivjar;

import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.TransportOptions;
import com.google.cloud.WriteChannel;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.storage.*;
import com.google.common.annotations.VisibleForTesting;
import org.apache.http.client.HttpClient;
import org.apache.maven.wagon.*;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GcpStorageWagon extends AbstractWagon {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpStorageWagon.class);

    private Storage storage;
    private String bucket;
    private String projectId;
    private String baseDir;

    public GcpStorageWagon() {

    }

    @VisibleForTesting
    GcpStorageWagon(Storage storage) {
        this.storage = storage;
    }

    @Override
    void connectInternal() throws ConnectionException, AuthenticationException {
        final Repository repository = getRepository();
        final String host = repository.getHost();
        final String[] projectIdAndBucket = host.split("#");
        if (projectIdAndBucket.length != 2) {
            throw new ConnectionException(
                    String.format("Cannot resolve project id or bucket name, expected format is project-id#bucket but received %s", host));
        }
        this.projectId = projectIdAndBucket[0];
        this.bucket = projectIdAndBucket[1];

        String baseDir = repository.getBasedir().substring(1);
        if (!baseDir.endsWith("/")) {
            baseDir += "/";
        }
        this.baseDir = baseDir;

        LOGGER.debug("Initiating connection to GCP storage using project id {} and to bucket {} with base directory {}",
                this.projectId, this.bucket, this.baseDir);

        if (this.storage == null) {
            final StorageOptions storageOptions = buildStorageOptions();
            this.storage = storageOptions.getService();
        }
    }

    @Override
    void disconnectInternal() throws ConnectionException {
        this.baseDir = null;
        this.projectId = null;
        this.bucket = null;
        this.storage = null;
    }

    @Override
    public void get(String resourceName, File destination)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource resource = new Resource(resourceName);

        final String fullResourceName = getKey(resourceName);
        LOGGER.debug("Getting resource {} and putting to destination {}", fullResourceName, destination);

        createParentDirectories(destination);
        fireGetInitiated(resource, destination);

        final Blob blob = getBlob(fullResourceName);

        if (blob == null) {
            throw new ResourceDoesNotExistException(String.format("%s does not exist", resourceName));
        }

        fireGetStarted(resource, destination);
        downloadInternal(resource, blob, destination);
        fireGetCompleted(resource, destination);
    }

    @Override
    public boolean getIfNewer(String resourceName, File destination, long timestamp)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        final String fullResourceName = getKey(resourceName);
        final Blob blob = getBlob(fullResourceName);

        if (blob == null) {
            throw new ResourceDoesNotExistException(String.format("%s does not exist", fullResourceName));
        }

        final Long remoteTimestamp = blob.getUpdateTime();
        final boolean isNewer;
        if (remoteTimestamp == null) {
            LOGGER.warn("Resource {} has null timestamp, forcing download", resourceName);
            isNewer = true;
        } else {
            isNewer = remoteTimestamp > timestamp;
            LOGGER.debug("Resource {} timestamped as {} on remote vs {} on local",
                    fullResourceName, remoteTimestamp, timestamp);
        }

        if (isNewer) {
            get(resourceName, destination);
            return true;
        }
        return false;
    }

    @Override
    public void put(File source, String destination)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        final Resource resource = new Resource(destination);
        resource.setContentLength(source.length());

        final String fullResourceName = getKey(destination);
        LOGGER.debug("Putting resource {} to destination {}", source, fullResourceName);

        firePutInitiated(resource, source);

        final BlobId blobId = BlobId.of(this.bucket, fullResourceName);
        final String contentType = URLConnection.guessContentTypeFromName(source.getName());
        final BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();
        firePutStarted(resource, source);
        final TransferEvent transferProgressEvent = buildTransferProgressEvent(resource, TransferEvent.REQUEST_PUT);
        try (final WriteChannel writer = storage.writer(blobInfo)) {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            try (InputStream input = new FileInputStream(source)) {
                int limit;
                while ((limit = input.read(buffer)) >= 0) {
                    writer.write(ByteBuffer.wrap(buffer, 0, limit));
                    fireTransferProgress(transferProgressEvent, buffer, limit);
                }
            }
        } catch (FileNotFoundException fe) {
            throw new ResourceDoesNotExistException(String.format("%s does not exist", source.getName()), fe);
        } catch (IOException e) {
            throw new TransferFailedException(String.format("Failed to transfer %s to %s",
                    source.getName(), destination), e);
        } catch (StorageException se) {
            throw new TransferFailedException(String.format("Failed to transfer %s to %s",
                    source.getName(), destination), se);
        }
        firePutCompleted(resource, source);
    }

    @Override
    public boolean resourceExists(String resource) throws TransferFailedException, AuthorizationException {
        final Blob blob = getBlob(getKey(resource));

        if (blob == null) {
            return false;
        }

        return true;
    }

    @Override
    public List<String> getFileList(String prefix)
            throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        final String fullPrefix = getKey(prefix);
        final Page<Blob> page = this.storage.list(this.bucket, Storage.BlobListOption.prefix(fullPrefix));
        final Iterable<Blob> blobIterable = page.iterateAll();
        final List<String> allFileList = new ArrayList<>();

        blobIterable.forEach(b -> allFileList.add(b.getName()));
        return allFileList;
    }

    @Override
    public void putDirectory(File sourceDir, String destDir) throws
            TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                put(file, destDir + "/" + file.getName());
            }
        }
    }

    private Blob getBlob(final String resource) throws TransferFailedException {
        final BlobId blobId = BlobId.of(this.bucket, resource);
        final Blob blob;
        try {
            blob = this.storage.get(blobId);
        } catch (StorageException e) {
            throw new TransferFailedException(String.format("Failed to read %s", resource), e);
        }
        return blob;
    }

    private void downloadInternal(Resource resource, Blob blob, File destination) throws TransferFailedException {
        final TransferEvent transferProgressEvent = buildTransferProgressEvent(resource, TransferEvent.REQUEST_GET);
        try (OutputStream outputStream = new FileOutputStream(destination);
             ReadChannel reader = this.storage.reader(blob.getBlobId())) {
            WritableByteChannel channel = Channels.newChannel(outputStream);
            ByteBuffer bytes = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
            int limit;
            while ((limit = reader.read(bytes)) > 0) {
                bytes.flip();
                channel.write(bytes);
                fireTransferProgress(transferProgressEvent, bytes.array(), limit);
                bytes.clear();
            }
        } catch (FileNotFoundException fe) {
            throw new TransferFailedException(String.format("Failed to write to %s", destination));
        } catch (IOException e) {
            throw new TransferFailedException(String.format("Failed to read from %s and write to %s", resource, destination));
        }
    }

    private String getKey(String resource) {
        return this.baseDir + resource;
    }

    private TransportOptions buildTransportOptions() {
        final HttpClient client = ApacheHttpTransport.newDefaultHttpClient();
        return HttpTransportOptions.newBuilder()
                .setConnectTimeout(getTimeout())
                .setReadTimeout(getReadTimeout())
                .setHttpTransportFactory(() -> new ApacheHttpTransport(client))
                .build();
    }

    private StorageOptions buildStorageOptions() {
        return StorageOptions.newBuilder()
                .setTransportOptions(buildTransportOptions())
                .setProjectId(this.projectId)
                .build();
    }

    private TransferEvent buildTransferProgressEvent(Resource resource, int requestType) {
        return new TransferEvent(this, resource, TransferEvent.TRANSFER_PROGRESS, requestType);
    }

    @VisibleForTesting
    String getProjectId() {
        return this.projectId;
    }

    @VisibleForTesting
    String getBucket() {
        return this.bucket;
    }

    @VisibleForTesting
    String getBaseDir() {
        return this.baseDir;
    }

}
