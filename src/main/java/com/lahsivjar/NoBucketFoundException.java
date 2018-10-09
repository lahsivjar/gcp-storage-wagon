package com.lahsivjar;

import org.apache.maven.wagon.ConnectionException;

public class NoBucketFoundException extends ConnectionException {

    public NoBucketFoundException() {
        super("Connection cannot be established without a storage bucket");
    }

}
