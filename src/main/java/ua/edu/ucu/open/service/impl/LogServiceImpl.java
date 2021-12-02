package ua.edu.ucu.open.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.edu.ucu.open.exception.InconsistentException;
import ua.edu.ucu.open.grpc.ReplicatedLogClient;
import ua.edu.ucu.open.grpc.log.Acknowledge;
import ua.edu.ucu.open.model.WriteConcern;
import ua.edu.ucu.open.repo.LogRepository;
import ua.edu.ucu.open.service.HealthCheckService;
import ua.edu.ucu.open.service.LogService;

import java.util.ArrayList;
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
//    private int retryLogCounter;
//    private static final int MAX_RETRY_ATTEMPTS = Integer.MAX_VALUE; // should be moved to spring configuration property

    private final LogRepository logRepository;

    private final ExecutorService workerThreadPoll = Executors.newCachedThreadPool();

    private final HealthCheckService healthCheckService;
    private final List<ReplicatedLogClient> slaves;

    public static boolean retryStatus = false;

    @Override
    public List<String> getAll() {
        return logRepository.getAll();
    }

    @Override
    public void add(String logMessage, WriteConcern writeConcern) throws InconsistentException {
        log.debug("started adding log operation!");
//        checkQuorumsAlive();

        LOG_COUNTER.incrementAndGet();
        logRepository.add(logMessage, LOG_COUNTER.get());

        CountDownLatch latch = new CountDownLatch(writeConcern.getWriteConcern() - 1);
        List<Acknowledge> resultsFromSlaves = new ArrayList<>();
        slaves.forEach(slave -> workerThreadPoll.submit(() -> {
            resultsFromSlaves.add(slave.storeLog(logMessage, LOG_COUNTER.get()));
            latch.countDown();
        }));

        try {
            latch.await(TIME_OUT, TimeUnit.MINUTES);
            boolean failedOneOfSlaves = resultsFromSlaves.stream().anyMatch(ack -> !validateAckMessage(ack));
            if (failedOneOfSlaves) {
                log.error("One of the slaves are failed!");
                throw new InconsistentException("One of the slaves are failed!");
            }
        } catch (InterruptedException e) {
            log.error("One of the slaves are failed!");
            throw new InconsistentException("One of the slaves are failed!");
        }
        log.debug("finished adding log operation!");
    }

//    private void checkQuorumsAlive() throws NoQuorumException {
//        List<Boolean> slaveStatuses = new ArrayList<>();
//        int counter = 0;
//        for (int i = 0; i < asyncSlaves.size(); i++) {
//            try {
//                slaveStatuses.add(healthCheckService.healthCheck(i));
//            } catch (Exception ex) {
//                counter++;
//            }
//        }
//        boolean isWholeSystemBroken = slaveStatuses.stream().noneMatch(status -> status);
//        if (isWholeSystemBroken || counter == asyncSlaves.size()) {
//            log.error("There is no quorums, the master is switched into read-only mode!");
//            throw new NoQuorumException("There is no quorums, the master is switched into read-only mode!");
//        }
//    }

//    private void sendLogWithRetry(String logMessage, AsyncReplicatedLogClient client, Instant endOfTimeOutConnection) {
//        retryLogCounter = LOG_COUNTER.get();
//        OperationHelper.doWithRetry(MAX_RETRY_ATTEMPTS, new OperationHelper.Operation() {
//            @Override
//            public void act() throws ExecutionException, InterruptedException, InconsistentException {
//                if (endOfTimeOutConnection.isBefore(Instant.now())) {
//                    log.error("One of the slaves are failed!");
//                    return;
//                }
//                retryStatus = true;
//                if (healthCheckService.healthCheck(client.getClientId())) {
//                    ListenableFuture<Acknowledge> future =
//                            client.storeLog(logMessage, retryLogCounter);
//
//                    Acknowledge acknowledge = future.get();
//                    boolean success = validateAckMessage(acknowledge);
//
//                    if (!success) {
//                        log.error("Problem with write concern policy!");
//                        throw new InconsistentException("Problem with write concern policy!");
//                    }
//                }
//                retryStatus = false;
//                log.info("retry operation for client id {} is finished.", client.getClientId());
//            }
//
//            @Override
//            public void handleException(Exception cause) {
//                log.error("First slave is down. Trying to recover it.");
//            }
//        });
//    }

    private boolean validateAckMessage(Acknowledge acknowledge) {
        return (acknowledge != null && ACK.equals(acknowledge.getMessage())) ? Boolean.TRUE : Boolean.FALSE;
    }
}
