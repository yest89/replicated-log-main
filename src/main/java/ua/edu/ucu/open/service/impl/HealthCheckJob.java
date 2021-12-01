package ua.edu.ucu.open.service.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.edu.ucu.open.exception.HealthCheckException;
import ua.edu.ucu.open.exception.InconsistentException;
import ua.edu.ucu.open.grpc.AsyncReplicatedLogClient;
import ua.edu.ucu.open.grpc.log.Acknowledge;
import ua.edu.ucu.open.helper.OperationHelper;
import ua.edu.ucu.open.repo.LogRepository;
import ua.edu.ucu.open.service.HealthCheckService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static ua.edu.ucu.open.service.impl.LogServiceImpl.retryStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckJob {

    private static final int TIME_OUT = 2;  //minutes, should be moved to spring configuration property
    private static final int MAX_RETRY_ATTEMPTS = Integer.MAX_VALUE; // should be moved to spring configuration property

    private final LogRepository logRepository;
    private final HealthCheckService healthCheckService;
    private final List<AsyncReplicatedLogClient> slaves;

    public static List<Boolean> isHealthChecks = Arrays.asList(true, true);
    private CountDownLatch countDownLatch;

    @Async
    @Scheduled(fixedRate = 10000)
    public void checkHealthCheckForFirst() {
        Instant endOfTimeOutConnection = Instant.now().plus(TIME_OUT, ChronoUnit.MINUTES);
        OperationHelper.doWithRetry(MAX_RETRY_ATTEMPTS, new OperationHelper.Operation() {
            @Override
            public void act() throws InconsistentException, HealthCheckException, InterruptedException {
                if (endOfTimeOutConnection.isBefore(Instant.now())) {
                    log.error("One of the slaves are failed!");
                    throw new InconsistentException("One of the slaves are failed! It failed to recover connection");
                }

                for (int i = 0; i < slaves.size(); i++) {
                    try {
                        healthCheckService.healthCheck(i);
                    } catch (Exception ex) {
                        throw new HealthCheckException(String.valueOf(i));
                    }
                }

                OptionalInt first1 = IntStream.range(0, isHealthChecks.size())
                        .filter(i -> !isHealthChecks.get(i))
                        .findFirst();
                if (first1.isEmpty()) {
                    return;
                }

                List<String> logMessages = logRepository.getAll();
                AtomicInteger counter = new AtomicInteger(-1);
                if (!logMessages.isEmpty() && !retryStatus) {
                    List<ListenableFuture<Acknowledge>> futures = new ArrayList<>();
                    AsyncReplicatedLogClient asyncReplicatedLogClient = slaves.get(first1.getAsInt());
                    for (int j = 0; j < logMessages.size(); j++) {
                        ListenableFuture<Acknowledge> future =
                                asyncReplicatedLogClient.storeLog(logMessages.get(j), counter.incrementAndGet());
                        futures.add(future);
                    }
                    log.info("size of futures {}", futures.size());
                    countDownLatch = new CountDownLatch(futures.size());
                    futures.forEach(
                            future -> future.addListener(new LogExecutionEvent(), MoreExecutors.directExecutor())
                    );
                    countDownLatch.await();
                    log.info("All messages for slave {} are delivered", asyncReplicatedLogClient.getClientId());
                    for (int i = 0; i < isHealthChecks.size(); i++) {
                        isHealthChecks.set(i, true);
                    }
                }
            }
        });
    }

    public class LogExecutionEvent implements Runnable {
        @Override
        public void run() {
            countDownLatch.countDown();
        }
    }
}
