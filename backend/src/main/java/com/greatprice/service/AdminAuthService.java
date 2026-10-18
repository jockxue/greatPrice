package com.greatprice.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.exception.BizException;
import com.greatprice.common.web.DbTimes;
import com.greatprice.dto.admin.AdminProfileResponse;
import com.greatprice.dto.admin.LoginRequest;
import com.greatprice.entity.AdminUser;
import com.greatprice.mapper.AdminUserMapper;
import com.greatprice.security.AdminActors;
import com.greatprice.security.AdminUserDetails;
import com.greatprice.security.LoginAttemptGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminAuthService {

    static final int LOCK_AFTER = 5;
    static final String BAD_CREDENTIALS = "用户名或密码错误";
    static final String LOCKED = "登录失败次数过多，请 15 分钟后再试";

    private final AdminUserMapper adminUserMapper;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final LoginAttemptGuard loginAttemptGuard;
    private final AuditService auditService;
    private final AdminActors adminActors;

    public AdminAuthService(AdminUserMapper adminUserMapper,
                            AuthenticationManager authenticationManager,
                            SecurityContextRepository securityContextRepository,
                            LoginAttemptGuard loginAttemptGuard,
                            AuditService auditService,
                            AdminActors adminActors) {
        this.adminUserMapper = adminUserMapper;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.loginAttemptGuard = loginAttemptGuard;
        this.auditService = auditService;
        this.adminActors = adminActors;
    }

    public AdminProfileResponse login(LoginRequest request, HttpServletRequest http, HttpServletResponse response) {
        String username = request.username().strip();
        loginAttemptGuard.check(username);
        AdminUser existing = adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getUsername, username));
        if (existing != null && isLocked(existing)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, LOCKED);
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, http, response);
            resetFailures(existing);
            loginAttemptGuard.success(username);
            AdminUserDetails details = (AdminUserDetails) authentication.getPrincipal();
            auditService.record(details.getId(), details.getUsername(), AuditService.LOGIN_SUCCESS, "AUTH", details.getId(), null);
            return details.toProfile();
        } catch (BadCredentialsException ex) {
            loginAttemptGuard.fail(username);
            boolean locked = recordFailure(existing);
            auditService.record(existing == null ? null : existing.getId(), username, AuditService.LOGIN_FAILURE, "AUTH",
                    existing == null ? null : existing.getId(), null);
            throw new BizException(ErrorCode.UNAUTHORIZED, locked ? LOCKED : BAD_CREDENTIALS);
        } catch (DisabledException ex) {
            auditService.record(existing == null ? null : existing.getId(), username, AuditService.LOGIN_FAILURE, "AUTH",
                    existing == null ? null : existing.getId(), "disabled");
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号已停用");
        }
    }

    public void logout(HttpServletRequest request) {
        AdminUserDetails details = adminActors.current();
        auditService.record(details.getId(), details.getUsername(), AuditService.LOGOUT, "AUTH", details.getId(), null);
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public AdminProfileResponse me() {
        return adminActors.current().toProfile();
    }

    private boolean isLocked(AdminUser user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(DbTimes.now());
    }

    private void resetFailures(AdminUser user) {
        if (user == null) {
            return;
        }
        if ((user.getFailedLoginCount() == null || user.getFailedLoginCount() == 0) && user.getLockedUntil() == null) {
            return;
        }
        adminUserMapper.update(null, Wrappers.<AdminUser>lambdaUpdate()
                .eq(AdminUser::getId, user.getId())
                .set(AdminUser::getFailedLoginCount, 0)
                .set(AdminUser::getLockedUntil, null));
    }

    private boolean recordFailure(AdminUser user) {
        if (user == null) {
            return false;
        }
        int count = (user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount()) + 1;
        LocalDateTime lockedUntil = count >= LOCK_AFTER ? DbTimes.now().plusMinutes(15) : user.getLockedUntil();
        adminUserMapper.update(null, Wrappers.<AdminUser>lambdaUpdate()
                .eq(AdminUser::getId, user.getId())
                .set(AdminUser::getFailedLoginCount, count)
                .set(AdminUser::getLockedUntil, lockedUntil));
        return count >= LOCK_AFTER;
    }
}
