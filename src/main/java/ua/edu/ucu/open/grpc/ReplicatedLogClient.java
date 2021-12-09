package ua.edu.ucu.open.grpc;

import ua.edu.ucu.open.grpc.log.Acknowledge;

public interface ReplicatedLogClient {
    Acknowledge storeLog(String logMessage, int ordinal);

    int getClientId();

    String getPort();
}
