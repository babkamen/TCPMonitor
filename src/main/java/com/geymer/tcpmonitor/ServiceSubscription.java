package com.geymer.tcpmonitor;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ServiceSubscription {
    private Service service;
    private ServiceObserver serviceObserver;
    /**
     * Polling Frequency in seconds
     */
    private int pollingFrequency;
    /**
     * Grace Period in seconds
     */
    private long gracePeriod;

}
