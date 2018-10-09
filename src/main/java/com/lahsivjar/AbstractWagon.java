package com.lahsivjar;

import org.apache.maven.wagon.*;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.events.*;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

abstract class AbstractWagon implements Wagon {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWagon.class);

    static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private Repository repository;
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    private int readTimeout = DEFAULT_READ_TIMEOUT;
    private boolean interactive = false;
    private final SessionEventSupport sessionEventSupport;
    private final TransferEventSupport transferEventSupport;

    AbstractWagon() {
        this.sessionEventSupport = new SessionEventSupport();
        this.transferEventSupport = new TransferEventSupport();
    }

    AbstractWagon(SessionEventSupport sessionEventSupport, TransferEventSupport transferEventSupport) {
        this.sessionEventSupport = sessionEventSupport;
        this.transferEventSupport = transferEventSupport;
    }

    @Override
    public boolean supportsDirectoryCopy() {
        return true;
    }

    @Override
    public void connect(Repository source) throws ConnectionException, AuthenticationException {
        connect(source, new AuthenticationInfo(), new ProxyInfoProxyProvider(null));
    }

    @Override
    public void connect(Repository source, ProxyInfo proxyInfo) throws ConnectionException, AuthenticationException {
        connect(source, new AuthenticationInfo(), new ProxyInfoProxyProvider(proxyInfo));
    }

    @Override
    public void connect(Repository source, ProxyInfoProvider proxyInfoProvider) throws ConnectionException, AuthenticationException {
        connect(source, new AuthenticationInfo(), proxyInfoProvider);
    }

    @Override
    public void connect(Repository source, AuthenticationInfo authenticationInfo) throws ConnectionException, AuthenticationException {
        connect(source, authenticationInfo, new ProxyInfoProxyProvider(null));
    }

    @Override
    public void connect(Repository source, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo) throws ConnectionException, AuthenticationException {
        connect(source, authenticationInfo, new ProxyInfoProxyProvider(proxyInfo));
    }

    @Override
    public void connect(Repository source, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider) throws ConnectionException, AuthenticationException {
        this.repository = source;
        fireSessionOpening();
        connectInternal();
        fireSessionOpened();
    }

    @Override
    public void openConnection() throws ConnectionException, AuthenticationException {
        // Do nothing
    }

    @Override
    public void disconnect() throws ConnectionException {
        fireSessionDisconnecting();
        try {
            this.repository = null;
            disconnectInternal();
        } catch (ConnectionException e) {
            fireSessionError(e);
        }
        fireSessionDisconnected();
    }

    @Override
    public Repository getRepository() {
        return this.repository;
    }

    @Override
    public void setTimeout(int timeoutValue) {
        this.connectionTimeout = timeoutValue;
    }

    @Override
    public int getTimeout() {
        return this.connectionTimeout;
    }

    @Override
    public void setReadTimeout(int timeoutValue) {
        this.readTimeout = timeoutValue;
    }

    @Override
    public int getReadTimeout() {
        return this.readTimeout;
    }

    @Override
    public void addSessionListener(SessionListener listener) {
        this.sessionEventSupport.addSessionListener(listener);
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        this.sessionEventSupport.removeSessionListener(listener);
    }

    @Override
    public boolean hasSessionListener(SessionListener listener) {
        return this.sessionEventSupport.hasSessionListener(listener);
    }

    @Override
    public void addTransferListener(TransferListener listener) {
        this.transferEventSupport.addTransferListener(listener);
    }

    @Override
    public void removeTransferListener(TransferListener listener) {
        this.transferEventSupport.removeTransferListener(listener);
    }

    @Override
    public boolean hasTransferListener(TransferListener listener) {
        return this.transferEventSupport.hasTransferListener(listener);
    }

    @Override
    public boolean isInteractive() {
        return this.interactive;
    }

    @Override
    public void setInteractive(boolean interactive) {
        this.interactive = true;
    }

    void createParentDirectories(File destination) throws TransferFailedException {
        fireTransferDebug("Attempting to create parent directories for destination: " + destination.getName());
        File destinationDirectory = destination.getParentFile();
        try {
            destinationDirectory = destinationDirectory.getCanonicalFile();
        } catch (IOException e) {
            // Not essential to have a canonical file
        }
        if (destinationDirectory != null && !destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
            if (!destinationDirectory.exists()) {
                throw new TransferFailedException(
                        "Failed to create specified destination directory: " + destinationDirectory);
            }
        }
    }

    void fireSessionOpening() {
        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_OPENING);
        sessionEvent.setTimestamp(System.currentTimeMillis());
        sessionEventSupport.fireSessionOpening(sessionEvent);
    }

    void fireSessionOpened() {
        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_OPENED);
        sessionEvent.setTimestamp(System.currentTimeMillis());
        sessionEventSupport.fireSessionOpened(sessionEvent);
    }


    void fireSessionError(Exception exception) {
        SessionEvent sessionEvent = new SessionEvent(this, exception);
        sessionEvent.setTimestamp(System.currentTimeMillis());
        sessionEventSupport.fireSessionError(sessionEvent);
    }

    void fireSessionDisconnecting() {
        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_DISCONNECTING);
        sessionEvent.setTimestamp(System.currentTimeMillis());
        sessionEventSupport.fireSessionDisconnecting(sessionEvent);
    }

    void fireSessionDisconnected() {
        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_DISCONNECTED);
        sessionEvent.setTimestamp(System.currentTimeMillis());
        sessionEventSupport.fireSessionDisconnected(sessionEvent);
    }

    void fireGetInitiated(Resource resource, File localFile) {
        TransferEvent transferEvent =
                new TransferEvent(this, resource, TransferEvent.TRANSFER_INITIATED, TransferEvent.REQUEST_GET);
        transferEvent.setTimestamp(System.currentTimeMillis());
        transferEvent.setLocalFile(localFile);
        transferEventSupport.fireTransferInitiated(transferEvent);
    }

    void fireGetStarted(Resource resource, File localFile) {
        TransferEvent transferEvent =
                new TransferEvent(this, resource, TransferEvent.TRANSFER_STARTED, TransferEvent.REQUEST_GET);
        transferEvent.setTimestamp(System.currentTimeMillis());
        transferEvent.setLocalFile(localFile);
        transferEventSupport.fireTransferStarted(transferEvent);
    }

    void fireGetCompleted(Resource resource, File localFile) {
        TransferEvent transferEvent =
                new TransferEvent(this, resource, TransferEvent.TRANSFER_COMPLETED, TransferEvent.REQUEST_GET);
        transferEvent.setTimestamp(System.currentTimeMillis());
        transferEvent.setLocalFile(localFile);
        transferEventSupport.fireTransferCompleted(transferEvent);
    }


    void firePutInitiated(Resource resource, File localFile) {
        TransferEvent transferEvent =
                new TransferEvent(this, resource, TransferEvent.TRANSFER_INITIATED, TransferEvent.REQUEST_PUT);
        transferEvent.setTimestamp(System.currentTimeMillis());
        transferEvent.setLocalFile(localFile);
        transferEventSupport.fireTransferInitiated(transferEvent);
    }

    void firePutStarted(Resource resource, File localFile) {
        TransferEvent transferEvent =
                new TransferEvent(this, resource, TransferEvent.TRANSFER_STARTED, TransferEvent.REQUEST_PUT);
        transferEvent.setTimestamp(System.currentTimeMillis());
        transferEvent.setLocalFile(localFile);
        transferEventSupport.fireTransferStarted(transferEvent);
    }

    void firePutCompleted(Resource resource, File localFile) {
        TransferEvent transferEvent =
                new TransferEvent(this, resource, TransferEvent.TRANSFER_COMPLETED, TransferEvent.REQUEST_PUT);
        transferEvent.setTimestamp(System.currentTimeMillis());
        transferEvent.setLocalFile(localFile);
        transferEventSupport.fireTransferCompleted(transferEvent);
    }

    void fireTransferProgress(TransferEvent transferEvent, byte[] buffer, int n) {
        transferEventSupport.fireTransferProgress(transferEvent, buffer, n);
    }

    void fireTransferDebug(String message) {
        transferEventSupport.fireDebug(message);
    }

    abstract void connectInternal() throws ConnectionException, AuthenticationException;

    abstract void disconnectInternal() throws ConnectionException;
}
