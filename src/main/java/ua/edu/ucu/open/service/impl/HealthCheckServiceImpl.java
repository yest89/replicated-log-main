package ua.edu.ucu.open.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import ua.edu.ucu.open.grpc.ReplicatedLogClient;
import ua.edu.ucu.open.model.HealthCheckStatus;
import ua.edu.ucu.open.service.HealthCheckService;

@RestController
@Slf4j
public class HealthCheckServiceImpl implements HealthCheckService {

    private RestTemplate restTemplate;

    @Autowired
    public HealthCheckServiceImpl(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    @Override
    public boolean healthCheck(ReplicatedLogClient client) {
        String status = restTemplate.getForObject(client.getPort(), String.class);
        return HealthCheckStatus.Healthy.name().equals(status);
    }
}
