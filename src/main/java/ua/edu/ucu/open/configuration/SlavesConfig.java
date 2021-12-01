package ua.edu.ucu.open.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ua.edu.ucu.open.grpc.AsyncReplicatedLogClient;
import ua.edu.ucu.open.grpc.AsyncReplicatedLogClientImpl;

import java.util.List;

@Configuration
public class SlavesConfig {

    private static int id = 1;

    @Bean
    public AsyncReplicatedLogClient getFirstSlave() {
        return new AsyncReplicatedLogClientImpl(6567, id++);
    }

    @Bean
    public AsyncReplicatedLogClient getSecondSlave() {
        return new AsyncReplicatedLogClientImpl(6568, id++);
    }

    @Bean
    public List<AsyncReplicatedLogClient> slaves() {
        return List.of(getFirstSlave(), getSecondSlave());
    }
}
