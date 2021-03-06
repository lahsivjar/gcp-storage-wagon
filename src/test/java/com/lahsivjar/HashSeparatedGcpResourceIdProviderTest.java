package com.lahsivjar;

import org.apache.maven.wagon.repository.Repository;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.NoSuchElementException;

public class HashSeparatedGcpResourceIdProviderTest {

    @Test
    public void testGet() {
        final HashSeparatedGcpResourceIdProvider provider = new HashSeparatedGcpResourceIdProvider();
        final Repository mockRepository = Mockito.mock(Repository.class);
        Mockito.when(mockRepository.getHost()).thenReturn("test-project-id#test-bucket");

        final GcpResourceId gcpResourceId = provider.get(mockRepository).get();
        Assert.assertEquals("test-project-id", gcpResourceId.getProjectId());
        Assert.assertEquals("test-bucket", gcpResourceId.getBucket());
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetNull() {
        final HashSeparatedGcpResourceIdProvider provider = new HashSeparatedGcpResourceIdProvider();
        final Repository mockRepository = Mockito.mock(Repository.class);
        Mockito.when(mockRepository.getHost()).thenReturn("repository-url");

        provider.get(mockRepository).get();
    }

    @Test
    public void testInterpolation() {
        final HashSeparatedGcpResourceIdProvider spyProvider =
                Mockito.spy(new HashSeparatedGcpResourceIdProvider());
        final Repository mockRepository = Mockito.mock(Repository.class);
        Mockito.when(mockRepository.getHost()).thenReturn("${env.GCP_PROJECT_ID}#${env.BUCKET_NAME}");

        Mockito.when(spyProvider.getFromEnv("GCP_PROJECT_ID")).thenReturn("test-project-id");
        Mockito.when(spyProvider.getFromEnv("BUCKET_NAME")).thenReturn("test-bucket");

        final GcpResourceId gcpResourceId = spyProvider.get(mockRepository).get();
        Assert.assertEquals("test-project-id", gcpResourceId.getProjectId());
        Assert.assertEquals("test-bucket", gcpResourceId.getBucket());
    }

}
