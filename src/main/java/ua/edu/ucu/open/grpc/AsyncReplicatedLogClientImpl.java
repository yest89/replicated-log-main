package ua.edu.ucu.open.grpc;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import ua.edu.ucu.open.grpc.log.Acknowledge;
import ua.edu.ucu.open.grpc.log.Log;
import ua.edu.ucu.open.grpc.log.ReplicatedLogServiceGrpc;

import javax.annotation.PostConstruct;

@Slf4j
public class AsyncReplicatedLogClientImpl implements AsyncReplicatedLogClient {

    private ReplicatedLogServiceGrpc.ReplicatedLogServiceFutureStub replicatedLogService;
    private int port;
    private int id;

    public AsyncReplicatedLogClientImpl(int port, int id) {
        this.port = port;
        this.id = id;
    }

    @PostConstruct
    private void init() {
        ManagedChannel managedChannel = NettyChannelBuilder
                .forAddress("host.docker.internal", port)
                .usePlaintext()
                .build();

        replicatedLogService = ReplicatedLogServiceGrpc.newFutureStub(managedChannel);
    }

    public ListenableFuture<Acknowledge> storeLog(String logMessage, int ordinal) {
        Log logToStore = Log.newBuilder()
                .setLog(logMessage)
                .setOrdinal(ordinal)
                .build();
        log.debug("log: {}", logToStore.getLog());

        return replicatedLogService.storeLog(logToStore);
    }


    @Override
    public int getClientId() {
        return id;
    }
}
