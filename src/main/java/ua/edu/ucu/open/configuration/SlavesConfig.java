package ua.edu.ucu.open.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ua.edu.ucu.open.grpc.ReplicatedLogClient;
import ua.edu.ucu.open.grpc.ReplicatedLogClientImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Configuration
public class SlavesConfig {

    private static int id = 0;
    private final static String HOST = "http://host.docker.internal:";
    private final static String URI = "/api/v1/healthcheck";

    @Value("${portOfSlavesForGrpc}")
    private List<Integer> grpcPorts;

    @Value("${portOfSlavesForHttp}")
    private List<Integer> httpPorts;

    @Bean
    public ReplicatedLogClient getFirstSlave() {
        return new ReplicatedLogClientImpl(grpcPorts.get(id), httpPorts.get(id), id++);
    }

    @Bean
    public ReplicatedLogClient getSecondSlave() {
        return new ReplicatedLogClientImpl(grpcPorts.get(id), httpPorts.get(id), id++);
    }

    @Bean
    public List<ReplicatedLogClient> slaves() {
        return List.of(getFirstSlave(), getSecondSlave());
    }

    @Bean
    public Map<String, String> heartBeatPorts() {
        return httpPorts.stream()
                .collect(Collectors.toMap(
                        String::valueOf,
                        port -> HOST + port + URI));
    }

    @Bean
    public Map<ReplicatedLogClient, Boolean> heartBeatStatuses() {
        ConcurrentHashMap<ReplicatedLogClient, Boolean> result = new ConcurrentHashMap<>();
        result.putIfAbsent(getFirstSlave(), false);
        result.putIfAbsent(getSecondSlave(), false);
        return result;
    }
}
