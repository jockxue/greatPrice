package com.greatprice.security;

import com.greatprice.dto.admin.AdminProfileResponse;
import com.greatprice.entity.AdminUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AdminUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String displayName;
    private final String role;
    private final boolean enabled;

    public AdminUserDetails(AdminUser user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.displayName = user.getDisplayName();
        this.role = user.getRole();
        this.enabled = AdminUser.STATUS_ACTIVE.equals(user.getStatus());
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return AdminUser.ROLE_ADMIN.equals(role);
    }

    public AdminProfileResponse toProfile() {
        return new AdminProfileResponse(id, username, displayName, role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
