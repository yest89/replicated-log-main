package ua.edu.ucu.open.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import ua.edu.ucu.open.grpc.ReplicatedLogClient;
import ua.edu.ucu.open.model.HealthCheckStatus;

import java.util.Map;

@RestController
@Slf4j
public class HealthCheckClient {

    private RestTemplate restTemplate;
    private Map<String, String> heartBeatPorts;

    @Autowired
    public HealthCheckClient(RestTemplateBuilder builder, Map<String, String> heartBeatPorts) {
        this.restTemplate = builder.build();
        this.heartBeatPorts = heartBeatPorts;
    }

    public boolean healthCheck(ReplicatedLogClient client) {
        String status;
        try {
            status = restTemplate.getForObject(heartBeatPorts.get(client.getHttpPort()), String.class);
        } catch (Exception ex) {
            return Boolean.FALSE;
        }
        return HealthCheckStatus.Healthy.name().equals(status);
    }
}
