package ua.edu.ucu.open.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.edu.ucu.open.grpc.ReplicatedLogClient;
import ua.edu.ucu.open.service.HealthCheckService;
import ua.edu.ucu.open.service.LogService;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogRetryJob {

    private final LogService logService;
    private final HealthCheckService healthCheckService;
    private final List<ReplicatedLogClient> slaves;

    private Map<ReplicatedLogClient, Boolean> previousHeartBeatResult = new HashMap<>();

    @Async
    @Scheduled(fixedRate = 10000)
    public void retry() {

        Map<ReplicatedLogClient, Boolean> currentHeartBeatResult = slaves.stream()
                .collect(Collectors.toMap(Function.identity(), healthCheckService::healthCheck));

        List<ReplicatedLogClient> clientsToRetry = findClientWhoTurnsOn();

        List<String> logMessages = logService.getAll();

        clientsToRetry.
                forEach(entry -> {
                    CountDownLatch latch = new CountDownLatch(Collections.frequency(currentHeartBeatResult.keySet(), Boolean.FALSE));
                    logMessages.forEach(log -> logService.sendLogMessage(log, latch, entry));
                });

        previousHeartBeatResult = currentHeartBeatResult;
    }

    private List<ReplicatedLogClient> findClientWhoTurnsOn() {
        return previousHeartBeatResult.entrySet().stream()
                .filter(entry -> entry.getValue().equals(
                        previousHeartBeatResult.entrySet().stream()
                                .filter(entry1 -> entry1.getKey().getClientId() == entry.getKey().getClientId())
                                .findFirst()
                                .get()
                                .getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
