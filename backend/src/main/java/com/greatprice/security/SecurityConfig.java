package com.greatprice.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.greatprice.entity.AdminUser;
import com.greatprice.mapper.AdminUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final ApiSecurityHandlers securityHandlers;
    private final AdminUserMapper adminUserMapper;

    public SecurityConfig(ApiSecurityHandlers securityHandlers, AdminUserMapper adminUserMapper) {
        this.securityHandlers = securityHandlers;
        this.adminUserMapper = adminUserMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository)
            throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(CsrfToken.class.getName());

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(csrfHandler))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityHandlers)
                        .accessDeniedHandler(securityHandlers))
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/v1/auth/login").permitAll()
                        .requestMatchers("/api/admin/v1/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterAfter(new AdminReloadFilter(adminUserMapper), SecurityContextHolderFilter.class);
        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /** 让 CSRF Cookie 在首次请求时就写到浏览器，供管理后台读取后放进请求头。 */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }

    /** 每次请求从数据库刷新角色和停用状态，避免只信任登录时放进会话的身份。 */
    static final class AdminReloadFilter extends OncePerRequestFilter {
        private final AdminUserMapper adminUserMapper;

        AdminReloadFilter(AdminUserMapper adminUserMapper) {
            this.adminUserMapper = adminUserMapper;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            var context = org.springframework.security.core.context.SecurityContextHolder.getContext();
            var authentication = context.getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof AdminUserDetails details) {
                AdminUser user = adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getId, details.getId()));
                if (user == null || !AdminUser.STATUS_ACTIVE.equals(user.getStatus())) {
                    context.setAuthentication(null);
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                } else {
                    AdminUserDetails fresh = new AdminUserDetails(user);
                    var token = org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                            fresh, null, fresh.getAuthorities());
                    context.setAuthentication(token);
                }
            }
            filterChain.doFilter(request, response);
        }
    }
}
