package ua.edu.ucu.open.grpc;

import com.google.common.util.concurrent.ListenableFuture;
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
public class AsyncReplicatedLogClientSecond {

    private ReplicatedLogServiceGrpc.ReplicatedLogServiceFutureStub replicatedLogService;

    @PostConstruct
    private void init() {
        ManagedChannel managedChannel = ManagedChannelBuilder
                .forAddress("localhost", 6568).usePlaintext().build();

        replicatedLogService = ReplicatedLogServiceGrpc.newFutureStub(managedChannel);
    }

    public ListenableFuture<Acknowledge> storeLog(String logMessage) {
        Log logToStore = Log.newBuilder()
                .setLog(logMessage)
                .build();
        log.debug("log: {}", logToStore.getLog());

        return replicatedLogService.storeLog(logToStore);
    }
}
