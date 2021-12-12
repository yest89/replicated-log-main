package ua.edu.ucu.open.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.edu.ucu.open.grpc.ReplicatedLogClient;
import ua.edu.ucu.open.service.HealthCheckService;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckServiceImpl implements HealthCheckService {

    private final Map<ReplicatedLogClient, Boolean> heartBeatStatuses;

    @Override
    public boolean healthCheck(ReplicatedLogClient client) {
        return heartBeatStatuses.getOrDefault(client, Boolean.FALSE);
    }
}
