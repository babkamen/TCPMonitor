package com.geymer.tcpmonitor;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;


@Data
@Builder
public class ServiceOutage {
    private ZonedDateTime startTime,endTime;
}
