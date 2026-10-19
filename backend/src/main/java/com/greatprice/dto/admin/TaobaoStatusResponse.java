package com.greatprice.dto.admin;

import java.time.OffsetDateTime;

public record TaobaoStatusResponse(
        boolean enabled,
        boolean manualSyncEnabled,
        boolean appKeyConfigured,
        String appKeyMasked,
        boolean appSecretConfigured,
        boolean siteIdConfigured,
        boolean adzoneIdConfigured,
        boolean sessionConfigured,
        String gatewayHost,
        boolean searchConfigured,
        boolean detailConfigured,
        boolean promotionConfigured,
        String lastTestStatus,
        String lastTestMessage,
        String lastTestErrorCode,
        OffsetDateTime lastTestAt) {
}
