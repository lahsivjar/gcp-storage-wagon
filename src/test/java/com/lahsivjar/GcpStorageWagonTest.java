package com.lahsivjar;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;
import com.google.common.io.Files;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
            // A few kbs worth of file
            for (int i = 0; i < 2048; i++) {
                writer.write(DUMMY_FILE_CONTENT);
            }
        }
    }

    private void writeContentToFile(File file, int size) throws IOException {
        final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        randomAccessFile.setLength(size);
    }

    private void putFileUtil(GcpStorageWagon storageWagon, String destinationPath) throws IOException, ConnectionException,
            AuthenticationException, AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        final Path fullPath = Paths.get(destinationPath);
        final File sourceFile = sourceFolder.newFile(fullPath.getFileName().toString());

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
    public void testPutMultiple() throws InterruptedException {
        final int threadCount = 20;

        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch simultaneousStartLatch = new CountDownLatch(threadCount);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>(threadCount));

        final Storage storage = fakeStorage();
        final GcpStorageWagon storageWagon = new GcpStorageWagon(storage);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    simultaneousStartLatch.countDown();
                    simultaneousStartLatch.await();

                    final String destPath = UUID.randomUUID().toString() + "/" +  UUID.randomUUID().toString() + DUMMY_FILE_NAME;
                    putFileUtil(storageWagon, destPath);

                    final Blob blob = storage.get(BlobId.of(DUMMY_BUCKET, DUMMY_BASE_DIR + destPath));
                    Assert.assertNotNull(blob);
                    Assert.assertTrue(blob.exists());
                } catch (Exception e) {
                    e.printStackTrace();
                    exceptions.add(e);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        finishLatch.await();

        Assert.assertTrue("Exception occurred during put files", exceptions.isEmpty());
    }
  
    @Test
    public void testPut_fileGreaterThan1MB() throws IOException, ConnectionException, AuthenticationException,
            AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        final Storage storage = fakeStorage();
        final GcpStorageWagon storageWagon = new GcpStorageWagon(storage);
        final File sourceFile = sourceFolder.newFile(DUMMY_FILE_NAME);

        writeContentToFile(sourceFile, 1024 * 1024);
        storageWagon.connect(fakeRepository());
        storageWagon.put(sourceFile, DUMMY_FILE_NAME);

        Blob blob = storage.get(BlobId.of(DUMMY_BUCKET, DUMMY_BASE_DIR + DUMMY_FILE_NAME));
        Assert.assertNotNull(blob);
        Assert.assertTrue(blob.exists());
    }

    @Test(expected = ResourceDoesNotExistException.class)
    public void testPutNoResource() throws IOException, ConnectionException, AuthenticationException,
            AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        final Storage storage = fakeStorage();
        final GcpStorageWagon storageWagon = new GcpStorageWagon(storage);
        storageWagon.connect(fakeRepository());
        storageWagon.put(new File("does-not-exist"), "will-fail-before");
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

    @Test(expected = ResourceDoesNotExistException.class)
    public void testGetNoResource() throws IOException, ConnectionException, AuthenticationException,
            AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        final Storage storage = fakeStorage();
        final GcpStorageWagon storageWagon = new GcpStorageWagon(storage);
        storageWagon.connect(fakeRepository());

        final File localDestinationFile = new File(m2EmulatedFolder.getRoot().getPath() + "/" + DUMMY_FILE_NAME);
        storageWagon.get(DUMMY_FILE_NAME, localDestinationFile);
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

    @Test(expected = ResourceDoesNotExistException.class)
    public void testGetIfNewerNoResource() throws IOException, ConnectionException, AuthenticationException,
            AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        final Storage storage = fakeStorage();
        final GcpStorageWagon storageWagon = new GcpStorageWagon(storage);
        storageWagon.connect(fakeRepository());

        final File localDestinationFile = new File(m2EmulatedFolder.getRoot().getPath() + "/" + DUMMY_FILE_NAME);
        storageWagon.getIfNewer(DUMMY_FILE_NAME, localDestinationFile, System.currentTimeMillis());
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
