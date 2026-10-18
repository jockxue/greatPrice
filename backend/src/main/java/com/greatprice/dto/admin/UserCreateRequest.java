package com.greatprice.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "长度为 3 到 32 个字符")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "只能包含字母、数字、下划线和中划线")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 10, max = 72, message = "长度为 10 到 72 个字符")
        String password,
        @NotBlank(message = "显示名称不能为空")
        @Size(max = 32, message = "不能超过 32 个字符")
        String displayName,
        @NotBlank(message = "角色不能为空")
        String role) {
}
