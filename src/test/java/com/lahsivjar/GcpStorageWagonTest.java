package com.lahsivjar;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.repository.Repository;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class GcpStorageWagonTest {

    @Rule
    public TemporaryFolder m2EmulatedFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder sourceFolder = new TemporaryFolder();

    // Defined in LocalStorageHelper
    private static final String DUMMY_PROJECT_ID = "dummy-project-for-testing";
    private static final String DUMMY_BUCKET = "fake-dummy-bucket";
    private static final String DUMMY_BASE_DIR = "snapshot/";
    private static final String DUMMY_FILE_NAME = "dummy.txt";
    private static final String DUMMY_FILE_CONTENT = "ereh saw namrepus";

    private Storage fakeStorage() {
        return LocalStorageHelper.getOptions().getService();
    }

    private Repository fakeRepository() {
        final Repository mockRepo = Mockito.mock(Repository.class);
        Mockito.when(mockRepo.getHost()).thenReturn(DUMMY_PROJECT_ID + "#" + DUMMY_BUCKET);
        // Passing bad base dir here since it should normalize to snapshot/
        Mockito.when(mockRepo.getBasedir()).thenReturn("/snapshot");

        return mockRepo;
    }

    private void writeContentToFile(File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))){
            writer.write(DUMMY_FILE_CONTENT);
        }
    }

    private void putFileUtil(GcpStorageWagon storageWagon, String destinationPath) throws IOException, ConnectionException,
            AuthenticationException, AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        final File sourceFile = sourceFolder.newFile(DUMMY_FILE_NAME);

        writeContentToFile(sourceFile);
        storageWagon.connect(fakeRepository());
        storageWagon.put(sourceFile, destinationPath);
    }

    @Test
    public void testProjectIdResolution() throws ConnectionException, AuthenticationException {
        final GcpStorageWagon storageWagon = new GcpStorageWagon();
        storageWagon.connect(fakeRepository());

        Assert.assertEquals(storageWagon.getProjectId(), DUMMY_PROJECT_ID);
    }

    @Test
    public void testBucketResolution() throws ConnectionException, AuthenticationException {
        final GcpStorageWagon storageWagon = new GcpStorageWagon();
        storageWagon.connect(fakeRepository());

        Assert.assertEquals(storageWagon.getBucket(), DUMMY_BUCKET);
    }

    @Test
    public void testBaseDir() throws ConnectionException, AuthenticationException {
        final GcpStorageWagon storageWagon = new GcpStorageWagon();
        storageWagon.connect(fakeRepository());

        Assert.assertEquals(DUMMY_BASE_DIR, storageWagon.getBaseDir());
    }

    @Test(expected = ConnectionException.class)
    public void testInvalidHost() throws ConnectionException, AuthenticationException {
        final GcpStorageWagon storageWagon = new GcpStorageWagon();
        final Repository repository = fakeRepository();
        Mockito.when(repository.getHost()).thenReturn("invalid-repository");

        storageWagon.connect(repository);
    }

    @Test
    public void testPut() throws IOException, ConnectionException, AuthenticationException,
            AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        final Storage storage = fakeStorage();
        final GcpStorageWagon storageWagon = new GcpStorageWagon(storage);
        putFileUtil(storageWagon, DUMMY_FILE_NAME);

        Blob blob = storage.get(BlobId.of(DUMMY_BUCKET, DUMMY_BASE_DIR + DUMMY_FILE_NAME));
        Assert.assertNotNull(blob);
        Assert.assertTrue(blob.exists());
    }

    @Test
    public void testGet() throws IOException, ConnectionException, AuthenticationException,
            AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        final Storage storage = fakeStorage();
        final GcpStorageWagon storageWagon = new GcpStorageWagon(storage);
        putFileUtil(storageWagon, DUMMY_FILE_NAME);

        final File localDestinationFile = new File(m2EmulatedFolder.getRoot().getPath() + "/" + DUMMY_FILE_NAME);
        Assert.assertFalse(localDestinationFile.exists());
        storageWagon.get(DUMMY_FILE_NAME, localDestinationFile);
        Assert.assertTrue(localDestinationFile.exists());
    }

    @Test
    public void testGetIfNewerTrue() throws IOException, ConnectionException, AuthenticationException,
            AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        final Storage spyStorage = Mockito.spy(fakeStorage());
        final GcpStorageWagon storageWagon = new GcpStorageWagon(spyStorage);
        putFileUtil(storageWagon, DUMMY_FILE_NAME);

        final File localDestinationFile = new File(m2EmulatedFolder.getRoot().getPath() + "/" + DUMMY_FILE_NAME);
        Assert.assertFalse(localDestinationFile.exists());

        // Currently LocalStorage doesn't handle update time so mock it
        final BlobId blobId = BlobId.of(storageWagon.getBucket(), storageWagon.getBaseDir() + DUMMY_FILE_NAME);
        final Blob spyBlob = Mockito.spy(spyStorage.get(blobId));
        Mockito.when(spyBlob.getUpdateTime()).thenReturn(System.currentTimeMillis());
        Mockito.when(spyStorage.get(blobId)).thenReturn(spyBlob);

        final boolean newer = storageWagon.getIfNewer(DUMMY_FILE_NAME, localDestinationFile, 0L);

        Assert.assertTrue(newer);
        Assert.assertTrue(localDestinationFile.exists());
    }

    @Test
    public void testGetIfNewerFalse() throws IOException, ConnectionException, AuthenticationException,
            AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        final Storage spyStorage = Mockito.spy(fakeStorage());
        final GcpStorageWagon storageWagon = new GcpStorageWagon(spyStorage);
        putFileUtil(storageWagon, DUMMY_FILE_NAME);

        final File localDestinationFile = new File(m2EmulatedFolder.getRoot().getPath() + "/" + DUMMY_FILE_NAME);
        Assert.assertFalse(localDestinationFile.exists());

        // Currently LocalStorage doesn't handle update time so mock it
        final BlobId blobId = BlobId.of(storageWagon.getBucket(), storageWagon.getBaseDir() + DUMMY_FILE_NAME);
        final Blob spyBlob = Mockito.spy(spyStorage.get(blobId));
        Mockito.when(spyBlob.getUpdateTime()).thenReturn(0L);
        Mockito.when(spyStorage.get(blobId)).thenReturn(spyBlob);

        final boolean newer = storageWagon.getIfNewer(DUMMY_FILE_NAME, localDestinationFile, System.currentTimeMillis());

        Assert.assertFalse(newer);
        Assert.assertFalse(localDestinationFile.exists());
    }

    @Test
    public void testGetIfNewerNull() throws IOException, ConnectionException, AuthenticationException,
            AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        final Storage spyStorage = Mockito.spy(fakeStorage());
        final GcpStorageWagon storageWagon = new GcpStorageWagon(spyStorage);
        putFileUtil(storageWagon, DUMMY_FILE_NAME);

        final File localDestinationFile = new File(m2EmulatedFolder.getRoot().getPath() + "/" + DUMMY_FILE_NAME);
        Assert.assertFalse(localDestinationFile.exists());

        // This is the default behavior of LocalStorage's fake rpc implementation i.e. it gives null update timestamp
        final boolean newer = storageWagon.getIfNewer(DUMMY_FILE_NAME, localDestinationFile, System.currentTimeMillis());

        Assert.assertTrue(newer);
        Assert.assertTrue(localDestinationFile.exists());

    }

    @Test
    public void testResourceExists() throws ConnectionException, AuthenticationException,
            IOException, AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        final Storage storage = fakeStorage();
        final GcpStorageWagon storageWagon = new GcpStorageWagon(storage);
        final File sourceFile = sourceFolder.newFile(DUMMY_FILE_NAME);

        writeContentToFile(sourceFile);
        storageWagon.connect(fakeRepository());
        Assert.assertFalse(storageWagon.resourceExists(DUMMY_FILE_NAME));

        storageWagon.put(sourceFile, DUMMY_FILE_NAME);
        Assert.assertTrue(storageWagon.resourceExists(DUMMY_FILE_NAME));
    }

    @Test
    public void testGetFileListAndPutDirectory() throws ConnectionException, AuthenticationException,
            IOException, AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        // Prepare folder
        // - com/1.txt
        // - com/2.txt
        final Storage storage = fakeStorage();
        final GcpStorageWagon storageWagon = new GcpStorageWagon(storage);
        final List<String> expectedFiles = Arrays.asList("1.txt", "2.txt");
        final File comFolder = sourceFolder.newFolder("com");

        for (String file : expectedFiles) {
            final File fileObj = new File(comFolder.getPath(), file);
            writeContentToFile(fileObj);
        }

        storageWagon.connect(fakeRepository());
        storageWagon.putDirectory(comFolder, "com");

        Assert.assertTrue(storageWagon.resourceExists("com/1.txt"));
        Assert.assertTrue(storageWagon.resourceExists("com/2.txt"));

        final List<String> actualFiles = storageWagon.getFileList("com");

        Assert.assertSame(actualFiles.size(), 2);
    }

    @Test
    public void testDisconnect() throws ConnectionException {
        final GcpStorageWagon spyStorageWagon = Mockito.spy(new GcpStorageWagon(fakeStorage()));
        spyStorageWagon.disconnect();
        Mockito.verify(spyStorageWagon).disconnectInternal();
    }

}
