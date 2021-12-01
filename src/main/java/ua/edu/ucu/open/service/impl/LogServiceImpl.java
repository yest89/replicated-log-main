package ua.edu.ucu.open.service.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.edu.ucu.open.exception.InconsistentException;
import ua.edu.ucu.open.exception.NoQuorumException;
import ua.edu.ucu.open.grpc.AsyncReplicatedLogClient;
import ua.edu.ucu.open.grpc.log.Acknowledge;
import ua.edu.ucu.open.helper.OperationHelper;
import ua.edu.ucu.open.model.WriteConcern;
import ua.edu.ucu.open.repo.LogRepository;
import ua.edu.ucu.open.service.HealthCheckService;
import ua.edu.ucu.open.service.LogService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private static final String ACK = "ACK";
    private static final int TIME_OUT = 2;  //minutes, should be moved to spring configuration property
    private static final AtomicInteger LOG_COUNTER = new AtomicInteger(-1);
    private int retryLogCounter;
    private static final int MAX_RETRY_ATTEMPTS = Integer.MAX_VALUE; // should be moved to spring configuration property

    private final LogRepository logRepository;

    private final HealthCheckService healthCheckService;
    private final List<AsyncReplicatedLogClient> slaves;

    private CountDownLatch countDownLatch = new CountDownLatch(1);
    private String currentLogMessage;
    public static boolean retryStatus = false;

    @Override
    public List<String> getAll() {
        return logRepository.getAll();
    }

    @Override
    public void add(String logMessage, WriteConcern writeConcern) throws InconsistentException, NoQuorumException {
        log.debug("started adding log operation!");
        checkQuorumsAlive();

        currentLogMessage = logMessage;

        LOG_COUNTER.incrementAndGet();
        logRepository.add(logMessage, LOG_COUNTER.get());
        saveLog(writeConcern, logMessage);

        log.debug("finished adding log operation!");
    }

    private void checkQuorumsAlive() throws NoQuorumException {
        List<Boolean> slaveStatuses = new ArrayList<>();
        int counter = 0;
        for (int i = 0; i < slaves.size(); i++) {
            try {
                slaveStatuses.add(healthCheckService.healthCheck(i));
            } catch (Exception ex) {
                counter++;
            }
        }
        boolean isWholeSystemBroken = slaveStatuses.stream().noneMatch(status -> status);
        if (isWholeSystemBroken || counter == slaves.size()) {
            log.error("There is no quorums, the master is switched into read-only mode!");
            throw new NoQuorumException("There is no quorums, the master is switched into read-only mode!");
        }
    }

    private void saveLog(WriteConcern writeConcern, String logMessage) throws InconsistentException {
        List<ListenableFuture<Acknowledge>> acknowledgeFutures = slaves.stream()
                .map(inst -> inst.storeLog(logMessage, LOG_COUNTER.get()))
                .collect(Collectors.toList());

        switch (writeConcern) {
            case ONLY_FROM_MASTER:
                return;
            case MASTER_AND_ONE_SECONDARY:
                doMasterAndOneSecondary(acknowledgeFutures);
                return;
            case MASTER_AND_TWO_SECONDARIES:
                doMasterAndTwoSecondaries(acknowledgeFutures);
                return;
            default:
                log.error("There is no such write concern");
                throw new IllegalArgumentException("There is no such write concern");
        }
    }


    private void doMasterAndOneSecondary(List<ListenableFuture<Acknowledge>> acknowledgeFutures) throws InconsistentException {
        try {
            acknowledgeFutures.forEach(future ->
                    future.addListener(new LogExecutionEvent(), MoreExecutors.directExecutor()));
            countDownLatch.await();
            countDownLatch = new CountDownLatch(1);
        } catch (Exception e) {
            log.error("One of the slaves are failed!");
            throw new InconsistentException("One of the slaves are failed!");
        }
    }

    private void doMasterAndTwoSecondaries(List<ListenableFuture<Acknowledge>> acknowledgeFutures) throws InconsistentException {
        Acknowledge acknowledgeFromSlave;
        boolean isValidatedFromSlave;
        List<Boolean> validationFromSlaves = new ArrayList<>();

        for (int i = 0; i < acknowledgeFutures.size(); i++) {
            try {
                acknowledgeFromSlave = acknowledgeFutures.get(i).get();
                isValidatedFromSlave = validateAckMessage(acknowledgeFromSlave);
                validationFromSlaves.add(isValidatedFromSlave);
            } catch (InterruptedException | ExecutionException e) {
                log.error("One of the slaves are failed!");
                sendLogWithRetry(currentLogMessage, slaves.get(i), Instant.now().plus(TIME_OUT, ChronoUnit.MINUTES));
            }
        }

        for (int i = 0; i < validationFromSlaves.size(); i++) {
            if (!validationFromSlaves.get(i)) {
                log.error("Problem with write concern policy!");
                throw new InconsistentException("Problem with write concern policy!");
            }
        }
    }

    private void sendLogWithRetry(String logMessage, AsyncReplicatedLogClient client, Instant endOfTimeOutConnection) {
        retryLogCounter = LOG_COUNTER.get();
        OperationHelper.doWithRetry(MAX_RETRY_ATTEMPTS, new OperationHelper.Operation() {
            @Override
            public void act() throws ExecutionException, InterruptedException, InconsistentException {
                if (endOfTimeOutConnection.isBefore(Instant.now())) {
                    log.error("One of the slaves are failed!");
                    return;
                }
                retryStatus = true;
                if (healthCheckService.healthCheck(client.getClientId())) {
                    ListenableFuture<Acknowledge> future =
                            client.storeLog(logMessage, retryLogCounter);

                    Acknowledge acknowledge = future.get();
                    boolean success = validateAckMessage(acknowledge);

                    if (!success) {
                        log.error("Problem with write concern policy!");
                        throw new InconsistentException("Problem with write concern policy!");
                    }
                }
                retryStatus = false;
                log.info("retry operation for client id {} is finished.", client.getClientId());
            }

            @Override
            public void handleException(Exception cause) {
                log.error("First slave is down. Trying to recover it.");
            }
        });
    }

    private boolean validateAckMessage(Acknowledge acknowledge) {
        return (acknowledge != null && ACK.equals(acknowledge.getMessage())) ? Boolean.TRUE : Boolean.FALSE;
    }

    public class LogExecutionEvent implements Runnable {
        @Override
        public void run() {
            countDownLatch.countDown();
        }
    }
}
