package ua.edu.ucu.open.service.impl;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.edu.ucu.open.exception.InconsistentException;
import ua.edu.ucu.open.grpc.AsyncReplicatedLogClientImpl;
import ua.edu.ucu.open.grpc.AsyncReplicatedLogClientSecond;
import ua.edu.ucu.open.grpc.log.Acknowledge;
import ua.edu.ucu.open.helper.OperationHelper;
import ua.edu.ucu.open.repo.LogRepository;
import ua.edu.ucu.open.service.HealthCheckService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckJob {

    private static final int TIME_OUT = 2;  //minutes, should be moved to spring configuration property
    private static final int MAX_RETRY_ATTEMPTS = Integer.MAX_VALUE; // should be moved to spring configuration property

    private final LogRepository logRepository;
    private final HealthCheckService healthCheckService;
    private final AsyncReplicatedLogClientImpl asyncReplicatedLogClientImpl;
    private final AsyncReplicatedLogClientSecond asyncReplicatedLogClientSecond;
    private boolean isHealthCheckForFirst = true;
    private boolean isHealthCheckForSecond = true;

    @Async
    @Scheduled(fixedRate = 10000)
    public void checkHealthCheckForFirst() {
        Instant endOfTimeOutConnection = Instant.now().plus(TIME_OUT, ChronoUnit.MINUTES);
        OperationHelper.doWithRetry(MAX_RETRY_ATTEMPTS, isHealthCheckForFirst, new OperationHelper.Operation() {
            @Override
            public void act() throws InconsistentException {
                if (endOfTimeOutConnection.isBefore(Instant.now())) {
                    log.error("One of the slaves are failed!");
                    throw new InconsistentException("One of the slaves are failed! It failed to recover connection");
                }

                isHealthCheckForFirst = healthCheckService.healthCheckForFirstSlave();

                List<String> logMessages = logRepository.getAll();
                if (!logMessages.isEmpty() && !isHealthCheckForFirst) {
                    List<ListenableFuture<Acknowledge>> futures = new ArrayList<>();

                    for (int i = 0; i < logMessages.size(); i++) {
                        ListenableFuture<Acknowledge> future =
                                asyncReplicatedLogClientImpl.storeLog(logMessages.get(i), i);
                        futures.add(future);
                    }
                    log.info("message for slave {}", asyncReplicatedLogClientImpl.getClientId());

                    CompletableFuture.allOf((CompletableFuture<?>) futures).join();
                    isHealthCheckForFirst = true;
                }
            }
        });
    }

    @Async
    @Scheduled(fixedRate = 10000)
    public void checkHealthCheckForSecond() {
        Instant endOfTimeOutConnection = Instant.now().plus(TIME_OUT, ChronoUnit.MINUTES);
        OperationHelper.doWithRetry(MAX_RETRY_ATTEMPTS, isHealthCheckForSecond, new OperationHelper.Operation() {
            @Override
            public void act() throws InconsistentException {
                if (endOfTimeOutConnection.isBefore(Instant.now())) {
                    log.error("One of the slaves are failed!");
                    throw new InconsistentException("One of the slaves are failed! It failed to recover connection");
                }

                isHealthCheckForSecond = healthCheckService.healthCheckForFirstSlave();

                List<String> logMessages = logRepository.getAll();
                if (!logMessages.isEmpty() && !isHealthCheckForSecond) {
                    List<ListenableFuture<Acknowledge>> futures = new ArrayList<>();

                    for (int i = 0; i < logMessages.size(); i++) {
                        ListenableFuture<Acknowledge> future =
                                asyncReplicatedLogClientSecond.storeLog(logMessages.get(i), i);
                        futures.add(future);
                    }
                    log.info("message for slave {}", asyncReplicatedLogClientSecond.getClientId());

                    CompletableFuture.allOf((CompletableFuture<?>) futures).join();
                    isHealthCheckForSecond = true;
                }
            }
        });
    }
}
