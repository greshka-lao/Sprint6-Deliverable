package com.fdmgroup.sprint4Deliverable.Service;

import org.springframework.stereotype.Service;

import com.fdmgroup.sprint4Deliverable.Model.AdminSecurityEvent;
import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Repository.AdminSecurityEventRepository;

@Service
public class AdminSecurityEventService {

    private final AdminSecurityEventRepository repository;

    public AdminSecurityEventService(AdminSecurityEventRepository repository) {
        this.repository = repository;
    }

    public void record(AdminUser adminUser, String eventType, String outcome, String subject) {
        repository.save(new AdminSecurityEvent(adminUser, eventType, outcome, subject));
    }
}
