package com.lahsivjar;

import com.google.common.base.Strings;
import org.apache.maven.wagon.repository.Repository;

import java.util.Optional;

final class HashSeparatedGcpResourceIdProvider implements GcpResourceIdProvider {

    private static final String SEPARATOR = "#";

    @Override
    public Optional<GcpResourceId> get(Repository repository) {
        final String host = repository.getHost();
        if (!Strings.isNullOrEmpty(host) && host.contains(SEPARATOR)) {
            final String[] processedHost = host.split(SEPARATOR);
            return Optional.of(new GcpResourceId(processedHost[0], processedHost[1]));
        }
        return Optional.empty();
    }

}
