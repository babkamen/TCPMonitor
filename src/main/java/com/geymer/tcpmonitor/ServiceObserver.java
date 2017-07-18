package com.geymer.tcpmonitor;

public interface ServiceObserver {
    void update(Status serviceStatus);
}
