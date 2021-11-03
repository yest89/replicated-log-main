package ua.edu.ucu.open.service.impl;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private static final String ACK = "ACK";

    private final LogRepository logRepository;

    private final AsyncReplicatedLogClient asyncReplicatedLogClient;
    private final AsyncReplicatedLogClientSecond asyncReplicatedLogClientSecond;

    @Override
    public List<String> getAll() {
        return logRepository.getAll();
    }

    @Override
    public void add(String logMessage, WriteConcern writeConcern) throws InconsistentException {
        logRepository.add(logMessage);
        saveLog(writeConcern, logMessage);
        log.info("finished");
    }

    private void saveLog(WriteConcern writeConcern, String logMessage) throws InconsistentException {
        ListenableFuture<Acknowledge> acknowledgeFuture = asyncReplicatedLogClient.storeLog(logMessage);
        ListenableFuture<Acknowledge> acknowledgeFutureSecond = asyncReplicatedLogClientSecond.storeLog(logMessage);
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
        while (!acknowledgeFuture.isDone() || acknowledgeFutureSecond.isDone()) {
        }
        if (acknowledgeFuture.isDone() && validateAckMessage(acknowledgeFuture)) {
            return;
        }
        if (acknowledgeFutureSecond.isDone() && validateAckMessage(acknowledgeFutureSecond)) {
            return;
        }
        log.error("Problem with write concern policy!");
        throw new InconsistentException("Problem with write concern policy!");
    }

    private void doMasterAndTwoSecondaries(
            ListenableFuture<Acknowledge> acknowledgeFuture,
            ListenableFuture<Acknowledge> acknowledgeFutureSecond) throws InconsistentException {
        while (!acknowledgeFuture.isDone() && acknowledgeFutureSecond.isDone()) {
        }
        if (!(validateAckMessage(acknowledgeFuture) && validateAckMessage(acknowledgeFutureSecond))) {
            log.error("Problem with write concern policy!");
            throw new InconsistentException("Problem with write concern policy!");
        }
    }

    //TODO intentionally to skip processing exception/exception policies
    @SneakyThrows
    private boolean validateAckMessage(ListenableFuture<Acknowledge> acknowledgeFuture) {
        return (acknowledgeFuture.isDone() && ACK.equals(acknowledgeFuture.get().getMessage()))
                ? Boolean.TRUE : Boolean.FALSE;
    }
}
