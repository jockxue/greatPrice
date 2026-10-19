package com.greatprice.service;

import com.greatprice.common.web.DbTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.deal-expiry.enabled", havingValue = "true", matchIfMissing = true)
public class DealExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(DealExpiryJob.class);

    private final DealExpiryService dealExpiryService;

    public DealExpiryJob(DealExpiryService dealExpiryService) {
        this.dealExpiryService = dealExpiryService;
    }

    @Scheduled(fixedDelayString = "${app.deal-expiry.interval-ms:60000}")
    public void expireDueDeals() {
        try {
            dealExpiryService.runOnce(DbTimes.now());
        } catch (RuntimeException ex) {
            log.error("自动过期任务异常，已跳过本次执行", ex);
        }
    }
}
