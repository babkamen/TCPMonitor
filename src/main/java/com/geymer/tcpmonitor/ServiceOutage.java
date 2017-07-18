package com.geymer.tcpmonitor;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

/**
 * Created by babkamen on 17.07.2017.
 */
@Data
@Builder
public class ServiceOutage {
    private ZonedDateTime startTime,endTime;
}
