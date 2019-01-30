package com.lahsivjar;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.apache.maven.wagon.repository.Repository;

import java.util.Optional;

final class HashSeparatedGcpResourceIdProvider implements GcpResourceIdProvider {

    private static final String SEPARATOR = "#";
    private static final String INTERPOLATE_PREFIX = "${env.";
    private static final String INTERPOLATE_SUFFIX = "}";

    @Override
    public Optional<GcpResourceId> get(Repository repository) {
        final String host = repository.getHost();
        if (!Strings.isNullOrEmpty(host) && host.contains(SEPARATOR)) {
            final String[] processedHost = host.split(SEPARATOR);
            final String projectId = tryInterpolate(processedHost[0]);
            final String bucket = tryInterpolate(processedHost[1]);

            return Optional.of(new GcpResourceId(projectId, bucket));
        }
        return Optional.empty();
    }

    private String tryInterpolate(String source) {
        if (source.startsWith(INTERPOLATE_PREFIX)
                && source.endsWith(INTERPOLATE_SUFFIX)) {
            final int startIndex = INTERPOLATE_PREFIX.length();
            final int endIndex = source.length() - 1;

            final String envSource = source.substring(startIndex, endIndex);
            return getFromEnv(envSource);
        }
        return source;
    }

    @VisibleForTesting
    String getFromEnv(String source) {
        return System.getenv(source);
    }

}
