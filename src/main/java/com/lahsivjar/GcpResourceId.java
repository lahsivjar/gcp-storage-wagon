package com.lahsivjar;

final class GcpResourceId {
    private final String projectId;
    private final String bucket;

    GcpResourceId(String projectId, String bucket) {
        this.projectId = projectId;
        this.bucket = bucket;
    }

    String getProjectId() {
        return this.projectId;
    }

    String getBucket() {
        return this.bucket;
    }
}
