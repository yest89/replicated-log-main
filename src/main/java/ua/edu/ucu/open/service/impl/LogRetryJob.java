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

//    private static final int TIME_OUT = 2;  //minutes, should be moved to spring configuration property
//    private static final int MAX_RETRY_ATTEMPTS = Integer.MAX_VALUE; // should be moved to spring configuration property

    private final LogService logService;
    private final HealthCheckService healthCheckService;
    private final List<ReplicatedLogClient> slaves;

    private Map<ReplicatedLogClient, Boolean> previousHeartBeatResult = new HashMap<>();

//    @Async
//    @Scheduled(fixedRate = 10000)
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

//        currentHeartBeatResult.entrySet().stream()
//                .filter(entry -> !entry.getValue())
//                .forEach(entry -> {
//                    CountDownLatch latch = new CountDownLatch(Collections.frequency(currentHeartBeatResult.keySet(), Boolean.FALSE));
//                    logMessages.forEach(log -> logService.sendLogMessage(log, latch, entry.getKey()));
//                });
//                .map(entry -> logMessages.forEach(log -> entry.getValue().storeLog(log, counter.incrementAndGet()))

        previousHeartBeatResult = currentHeartBeatResult;

//        Instant endOfTimeOutConnection = Instant.now().plus(TIME_OUT, ChronoUnit.MINUTES);
//        OperationHelper.doWithRetry(MAX_RETRY_ATTEMPTS, new OperationHelper.Operation() {
//            @Override
//            public void act() throws InconsistentException, HealthCheckException, InterruptedException {
//                if (endOfTimeOutConnection.isBefore(Instant.now())) {
//                    log.error("One of the slaves are failed!");
//                    throw new InconsistentException("One of the slaves are failed! It failed to recover connection");
//                }
//
//                for (int i = 0; i < slaves.size(); i++) {
//                    try {
//                        healthCheckService.healthCheck(i);
//                    } catch (Exception ex) {
//                        throw new HealthCheckException(String.valueOf(i));
//                    }
//                }
//
//                OptionalInt first1 = IntStream.range(0, isHealthChecks.size())
//                        .filter(i -> !isHealthChecks.get(i))
//                        .findFirst();
//                if (first1.isEmpty()) {
//                    return;
//                }
//
//                List<String> logMessages = logRepository.getAll();
//                AtomicInteger counter = new AtomicInteger(-1);
//                if (!logMessages.isEmpty() && !retryStatus) {
//                    List<ListenableFuture<Acknowledge>> futures = new ArrayList<>();
//                    AsyncReplicatedLogClient asyncReplicatedLogClient = slaves.get(first1.getAsInt());
//                    for (int j = 0; j < logMessages.size(); j++) {
//                        ListenableFuture<Acknowledge> future =
//                                asyncReplicatedLogClient.storeLog(logMessages.get(j), counter.incrementAndGet());
//                        futures.add(future);
//                    }
//                    log.info("size of futures {}", futures.size());
//                    countDownLatch = new CountDownLatch(futures.size());
//                    futures.forEach(
//                            future -> future.addListener(new LogExecutionEvent(), MoreExecutors.directExecutor())
//                    );
//                    countDownLatch.await();
//                    log.info("All messages for slave {} are delivered", asyncReplicatedLogClient.getClientId());
//                    for (int i = 0; i < isHealthChecks.size(); i++) {
//                        isHealthChecks.set(i, true);
//                    }
//                }
//            }
//        });
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
