package ua.edu.ucu.open.service.impl;

import org.springframework.web.bind.annotation.RestController;
import ua.edu.ucu.open.service.HealthCheckService;

@RestController
public class HealthCheckServiceImpl implements HealthCheckService {
    @Override
    public String healthCheck(String uri) {
        return null;
    }
    //    private final RestTemplate restTemplate;
//
//    Autowired
//    public HealthCheckServiceImpl(RestTemplate restTemplate) {
//        this.restTemplate = restTemplate;
//    }
//
//    @Override
//    public String healthCheck(String uri) {
//        restTemplate.
//        return null;
//    }
}
