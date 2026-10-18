package com.greatprice.common.exception;

import com.greatprice.common.api.ApiResponse;
import com.greatprice.common.api.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.DemoController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class,
        org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.DemoController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void bizExceptionUsesItsErrorCode() throws Exception {
        mockMvc.perform(get("/test/biz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("好价不存在"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void invalidRequestBodyReturns400() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value(containsString("title")));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void invalidRequestParamReturns400() throws Exception {
        mockMvc.perform(get("/test/param").param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("size 不能超过 100"));
    }

    @Test
    void missingRequestParamReturns400() throws Exception {
        mockMvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("缺少参数: size"));
    }

    @Test
    void typeMismatchReturns400() throws Exception {
        mockMvc.perform(get("/test/param").param("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("参数格式错误: size"));
    }

    @Test
    void unknownPathReturns404() throws Exception {
        mockMvc.perform(get("/test/not-exists"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(delete("/test/biz"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(40500));
    }

    @Test
    void unexpectedExceptionHidesDetails() throws Exception {
        mockMvc.perform(get("/test/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(50000))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getMessage()));
    }

    record DemoRequest(@NotBlank String title) {
    }

    @RestController
    @RequestMapping("/test")
    static class DemoController {

        @GetMapping("/biz")
        ApiResponse<Void> biz() {
            throw new BizException(ErrorCode.NOT_FOUND, "好价不存在");
        }

        @PostMapping("/body")
        ApiResponse<Void> body(@Valid @RequestBody DemoRequest request) {
            return ApiResponse.ok();
        }

        @GetMapping("/param")
        ApiResponse<Integer> param(@RequestParam @Max(value = 100, message = "size 不能超过 100") Integer size) {
            return ApiResponse.ok(size);
        }

        @GetMapping("/error")
        ApiResponse<Void> error() {
            throw new IllegalStateException("database password is xxx");
        }
    }
}
