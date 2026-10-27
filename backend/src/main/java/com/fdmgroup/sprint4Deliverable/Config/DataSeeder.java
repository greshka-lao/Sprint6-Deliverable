package com.fdmgroup.sprint4Deliverable.Config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Model.Role;
import com.fdmgroup.sprint4Deliverable.Repository.AdminUserRepository;
import com.fdmgroup.sprint4Deliverable.Repository.RoleRepository;

@Component
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final AdminUserRepository adminUserRepository;

    public DataSeeder(RoleRepository roleRepository, AdminUserRepository adminUserRepository) {
        this.roleRepository = roleRepository;
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Role tellerRole = roleRepository.findByName("BANK_TELLER")
                .orElseGet(() -> roleRepository.save(new Role("BANK_TELLER", "Bank teller with admin access")));

        Role managerRole = roleRepository.findByName("BANK_MANAGER")
                .orElseGet(() -> roleRepository.save(new Role("BANK_MANAGER", "Bank manager with admin access")));

        if (!adminUserRepository.existsByUsername("bank.teller")) {
            adminUserRepository.save(new AdminUser("bank.teller", "bank.teller@hooperbank.local", "password123", tellerRole));
        }

        if (!adminUserRepository.existsByUsername("bank.manager")) {
            adminUserRepository.save(new AdminUser("bank.manager", "bank.manager@hooperbank.local", "password123", managerRole));
        }
    }
}
