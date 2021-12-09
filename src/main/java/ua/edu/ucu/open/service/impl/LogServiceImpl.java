package ua.edu.ucu.open.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.edu.ucu.open.exception.InconsistentException;
import ua.edu.ucu.open.exception.NoQuorumException;
import ua.edu.ucu.open.grpc.ReplicatedLogClient;
import ua.edu.ucu.open.grpc.log.Acknowledge;
import ua.edu.ucu.open.helper.OperationHelper;
import ua.edu.ucu.open.model.WriteConcern;
import ua.edu.ucu.open.repo.LogRepository;
import ua.edu.ucu.open.service.HealthCheckService;
import ua.edu.ucu.open.service.LogService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private static final String ACK = "ACK";
    private static final int TIME_OUT = 2;  //minutes, should be moved to spring configuration property
    private static final AtomicInteger LOG_COUNTER = new AtomicInteger(-1);
    private static final int MAX_RETRY_ATTEMPTS = Integer.MAX_VALUE; // should be moved to spring configuration property

    private final LogRepository logRepository;
    private final ExecutorService workerThreadPoll = Executors.newCachedThreadPool();
    private final List<ReplicatedLogClient> slaves;
    private final HealthCheckService healthCheckService;

    @Override
    public List<String> getAll() {
        return logRepository.getAll();
    }

    @Override
    public void add(String logMessage, WriteConcern writeConcern) throws InconsistentException, NoQuorumException {
        log.debug("started adding log operation!");
        checkQuorumsAlive();

        LOG_COUNTER.incrementAndGet();
        logRepository.add(logMessage, LOG_COUNTER.get());

        CountDownLatch latch = new CountDownLatch(writeConcern.getWriteConcern() - 1);
        slaves.forEach(slave -> workerThreadPoll.submit(() -> sendLogMessage(logMessage, latch, slave)));

        try {
            latch.await(TIME_OUT, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.error("One of the slaves are failed!");
            throw new InconsistentException("One of the slaves are failed!");
        }
        log.debug("finished adding log operation!");
    }

    public void sendLogMessage(String logMessage, CountDownLatch latch, ReplicatedLogClient slave) {
        int counter = LOG_COUNTER.get();
        try {
            Acknowledge acknowledge = slave.storeLog(logMessage, counter);
            if (validateAckMessage(acknowledge)) {
                latch.countDown();
            }
        } catch (Exception e) {
            log.error("One of the slaves are failed!");
            sendLogWithRetry(logMessage, slave,
                    Instant.now().plus(TIME_OUT, ChronoUnit.MINUTES),
                    counter, latch);
        }
    }

    private void checkQuorumsAlive() throws NoQuorumException {
        boolean isWholeSystemBroken = slaves.stream()
                .noneMatch(healthCheckService::healthCheck);

        if (isWholeSystemBroken) {
            log.error("There is no quorums, the master is switched into read-only mode!");
            throw new NoQuorumException("There is no quorums, the master is switched into read-only mode!");
        }
    }

    private void sendLogWithRetry(String logMessage, ReplicatedLogClient client, Instant endOfTimeOutConnection,
                                  int counter, CountDownLatch latch) {
        OperationHelper.doWithRetry(MAX_RETRY_ATTEMPTS, new OperationHelper.Operation() {
            @Override
            public void act() {
                if (endOfTimeOutConnection.isBefore(Instant.now())) {
                    log.error("One of the slaves are failed!");
                    return;
                }

                Acknowledge acknowledge = client.storeLog(logMessage, counter);
                if (!validateAckMessage(acknowledge)) {
                    log.error("Validation acknowledge failed");
                    return;
                }
                latch.countDown();
                log.info("retry operation for client id {} is finished.", client.getClientId());
            }

            @Override
            public void handleException(Exception cause) {
                log.error("The slave {} is down. Trying to recover it.", client.getClientId());
            }
        });
    }

    private boolean validateAckMessage(Acknowledge acknowledge) {
        return (acknowledge != null && ACK.equals(acknowledge.getMessage())) ? Boolean.TRUE : Boolean.FALSE;
    }
}
