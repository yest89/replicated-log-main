package ua.edu.ucu.open.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import ua.edu.ucu.open.model.HealthCheckStatus;
import ua.edu.ucu.open.service.HealthCheckService;

@RestController
public class HealthCheckServiceImpl implements HealthCheckService {

    private RestTemplate restTemplate;
    private final static String URI_FOR_FIRST_SLAVE = "http://localhost:8091/api/v1/healthcheck";
    private final static String URI_FOR_SECOND_SLAVE = "http://localhost:8092/api/v1/healthcheck";

    @Autowired
    public HealthCheckServiceImpl(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    @Override
    public boolean healthCheckForFirstSlave() {
        RestTemplate restTemplate = new RestTemplate();
        String status = restTemplate.getForObject(URI_FOR_FIRST_SLAVE, String.class);

        return HealthCheckStatus.Healthy.name().equals(status);
    }

    @Override
    public boolean healthCheckForSecondSlave() {
        RestTemplate restTemplate = new RestTemplate();
        String status = restTemplate.getForObject(URI_FOR_SECOND_SLAVE, String.class);

        return HealthCheckStatus.Healthy.name().equals(status);
    }
}
