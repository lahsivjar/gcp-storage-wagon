package com.lahsivjar;

import org.apache.maven.wagon.ConnectionException;

public class NoProjectIdFoundException extends ConnectionException {

    public NoProjectIdFoundException() {
        super("Connection cannot be established without a project id");
    }

}
