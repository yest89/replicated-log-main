package ua.edu.ucu.open.service.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.edu.ucu.open.exception.InconsistentException;
import ua.edu.ucu.open.grpc.AsyncReplicatedLogClient;
import ua.edu.ucu.open.grpc.AsyncReplicatedLogClientSecond;
import ua.edu.ucu.open.grpc.log.Acknowledge;
import ua.edu.ucu.open.model.WriteConcern;
import ua.edu.ucu.open.repo.LogRepository;
import ua.edu.ucu.open.service.LogService;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private static final String ACK = "ACK";
    private static final AtomicInteger LOG_COUNTER = new AtomicInteger(1);

    private final LogRepository logRepository;

    private final AsyncReplicatedLogClient asyncReplicatedLogClient;
    private final AsyncReplicatedLogClientSecond asyncReplicatedLogClientSecond;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    @Override
    public List<String> getAll() {
        return logRepository.getAll();
    }

    @Override
    public void add(String logMessage, WriteConcern writeConcern) throws InconsistentException {
        logRepository.add(logMessage);
        saveLog(writeConcern, logMessage);
        log.info("finished adding log operation!");
    }

    private void saveLog(WriteConcern writeConcern, String logMessage) throws InconsistentException {
        ListenableFuture<Acknowledge> acknowledgeFuture = asyncReplicatedLogClient.storeLog(logMessage, LOG_COUNTER.getAndIncrement());
        ListenableFuture<Acknowledge> acknowledgeFutureSecond = asyncReplicatedLogClientSecond.storeLog(logMessage, LOG_COUNTER.getAndIncrement());

        switch (writeConcern) {
            case ONLY_FROM_MASTER:
                return;
            case MASTER_AND_ONE_SECONDARY:
                doMasterAndOneSecondary(acknowledgeFuture, acknowledgeFutureSecond);
                return;
            case MASTER_AND_TWO_SECONDARIES:
                doMasterAndTwoSecondaries(acknowledgeFuture, acknowledgeFutureSecond);
                return;
            default:
                log.error("There is no such write concern");
                throw new IllegalArgumentException("There is no such write concern");
        }
    }

    private void doMasterAndOneSecondary(
            ListenableFuture<Acknowledge> acknowledgeFuture,
            ListenableFuture<Acknowledge> acknowledgeFutureSecond) throws InconsistentException {
        try {
            acknowledgeFuture.addListener(new LogExecutionEvent(), MoreExecutors.directExecutor());
            acknowledgeFutureSecond.addListener(new LogExecutionEvent(), MoreExecutors.directExecutor());
            countDownLatch.await();
        } catch (Exception e) {
            log.error("One of the slaves are failed!");
            throw new InconsistentException("One of the slaves are failed!");
        }
    }

    private void doMasterAndTwoSecondaries(
            ListenableFuture<Acknowledge> acknowledgeFuture,
            ListenableFuture<Acknowledge> acknowledgeFutureSecond) throws InconsistentException {

        Acknowledge acknowledgeFromFirstSlave;
        Acknowledge acknowledgeFromSecondSlave;
        boolean success;
        try {
            acknowledgeFromFirstSlave = acknowledgeFuture.get();
            acknowledgeFromSecondSlave = acknowledgeFutureSecond.get();
            success = validateAckMessage(acknowledgeFromFirstSlave) && validateAckMessage(acknowledgeFromSecondSlave);
        } catch (InterruptedException | ExecutionException e) {
            log.error("One of the slaves are failed!");
            throw new InconsistentException("One of the slaves are failed!");
        }

        if (!success) {
            log.error("Problem with write concern policy!");
            throw new InconsistentException("Problem with write concern policy!");
        }
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
