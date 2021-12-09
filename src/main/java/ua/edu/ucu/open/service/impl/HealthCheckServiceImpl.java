package ua.edu.ucu.open.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import ua.edu.ucu.open.grpc.ReplicatedLogClient;
import ua.edu.ucu.open.model.HealthCheckStatus;
import ua.edu.ucu.open.service.HealthCheckService;

import java.util.Map;

@RestController
@Slf4j
public class HealthCheckServiceImpl implements HealthCheckService {

    private RestTemplate restTemplate;
    private Map<String, String> heartBeatPorts;

    @Autowired
    public HealthCheckServiceImpl(RestTemplateBuilder builder, Map<String, String> heartBeatPorts) {
        this.restTemplate = builder.build();
        this.heartBeatPorts = heartBeatPorts;
    }

    @Override
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
