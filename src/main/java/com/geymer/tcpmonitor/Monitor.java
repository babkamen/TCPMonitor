package com.geymer.tcpmonitor;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@NoArgsConstructor
public class Monitor {
    private List<Service> serviceRegistry = new ArrayList<>();
    private Map<Service, Set<ServiceObserver>> observers = new HashMap();
    private ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(10);


    public Monitor(ScheduledExecutorService scheduledPool) {
        this.scheduledPool = scheduledPool;
    }

    public void registerService(Service service) {
        this.serviceRegistry.add(service);
    }

    /**
     * Subscribe service observer to particular service with polling frequency
     *
     * @param serviceSubscription
     */
    public void subscribe(ServiceSubscription serviceSubscription) {

        Service service = serviceSubscription.getService();
        long queryPeriod = serviceSubscription.getPollingFrequency();
        
        if (serviceSubscription.getGracePeriod() != 0 && serviceSubscription.getGracePeriod() < queryPeriod) {
            queryPeriod = serviceSubscription.getGracePeriod();
        }
        service.setQueryPeriod(queryPeriod * 1000);

        if (observers.containsKey(service)) {
            observers.get(service)
                    .add(serviceSubscription.getServiceObserver());
            if (service.getQueryPeriod() < 1000) {
                service.setQueryPeriod(1000);
            }
        } else {
            Set<ServiceObserver> serviceObservers = new HashSet<>();
            serviceObservers.add(serviceSubscription.getServiceObserver());
            observers.put(service, serviceObservers);
        }
    }

    public void unsubscribe(Service service, ServiceObserver serviceObserver) {
        if (observers.containsKey(service)) {
            observers.get(service)
                    .remove(serviceObserver);
        }
    }

    public boolean containsService(Service service) {
        return serviceRegistry.contains(service);
    }

    public Status checkStatus(Service service) {
        try (Socket s = new Socket(service.getHost(), service.getPort())) {
            return Status.UP;
        } catch (IOException ex) {
                   /* expected case */
        }
        return Status.DOWN;
    }

    private void checkServiceStatus(Service service) {

        if (service.isInOutage()) {
            log.debug("Service {} is in outage", service);
            return;
        }

        Status status = checkStatus(service);

        log.debug("Service {} is {}", service, status);

        if (!status.equals(service.getStatus())) {

            Set<ServiceObserver> so = observers.get(service);
            log.debug("Found callers={}", so);
            for (ServiceObserver serviceObserver : so) {
                serviceObserver.update(status);
            }
            service.setStatus(status);
        }
    }

    public void start() {
        for (Service s : serviceRegistry) {
            ScheduledFuture<?> scheduledFuture = scheduledPool.scheduleWithFixedDelay(() -> {
                checkServiceStatus(s);
            }, s.getQueryPeriod(), s.getQueryPeriod(), TimeUnit.MILLISECONDS);
        }
        log.debug("Going up!");
    }

    public void stop() throws InterruptedException {
        scheduledPool.awaitTermination(1, TimeUnit.SECONDS);
        log.debug("Have a nice day!");
    }
}
