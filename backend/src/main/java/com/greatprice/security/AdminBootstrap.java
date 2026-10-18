package com.greatprice.security;

import com.greatprice.entity.AdminUser;
import com.greatprice.mapper.AdminUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 只在账号表为空、且环境变量同时提供用户名和密码时创建第一个管理员。
 * 不内置默认密码；已有账号时不会重置密码。
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final boolean enabled;
    private final String username;
    private final String password;
    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(@Value("${app.admin.init-enabled:true}") boolean enabled,
                          @Value("${app.admin.init-username:}") String username,
                          @Value("${app.admin.init-password:}") String password,
                          AdminUserMapper adminUserMapper,
                          PasswordEncoder passwordEncoder) {
        this.enabled = enabled;
        this.username = username == null ? "" : username.strip();
        this.password = password == null ? "" : password;
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || username.isBlank() || password.isBlank()) {
            return;
        }
        if (!username.matches("^[A-Za-z0-9_-]{3,32}$")) {
            log.error("ADMIN_INIT_USERNAME 格式不正确，已跳过管理员初始化");
            return;
        }
        if (password.length() < 10 || password.length() > 72) {
            log.error("ADMIN_INIT_PASSWORD 长度需为 10 到 72，已跳过管理员初始化");
            return;
        }
        Long count = adminUserMapper.selectCount(null);
        if (count != null && count > 0) {
            log.info("已存在管理员账号，跳过初始化");
            return;
        }
        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(username);
        user.setRole(AdminUser.ROLE_ADMIN);
        user.setStatus(AdminUser.STATUS_ACTIVE);
        user.setFailedLoginCount(0);
        try {
            adminUserMapper.insert(user);
            log.info("已创建初始管理员 username={}", username);
        } catch (DataIntegrityViolationException ex) {
            log.info("初始管理员已存在，跳过初始化");
        }
    }
}
