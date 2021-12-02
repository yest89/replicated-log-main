package ua.edu.ucu.open.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import ua.edu.ucu.open.model.HealthCheckStatus;
import ua.edu.ucu.open.service.HealthCheckService;

import java.util.List;

@RestController
@Slf4j
public class HealthCheckServiceImpl implements HealthCheckService {

    private RestTemplate restTemplate;
    private List<String> heartBeatPorts;

    @Autowired
    public HealthCheckServiceImpl(RestTemplateBuilder builder, List<String> heartBeatPorts) {
        this.restTemplate = builder.build();
        this.heartBeatPorts = heartBeatPorts;
    }

    @Override
    public boolean healthCheck(int id) {
        String status = restTemplate.getForObject(heartBeatPorts.get(id), String.class);
        return HealthCheckStatus.Healthy.name().equals(status);
    }
}
