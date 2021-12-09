package ua.edu.ucu.open.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import ua.edu.ucu.open.grpc.log.Acknowledge;
import ua.edu.ucu.open.grpc.log.Log;
import ua.edu.ucu.open.grpc.log.ReplicatedLogServiceGrpc;

import javax.annotation.PostConstruct;

@Slf4j
public class ReplicatedLogClientImpl implements ReplicatedLogClient {

    private ReplicatedLogServiceGrpc.ReplicatedLogServiceBlockingStub replicatedLogServiceBlockingStub;
    private int port;
    private int id;

    public ReplicatedLogClientImpl(int port, int id) {
        this.port = port;
        this.id = id;
    }

    @PostConstruct
    private void init() {
        ManagedChannel managedChannel = ManagedChannelBuilder
                .forAddress("host.docker.internal", port)
                .usePlaintext()
                .build();

        replicatedLogServiceBlockingStub = ReplicatedLogServiceGrpc.newBlockingStub(managedChannel);
    }

    public Acknowledge storeLog(String logMessage, int ordinal) {
        Log logToStore = Log.newBuilder()
                .setLog(logMessage)
                .setOrdinal(ordinal)
                .build();
        log.debug("log: {}", logToStore.getLog());

        return replicatedLogServiceBlockingStub.storeLog(logToStore);
    }

    @Override
    public int getClientId() {
        return id;
    }

    @Override
    public String getPort() {
        return String.valueOf(port);
    }
}
