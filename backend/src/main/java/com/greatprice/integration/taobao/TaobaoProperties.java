package com.greatprice.integration.taobao;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * 淘宝联盟配置。密钥只存在于后端环境变量，不提供写接口。
 * 网关默认值来自淘宝开放平台 API 文档中的正式环境 HTTPS 地址：https://eco.taobao.com/router/rest
 */
@ConfigurationProperties(prefix = "app.taobao")
public class TaobaoProperties {

    private boolean enabled;
    private boolean manualSyncEnabled;
    private String appKey = "";
    private String appSecret = "";
    private String adzoneId = "";
    private String siteId = "";
    private String session = "";
    private String gatewayUrl = "https://eco.taobao.com/router/rest";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 8000;
    private int maxRetries = 2;

    public boolean searchReady() {
        return callReady() && present(appKey) && present(appSecret) && positiveNumber(adzoneId);
    }

    public boolean detailReady() {
        return callReady() && present(appKey) && present(appSecret);
    }

    public boolean promotionReady() {
        return searchReady() && positiveNumber(siteId) && present(session);
    }

    public boolean httpsGateway() {
        if (gatewayUrl == null || gatewayUrl.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(gatewayUrl.strip());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && !uri.getHost().isBlank()
                    && uri.getUserInfo() == null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public String gatewayHost() {
        if (!httpsGateway()) {
            return null;
        }
        return URI.create(gatewayUrl.strip()).getHost();
    }

    public String maskedAppKey() {
        if (!present(appKey)) {
            return null;
        }
        String value = appKey.strip();
        if (value.length() < 8) {
            return "已配置";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 2);
    }

    /** 额外重试次数，最多 3 次，避免配置错误导致无限打平台。 */
    public int extraRetries() {
        return Math.min(Math.max(maxRetries, 0), 3);
    }

    private boolean callReady() {
        return enabled && manualSyncEnabled && httpsGateway();
    }

    public static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    public static boolean positiveNumber(String value) {
        return value != null && value.strip().matches("[1-9]\\d{0,17}");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isManualSyncEnabled() {
        return manualSyncEnabled;
    }

    public void setManualSyncEnabled(boolean manualSyncEnabled) {
        this.manualSyncEnabled = manualSyncEnabled;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getAdzoneId() {
        return adzoneId;
    }

    public void setAdzoneId(String adzoneId) {
        this.adzoneId = adzoneId;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}
