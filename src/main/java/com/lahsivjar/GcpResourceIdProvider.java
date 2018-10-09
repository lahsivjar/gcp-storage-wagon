package com.lahsivjar;

import org.apache.maven.wagon.repository.Repository;

import java.util.Optional;

interface GcpResourceIdProvider {
    Optional<GcpResourceId> get(Repository repository);
}
