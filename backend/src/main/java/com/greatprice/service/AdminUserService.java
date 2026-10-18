package com.greatprice.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.exception.BizException;
import com.greatprice.common.web.DbTimes;
import com.greatprice.dto.admin.AdminUserResponse;
import com.greatprice.dto.admin.UserCreateRequest;
import com.greatprice.entity.AdminUser;
import com.greatprice.mapper.AdminUserMapper;
import com.greatprice.security.AdminActors;
import com.greatprice.security.AdminUserDetails;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AdminUserService {

    private static final Set<String> ROLES = Set.of(AdminUser.ROLE_ADMIN, AdminUser.ROLE_OPERATOR);

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AdminActors adminActors;

    public AdminUserService(AdminUserMapper adminUserMapper, PasswordEncoder passwordEncoder,
                            AuditService auditService, AdminActors adminActors) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.adminActors = adminActors;
    }

    public List<AdminUserResponse> list() {
        adminActors.requireAdmin();
        return adminUserMapper.selectList(Wrappers.<AdminUser>lambdaQuery().orderByAsc(AdminUser::getId)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminUserResponse create(UserCreateRequest request) {
        AdminUserDetails actor = adminActors.requireAdmin();
        if (!ROLES.contains(request.role())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "角色不合法");
        }
        AdminUser user = new AdminUser();
        user.setUsername(request.username().strip());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().strip());
        user.setRole(request.role());
        user.setStatus(AdminUser.STATUS_ACTIVE);
        user.setFailedLoginCount(0);
        try {
            adminUserMapper.insert(user);
        } catch (DataIntegrityViolationException ex) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户名已存在");
        }
        auditService.record(actor.getId(), actor.getUsername(), AuditService.USER_CREATE, "ADMIN_USER", user.getId(),
                user.getUsername() + " " + user.getRole());
        return toResponse(adminUserMapper.selectById(user.getId()));
    }

    @Transactional
    public AdminUserResponse disable(Long id) {
        AdminUserDetails actor = adminActors.requireAdmin();
        AdminUser user = require(id);
        if (AdminUser.STATUS_DISABLED.equals(user.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "账号已停用");
        }
        if (AdminUser.ROLE_ADMIN.equals(user.getRole())) {
            Long others = adminUserMapper.selectCount(Wrappers.<AdminUser>lambdaQuery()
                    .eq(AdminUser::getRole, AdminUser.ROLE_ADMIN)
                    .eq(AdminUser::getStatus, AdminUser.STATUS_ACTIVE)
                    .ne(AdminUser::getId, id));
            if (others == null || others == 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "不能停用唯一的管理员");
            }
        }
        if (actor.getId().equals(id)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不能停用当前登录账号");
        }
        adminUserMapper.update(null, Wrappers.<AdminUser>lambdaUpdate()
                .eq(AdminUser::getId, id)
                .set(AdminUser::getStatus, AdminUser.STATUS_DISABLED));
        auditService.record(actor.getId(), actor.getUsername(), AuditService.USER_DISABLE, "ADMIN_USER", id, user.getUsername());
        return toResponse(require(id));
    }

    @Transactional
    public AdminUserResponse enable(Long id) {
        AdminUserDetails actor = adminActors.requireAdmin();
        AdminUser user = require(id);
        if (AdminUser.STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "账号已启用");
        }
        adminUserMapper.update(null, Wrappers.<AdminUser>lambdaUpdate()
                .eq(AdminUser::getId, id)
                .set(AdminUser::getStatus, AdminUser.STATUS_ACTIVE));
        auditService.record(actor.getId(), actor.getUsername(), AuditService.USER_ENABLE, "ADMIN_USER", id, user.getUsername());
        return toResponse(require(id));
    }

    private AdminUser require(Long id) {
        AdminUser user = adminUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        return user;
    }

    private AdminUserResponse toResponse(AdminUser user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                DbTimes.toOffset(user.getCreatedAt()));
    }
}
