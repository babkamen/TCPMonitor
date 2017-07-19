package com.geymer.tcpmonitor;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.concurrent.ScheduledFuture;

@Data
@Builder
@EqualsAndHashCode(exclude = {"status", "serviceOutage", "queryPeriod"})
public class Service {

    private String host;
    private int port;
    /**
     * Query period in milliseconds
     */
    private long queryPeriod;
    private Status status;
    private ServiceOutage serviceOutage;

    /**
     * Shows if service is in outage period
     * @return
     */
    public boolean isInOutage() {
        if (serviceOutage == null) {
            return false;
        }

        ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());
        ZonedDateTime startTime = serviceOutage.getStartTime();
        ZonedDateTime endTime = serviceOutage.getEndTime();
        return now.isEqual(startTime) || now.isEqual(endTime)
                || (now.isAfter(startTime) && now.isBefore(endTime));
    }

    public void scheduleOutage(ServiceOutage serviceOutage) {
        this.serviceOutage = serviceOutage;
    }


}
