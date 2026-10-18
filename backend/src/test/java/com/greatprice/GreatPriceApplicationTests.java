package com.greatprice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 验证完整 Spring 上下文（含 MyBatis-Plus 自动配置）能启动。不需要 MySQL 运行：连接池在首次使用时才建立连接。
 */
@SpringBootTest
class GreatPriceApplicationTests {

    @Test
    void contextLoads() {
    }
}
