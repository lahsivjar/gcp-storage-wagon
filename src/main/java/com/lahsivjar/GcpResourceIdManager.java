package com.lahsivjar;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.apache.maven.wagon.repository.Repository;

import java.util.Optional;
import java.util.stream.Stream;

public final class GcpResourceIdManager {

    private static final GcpResourceIdManager INSTANCE = new GcpResourceIdManager();
    private final ImmutableList<GcpResourceIdProvider> projectIdProviders;

    private GcpResourceIdManager() {
        // Order in the list defines priority
        projectIdProviders = ImmutableList.of(
                new HashSeparatedGcpResourceIdProvider()
        );
    }

    public static GcpResourceIdManager getInstance() {
        return INSTANCE;
    }

    public GcpResourceId get(Repository repository) throws NoProjectIdFoundException, NoBucketFoundException {
        final GcpResourceId gcpResourceId =  getProviders().stream()
                .map(m -> m.get(repository))
                .filter(Optional::isPresent)
                .flatMap(t -> Stream.of(t.get()))
                .findFirst()
                .orElseThrow(NoProjectIdFoundException::new);

        if (Strings.isNullOrEmpty(gcpResourceId.getBucket())) {
            throw new NoBucketFoundException();
        }
        return gcpResourceId;
    }

    @VisibleForTesting
    ImmutableList<GcpResourceIdProvider> getProviders() {
        return this.projectIdProviders;
    }

}
