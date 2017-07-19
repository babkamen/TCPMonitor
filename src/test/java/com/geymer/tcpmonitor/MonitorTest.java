package com.geymer.tcpmonitor;

import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class MonitorTest {
    //Get free random port
    private static int randomPort;
    Service service = Service.builder()
            .host("localhost")
            .port(randomPort)
            .build();

    @Mock
    ServiceObserver serviceObserver;
    @Captor
    ArgumentCaptor<Status> argumentCaptor;
    
    @BeforeClass
    public static void init() throws IOException {
        try(ServerSocket serverSocket = new ServerSocket(0)) {
            randomPort = serverSocket.getLocalPort();
        }
    }


    @Test
    public void monitorShouldConnectTcpConnectionToService() throws IOException {
        Monitor monitor = new Monitor();
        assertEquals(monitor.checkStatus(service), Status.DOWN);
    }

    @Test
    public void monitorShouldConnectTcpConnectionToServiceAndReturnTrue() throws Exception {
        Monitor monitor = new Monitor();
        ServerSocket serverSocket = new ServerSocket(0);
        Service service = Service.builder()
                .host("localhost")
                .port(serverSocket.getLocalPort())
                .build();
        try {
            assertEquals(monitor.checkStatus(service), Status.UP);
        } finally {
            serverSocket.close();
        }
    }

    @Test
    public void monitorShouldNotifyWhenServiceChangesState() throws Exception {
        Monitor monitor = new Monitor();
        List<Status> expected = Arrays.asList(Status.UP, Status.DOWN);

        try (final ServerSocket serverSocket = new ServerSocket(randomPort)) {

            monitor.subscribe(ServiceSubscription.builder()
                    .pollingFrequency(1)
                    .service(service)
                    .serviceObserver(serviceObserver)
                    .build());
            monitor.start();

            scheduleServerStopAfter(serverSocket, 2000);

            Thread.sleep(5_000L);

            //check if notified
            verify(serviceObserver, atLeast(1)).update(argumentCaptor.capture());
            List<Status> actual = argumentCaptor.getAllValues();
            assertEquals(expected, actual);
            monitor.stop();
        }
    }


    @Test
    public void monitorShouldBeSilentDuringServiceOutage() throws Exception {
        Monitor monitor = new Monitor(Executors.newScheduledThreadPool(10));
        List<Status> expected = Arrays.asList(Status.UP);

        try (final ServerSocket serverSocket = new ServerSocket(randomPort)) {

            monitor.subscribe(ServiceSubscription.builder()
                    .pollingFrequency(1)
                    .service(service)
                    .serviceObserver(serviceObserver)
                    .build());

            ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());
            service.scheduleOutage(ServiceOutage.builder()
                    .startTime(now.plusSeconds(2))
                    .endTime(now.plusSeconds(5))
                    .build());

            monitor.start();

            scheduleServerStopAfter(serverSocket, 2000);

            Thread.sleep(3_000L);

            //check if notified
            verify(serviceObserver, atLeast(1)).update(argumentCaptor.capture());
            List<Status> actual = argumentCaptor.getAllValues();
            assertEquals(expected, actual);
            monitor.stop();
        }
    }

    private void scheduleServerStopAfter(final ServerSocket serverSocket, long duration) {
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        try {
                            log.debug("Stopping server");
                            serverSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                },
                duration
        );
    }

}

