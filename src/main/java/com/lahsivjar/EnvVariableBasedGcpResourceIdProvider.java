package com.lahsivjar;

import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.wagon.repository.Repository;

import java.util.Optional;

final class EnvVariableBasedGcpResourceIdProvider implements GcpResourceIdProvider {

    static String WAGON_GCP_PROJECT_ID = "WAGON_GCP_PROJECT_ID";

    @Override
    public Optional<GcpResourceId> get(Repository repository) {
        final String projectId = getProjectIdFromEnv();
        if(projectId != null) {
            return Optional.of(new GcpResourceId(projectId, repository.getHost()));
        }
        return Optional.empty();
    }

    @VisibleForTesting
    String getProjectIdFromEnv() {
        return System.getenv(WAGON_GCP_PROJECT_ID);
    }

}
