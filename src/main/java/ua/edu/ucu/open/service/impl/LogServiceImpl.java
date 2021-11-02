package ua.edu.ucu.open.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.edu.ucu.open.exception.InconsistentException;
import ua.edu.ucu.open.grpc.ReplicatedLogClient;
import ua.edu.ucu.open.grpc.ReplicatedLogClientSecond;
import ua.edu.ucu.open.repo.LogRepository;
import ua.edu.ucu.open.service.LogService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private static final String ACK = "ACK";

    private final LogRepository logRepository;
    private final ReplicatedLogClient replicatedLogClient;
    private final ReplicatedLogClientSecond replicatedLogClientSecond;

    @Override
    public List<String> getAll() {
        return logRepository.getAll();
    }

    @Override
    public void add(String log) throws InconsistentException {
        logRepository.add(log);

        String ack1 = replicatedLogClient.storeLog(log);
        String ack2 = replicatedLogClientSecond.storeLog(log);

        if (!ACK.equals(ack1) || !ACK.equals(ack2)) {
            throw new InconsistentException("The log is not saved.");
        }

    }
}
