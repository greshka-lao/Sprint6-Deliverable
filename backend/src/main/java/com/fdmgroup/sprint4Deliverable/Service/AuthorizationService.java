package com.fdmgroup.sprint4Deliverable.Service;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Model.Role;

@Service
public class AuthorizationService {

    private static final Set<String> ADMIN_ROLES = Set.of("BANK_TELLER", "BANK_MANAGER");

    public boolean hasAdminAccess(AdminUser adminUser) {
        if (adminUser == null || adminUser.getRole() == null || adminUser.getRole().getName() == null) {
            return false;
        }

        return ADMIN_ROLES.contains(adminUser.getRole().getName().toUpperCase());
    }

    public boolean hasAccess(AdminUser adminUser, String requiredRole) {
        if (adminUser == null || adminUser.getRole() == null) {
            return false;
        }

        String userRole = adminUser.getRole().getName();
        if (requiredRole == null) {
            return hasAdminAccess(adminUser);
        }

        return userRole.equalsIgnoreCase(requiredRole)
            || ("ADMIN".equalsIgnoreCase(requiredRole) && hasAdminAccess(adminUser));
    }

    public Role getRequiredRole(String roleName) {
        return new Role(roleName, "Role-based access control");
    }
}
