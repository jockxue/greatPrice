package com.greatprice.security;

import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AdminActors {

    public AdminUserDetails current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AdminUserDetails details) || !details.isEnabled()) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return details;
    }

    public AdminUserDetails requireAdmin() {
        AdminUserDetails details = current();
        if (!details.isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return details;
    }
}
