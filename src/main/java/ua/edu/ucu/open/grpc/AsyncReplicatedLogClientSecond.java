package ua.edu.ucu.open.grpc;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ua.edu.ucu.open.grpc.log.Acknowledge;
import ua.edu.ucu.open.grpc.log.Log;
import ua.edu.ucu.open.grpc.log.ReplicatedLogServiceGrpc;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AsyncReplicatedLogClientSecond implements AsyncReplicatedLogClient {

    private ReplicatedLogServiceGrpc.ReplicatedLogServiceFutureStub replicatedLogService;

    @PostConstruct
    private void init() {
        ManagedChannel managedChannel = NettyChannelBuilder
                .forAddress("localhost", 6568)
                .usePlaintext()
                .enableRetry()
                .maxRetryAttempts(Integer.MAX_VALUE)
                .keepAliveWithoutCalls(true)
                .keepAliveTime(15, TimeUnit.SECONDS)
                .keepAliveTimeout(1, TimeUnit.MINUTES)
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
        return 2; //hardcode
    }
}
