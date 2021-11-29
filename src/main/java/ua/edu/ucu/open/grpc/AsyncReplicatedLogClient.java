package ua.edu.ucu.open.grpc;

import com.google.common.util.concurrent.ListenableFuture;
import ua.edu.ucu.open.grpc.log.Acknowledge;

public interface AsyncReplicatedLogClient {
    ListenableFuture<Acknowledge> storeLog(String logMessage, int ordinal);
    int getClientId();
}
