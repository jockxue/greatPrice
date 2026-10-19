package com.greatprice.integration.taobao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TaobaoProperties.class)
public class TaobaoConfig {

    @Bean
    public TaobaoClock taobaoClock() {
        return new ShanghaiTaobaoClock();
    }

    @Bean
    public TaobaoRetryDelay taobaoRetryDelay() {
        return new SleepingTaobaoRetryDelay();
    }

    @Bean
    @ConditionalOnMissingBean(TaobaoTransport.class)
    public TaobaoTransport taobaoTransport() {
        return new JdkTaobaoTransport();
    }

    @Bean
    public TaobaoTopClient taobaoTopClient(TaobaoProperties properties, TaobaoTransport transport,
                                           TaobaoClock clock, TaobaoRetryDelay delay) {
        return new TaobaoTopClient(properties, transport, clock, delay);
    }
}
