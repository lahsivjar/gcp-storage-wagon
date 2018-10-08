package com.lahsivjar;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionEvent;
import org.apache.maven.wagon.events.SessionEventSupport;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferEventSupport;
import org.apache.maven.wagon.resource.Resource;
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
