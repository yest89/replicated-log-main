package ua.edu.ucu.open.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.edu.ucu.open.controller.HealthCheckClient;
import ua.edu.ucu.open.grpc.ReplicatedLogClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogRetryJob {

    private final HealthCheckClient healthCheckClient;
    private final List<ReplicatedLogClient> slaves;
    private final Map<ReplicatedLogClient, Boolean> heartBeatStatuses;

    @Async
    @Scheduled(fixedRate = 1000)
    public void retry() {
        slaves.forEach(slave -> heartBeatStatuses.computeIfPresent(slave, (k, v) -> healthCheckClient.healthCheck(k)));
    }
}
