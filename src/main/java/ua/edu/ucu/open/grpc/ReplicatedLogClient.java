package ua.edu.ucu.open.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ua.edu.ucu.open.grpc.log.Acknowledge;
import ua.edu.ucu.open.grpc.log.Log;
import ua.edu.ucu.open.grpc.log.ReplicatedLogServiceGrpc;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class ReplicatedLogClient {

    private ReplicatedLogServiceGrpc.ReplicatedLogServiceBlockingStub replicatedLogServiceBlockingStub;

    @PostConstruct
    private void init() {
        ManagedChannel managedChannel = ManagedChannelBuilder
                .forAddress("localhost", 6565).usePlaintext().build();
        replicatedLogServiceBlockingStub =
                ReplicatedLogServiceGrpc.newBlockingStub(managedChannel);
    }

    public String storeLog(String logMessage) {
        Log logToStore = Log.newBuilder()
                .setLog(logMessage)
                .build();
        log.debug("log: {}", logToStore.getLog());

        Acknowledge acknowledge = replicatedLogServiceBlockingStub.storeLog(logToStore);

        log.debug("client received {}", acknowledge.getMessage());

        return acknowledge.getMessage();
    }
}
