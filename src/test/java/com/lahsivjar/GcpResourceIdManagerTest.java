package com.lahsivjar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.maven.wagon.repository.Repository;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class GcpResourceIdManagerTest {

    private final GcpResourceIdManager resourceIdManager = GcpResourceIdManager.getInstance();

    @Test(expected = NoProjectIdFoundException.class)
    public void testNoProjectIdResolved() throws NoProjectIdFoundException, NoBucketFoundException {
        resourceIdManager.get(Mockito.mock(Repository.class));
    }

    @Test(expected = NoBucketFoundException.class)
    public void testBucketResolved() throws NoProjectIdFoundException, NoBucketFoundException {
        // Setup fake repository to return blank indicating no bucket set
        final Repository mockRepository = Mockito.mock(Repository.class);
        Mockito.when(mockRepository.getHost()).thenReturn("");

        // Setup env based gcp resource id provider so that project id can be resolved
        final EnvVariableBasedGcpResourceIdProvider envSpyProvider =
                Mockito.spy(new EnvVariableBasedGcpResourceIdProvider());
        Mockito.when(envSpyProvider.getProjectIdFromEnv()).thenReturn("test-project-id");

        // Setup gcp resource id manager to use above created provider
        final GcpResourceIdManager spyResourceIdManager = Mockito.spy(resourceIdManager);
        Mockito.when(spyResourceIdManager.getProviders()).thenReturn(ImmutableList.of(
                envSpyProvider
        ));

        spyResourceIdManager.get(mockRepository);
    }

    @Test
    public void testProjectIdResolvedViaHashMethod() throws NoProjectIdFoundException, NoBucketFoundException {
        final Repository mockRepository = Mockito.mock(Repository.class);
        Mockito.when(mockRepository.getHost()).thenReturn("test-project-id#test-bucket");

        final GcpResourceId gcpResourceId = resourceIdManager.get(mockRepository);
        Assert.assertEquals("test-project-id", gcpResourceId.getProjectId());
        Assert.assertEquals("test-bucket", gcpResourceId.getBucket());
    }

}
