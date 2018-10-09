package com.lahsivjar;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.*;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AbstractWagonTest {

    private SessionEventSupport spySessionEventSupport;
    private TransferEventSupport spyTransferEventSupport;
    private StubWagon stubWagon;

    @Before
    public void setup() {
        spySessionEventSupport = Mockito.spy(new SessionEventSupport());
        spyTransferEventSupport = Mockito.spy(new TransferEventSupport());
        stubWagon = new StubWagon(spySessionEventSupport, spyTransferEventSupport);
    }

    @Test
    public void testSupportsDirectoryCopy() {
        Assert.assertTrue(this.stubWagon.supportsDirectoryCopy());
    }

    @Test
    public void testConnect1() throws ConnectionException, AuthenticationException {
        final Repository mockRepo = Mockito.mock(Repository.class);
        this.stubWagon.connect(mockRepo);
        Assert.assertEquals(mockRepo, this.stubWagon.getRepository());
        testConnectInternal();
    }

    @Test
    public void testConnect2() throws ConnectionException, AuthenticationException {
        final Repository mockRepo = Mockito.mock(Repository.class);
        final ProxyInfo proxyInfo = Mockito.mock(ProxyInfo.class);
        this.stubWagon.connect(mockRepo, proxyInfo);
        Assert.assertEquals(mockRepo, this.stubWagon.getRepository());
        testConnectInternal();
    }

    @Test
    public void testConnect3() throws ConnectionException, AuthenticationException {
        final Repository mockRepo = Mockito.mock(Repository.class);
        final AuthenticationInfo authInfo = Mockito.mock(AuthenticationInfo.class);
        this.stubWagon.connect(mockRepo, authInfo);
        Assert.assertEquals(mockRepo, this.stubWagon.getRepository());
        testConnectInternal();
    }

    @Test
    public void testConnect4() throws ConnectionException, AuthenticationException {
        final Repository mockRepo = Mockito.mock(Repository.class);
        final ProxyInfoProvider proxyInfoProvider = Mockito.mock(ProxyInfoProvider.class);
        this.stubWagon.connect(mockRepo, proxyInfoProvider);
        Assert.assertEquals(mockRepo, this.stubWagon.getRepository());
        testConnectInternal();
    }

    @Test
    public void testConnect5() throws ConnectionException, AuthenticationException {
        final Repository mockRepo = Mockito.mock(Repository.class);
        final ProxyInfoProvider proxyInfoProvider = Mockito.mock(ProxyInfoProvider.class);
        final AuthenticationInfo authInfo = Mockito.mock(AuthenticationInfo.class);
        this.stubWagon.connect(mockRepo, authInfo, proxyInfoProvider);
        Assert.assertEquals(mockRepo, this.stubWagon.getRepository());
        testConnectInternal();
    }

    @Test
    public void testConnect6() throws ConnectionException, AuthenticationException {
        final Repository mockRepo = Mockito.mock(Repository.class);
        final ProxyInfo proxyInfo = Mockito.mock(ProxyInfo.class);
        final AuthenticationInfo authInfo = Mockito.mock(AuthenticationInfo.class);
        this.stubWagon.connect(mockRepo, authInfo, proxyInfo);
        Assert.assertEquals(mockRepo, this.stubWagon.getRepository());
        testConnectInternal();
    }

    private void testConnectInternal() {
        Mockito.verify(this.spySessionEventSupport).fireSessionOpening(
                Mockito.argThat((SessionEvent se) -> se.getEventType() == SessionEvent.SESSION_OPENING));
        Mockito.verify(this.spySessionEventSupport).fireSessionOpened(
                Mockito.argThat((SessionEvent se) -> se.getEventType() == SessionEvent.SESSION_OPENED));
    }

    @Test
    public void testDisconnect() throws ConnectionException {
        this.stubWagon.disconnect();
        Mockito.verify(this.spySessionEventSupport).fireSessionDisconnecting(
                Mockito.argThat((SessionEvent se) -> se.getEventType() == SessionEvent.SESSION_DISCONNECTING));
        Mockito.verify(this.spySessionEventSupport).fireSessionDisconnected(
                Mockito.argThat((SessionEvent se) -> se.getEventType() == SessionEvent.SESSION_DISCONNECTED));
    }

    @Test
    public void testTimeout() {
        final int timeout = 9999;
        Assert.assertEquals(Wagon.DEFAULT_CONNECTION_TIMEOUT, this.stubWagon.getTimeout());
        this.stubWagon.setTimeout(timeout);
        Assert.assertEquals(timeout, this.stubWagon.getTimeout());
    }

    @Test
    public void testReadTimeout() {
        final int timeout = 9999;
        Assert.assertEquals(Wagon.DEFAULT_READ_TIMEOUT, this.stubWagon.getReadTimeout());
        this.stubWagon.setReadTimeout(timeout);
        Assert.assertEquals(timeout, this.stubWagon.getReadTimeout());
    }

    @Test
    public void testInteractive() {
        Assert.assertFalse(this.stubWagon.isInteractive());
        this.stubWagon.setInteractive(true);
        Assert.assertTrue(this.stubWagon.isInteractive());
    }

    @Test
    public void testAddSessionListener() {
        final SessionListener mockSessionListener = Mockito.mock(SessionListener.class);
        this.stubWagon.addSessionListener(mockSessionListener);
        Assert.assertTrue(spySessionEventSupport.hasSessionListener(mockSessionListener));
    }

    @Test
    public void testRemoveSessionListener() {
        final SessionListener mockSessionListener = Mockito.mock(SessionListener.class);
        this.stubWagon.addSessionListener(mockSessionListener);
        Assert.assertTrue(spySessionEventSupport.hasSessionListener(mockSessionListener));
        this.stubWagon.removeSessionListener(mockSessionListener);
        Assert.assertFalse(spySessionEventSupport.hasSessionListener(mockSessionListener));
    }

    @Test
    public void testHasSessionListener() {
        final SessionListener mockSessionListener = Mockito.mock(SessionListener.class);
        this.spySessionEventSupport.addSessionListener(mockSessionListener);
        Assert.assertTrue(this.stubWagon.hasSessionListener(mockSessionListener));
    }

    @Test
    public void testAddTransferListener() {
        final TransferListener mockTransferListener = Mockito.mock(TransferListener.class);
        this.stubWagon.addTransferListener(mockTransferListener);
        Assert.assertTrue(spyTransferEventSupport.hasTransferListener(mockTransferListener));
    }

    @Test
    public void testRemoveTransferListener() {
        final TransferListener mockTransferListener = Mockito.mock(TransferListener.class);
        this.stubWagon.addTransferListener(mockTransferListener);
        Assert.assertTrue(spyTransferEventSupport.hasTransferListener(mockTransferListener));
        this.stubWagon.removeTransferListener(mockTransferListener);
        Assert.assertFalse(spyTransferEventSupport.hasTransferListener(mockTransferListener));
    }

    @Test
    public void testHasTransferListener() {
        final TransferListener mockTransferListener = Mockito.mock(TransferListener.class);
        this.spyTransferEventSupport.addTransferListener(mockTransferListener);
        Assert.assertTrue(this.stubWagon.hasTransferListener(mockTransferListener));
    }

    @Test
    public void testFireSessionOpening() {
        stubWagon.fireSessionOpening();

        Mockito.verify(spySessionEventSupport).fireSessionOpening(
                Mockito.argThat((SessionEvent se) -> se.getEventType() == SessionEvent.SESSION_OPENING));
    }

    @Test
    public void testFireSessionError() {
        final Exception mockException = Mockito.mock(Exception.class);
        stubWagon.fireSessionError(mockException);

        Mockito.verify(spySessionEventSupport).fireSessionError(
                Mockito.argThat((SessionEvent se) -> se.getException() == mockException));
    }

    @Test
    public void testFireSessionDisconnecting() {
        stubWagon.fireSessionDisconnecting();

        Mockito.verify(spySessionEventSupport).fireSessionDisconnecting(
                Mockito.argThat((SessionEvent se) -> se.getEventType() == SessionEvent.SESSION_DISCONNECTING));
    }

    @Test
    public void testFireSessionDisconnected() {
        stubWagon.fireSessionDisconnected();

        Mockito.verify(spySessionEventSupport).fireSessionDisconnected(
                Mockito.argThat((SessionEvent se) -> se.getEventType() == SessionEvent.SESSION_DISCONNECTED));
    }

    @Test
    public void testFireGetInitiated() {
        stubWagon.fireGetInitiated(Mockito.mock(Resource.class), Mockito.mock(File.class));

        Mockito.verify(spyTransferEventSupport).fireTransferInitiated(
                Mockito.argThat((TransferEvent te) -> {
                    return te.getEventType() == TransferEvent.TRANSFER_INITIATED &&
                    te.getRequestType() == TransferEvent.REQUEST_GET;
                }));
    }

    @Test
    public void testFireGetStarted() {
        stubWagon.fireGetStarted(Mockito.mock(Resource.class), Mockito.mock(File.class));

        Mockito.verify(spyTransferEventSupport).fireTransferStarted(
                Mockito.argThat((TransferEvent te) -> {
                    return te.getEventType() == TransferEvent.TRANSFER_STARTED &&
                            te.getRequestType() == TransferEvent.REQUEST_GET;
                }));
    }

    @Test
    public void testFireGetCompleted() {
        stubWagon.fireGetCompleted(Mockito.mock(Resource.class), Mockito.mock(File.class));

        Mockito.verify(spyTransferEventSupport).fireTransferCompleted(
                Mockito.argThat((TransferEvent te) -> {
                    return te.getEventType() == TransferEvent.TRANSFER_COMPLETED &&
                            te.getRequestType() == TransferEvent.REQUEST_GET;
                }));
    }

    @Test
    public void testFirePutInitiated() {
        stubWagon.firePutInitiated(Mockito.mock(Resource.class), Mockito.mock(File.class));

        Mockito.verify(spyTransferEventSupport).fireTransferInitiated(
                Mockito.argThat((TransferEvent te) -> {
                    return te.getEventType() == TransferEvent.TRANSFER_INITIATED &&
                            te.getRequestType() == TransferEvent.REQUEST_PUT;
                }));
    }

    @Test
    public void testFirePutStarted() {
        stubWagon.firePutStarted(Mockito.mock(Resource.class), Mockito.mock(File.class));

        Mockito.verify(spyTransferEventSupport).fireTransferStarted(
                Mockito.argThat((TransferEvent te) -> {
                    return te.getEventType() == TransferEvent.TRANSFER_STARTED &&
                            te.getRequestType() == TransferEvent.REQUEST_PUT;
                }));
    }

    @Test
    public void testFirePutCompleted() {
        stubWagon.firePutCompleted(Mockito.mock(Resource.class), Mockito.mock(File.class));

        Mockito.verify(spyTransferEventSupport).fireTransferCompleted(
                Mockito.argThat((TransferEvent te) -> {
                    return te.getEventType() == TransferEvent.TRANSFER_COMPLETED &&
                            te.getRequestType() == TransferEvent.REQUEST_PUT;
                }));
    }

    @Test
    public void testFireTransferProgress() {
        final TransferEvent transferEvent = new TransferEvent(
                stubWagon,
                Mockito.mock(Resource.class),
                TransferEvent.TRANSFER_PROGRESS,
                TransferEvent.REQUEST_PUT);
        final byte[] buffer = "TEST".getBytes();

        stubWagon.fireTransferProgress(transferEvent, buffer, buffer.length);

        Mockito.verify(spyTransferEventSupport).fireTransferProgress(
                Mockito.argThat((TransferEvent te) -> {
                    return te.getEventType() == TransferEvent.TRANSFER_PROGRESS &&
                            te.getRequestType() == TransferEvent.REQUEST_PUT;
                }),
                Mockito.eq(buffer),
                Mockito.eq(buffer.length));
    }

    @Test
    public void testFireTransferDebug() {
        final String testDebugMsg = "debug me";
        stubWagon.fireTransferDebug(testDebugMsg);

        Mockito.verify(spyTransferEventSupport).fireDebug(Mockito.eq("debug me"));
    }

    static final class StubWagon extends AbstractWagon {

        StubWagon(SessionEventSupport sessionEventSupport, TransferEventSupport transferEventSupport) {
            super(sessionEventSupport, transferEventSupport);
        }

        @Override
        void connectInternal() throws ConnectionException, AuthenticationException {

        }

        @Override
        void disconnectInternal() throws ConnectionException {

        }

        @Override
        public void get(String resourceName, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

        }

        @Override
        public boolean getIfNewer(String resourceName, File destination, long timestamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
            return false;
        }

        @Override
        public void put(File source, String destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

        }

        @Override
        public void putDirectory(File sourceDirectory, String destinationDirectory) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {

        }

        @Override
        public boolean resourceExists(String resourceName) throws TransferFailedException, AuthorizationException {
            return false;
        }

        @Override
        public List<String> getFileList(String destinationDirectory) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
            return null;
        }
    }

}
