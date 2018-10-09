package com.lahsivjar;

import org.apache.maven.wagon.repository.Repository;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.NoSuchElementException;

public class EnvVariableBasedGcpResourceIdProviderTest {

    @Test
    public void testGet() {
        final EnvVariableBasedGcpResourceIdProvider spyProvider =
                Mockito.spy(new EnvVariableBasedGcpResourceIdProvider());
        final Repository mockRepository = Mockito.mock(Repository.class);
        Mockito.when(mockRepository.getHost()).thenReturn("test-bucket");

        Mockito.when(spyProvider.getProjectIdFromEnv()).thenReturn("test-project-id");
        final GcpResourceId gcpResourceId = spyProvider.get(mockRepository).get();
        Assert.assertEquals("test-project-id", gcpResourceId.getProjectId());
        Assert.assertEquals("test-bucket", gcpResourceId.getBucket());
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetNull() {
        final EnvVariableBasedGcpResourceIdProvider spyProvider =
                Mockito.spy(new EnvVariableBasedGcpResourceIdProvider());
        final Repository mockRepository = Mockito.mock(Repository.class);
        Mockito.when(mockRepository.getHost()).thenReturn("test-bucket");

        spyProvider.get(mockRepository).get();
    }

}
