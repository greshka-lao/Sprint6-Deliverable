package com.fdmgroup.sprint4Deliverable.Security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Repository.AdminUserRepository;

@Service
public class AdminUserDetailsService implements UserDetailsService, UserDetailsPasswordService {

    private final AdminUserRepository adminUserRepository;

    public AdminUserDetailsService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser adminUser = findAdminUser(username);
        if (!adminUser.isEnabled() || !isEligibleRole(adminUser)) {
            throw new UsernameNotFoundException("Administrator account is unavailable");
        }
        return new AdminUserDetails(adminUser);
    }

    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        AdminUser adminUser = findAdminUser(user.getUsername());
        adminUser.setPassword(newPassword);
        return new AdminUserDetails(adminUserRepository.save(adminUser));
    }

    private AdminUser findAdminUser(String username) {
        return adminUserRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("Administrator account not found"));
    }

    private boolean isEligibleRole(AdminUser adminUser) {
        String roleName = adminUser.getRole() == null ? null : adminUser.getRole().getName();
        return "BANK_TELLER".equalsIgnoreCase(roleName) || "BANK_MANAGER".equalsIgnoreCase(roleName);
    }
}
