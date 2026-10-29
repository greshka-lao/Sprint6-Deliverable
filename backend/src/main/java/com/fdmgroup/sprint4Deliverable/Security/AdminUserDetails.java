package com.fdmgroup.sprint4Deliverable.Security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fdmgroup.sprint4Deliverable.Model.AdminUser;

public class AdminUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String role;
    private final boolean enabled;

    public AdminUserDetails(AdminUser adminUser) {
        this.id = adminUser.getId();
        this.username = adminUser.getUsername();
        this.password = adminUser.getPassword();
        this.role = adminUser.getRole().getName();
        this.enabled = adminUser.isEnabled();
    }

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
