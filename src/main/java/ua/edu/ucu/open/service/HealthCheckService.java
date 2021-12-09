package ua.edu.ucu.open.service;

import ua.edu.ucu.open.grpc.ReplicatedLogClient;

public interface HealthCheckService {
    boolean healthCheck(ReplicatedLogClient client);
}
