package com.cx.plugin.configuration;

import java.net.MalformedURLException;

import org.slf4j.Logger;

import com.cx.restclient.CxClientDelegator;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.exception.CxClientException;
import com.cx.restclient.sast.utils.LegacyClient;

public class CommonClientFactory {
    public static final String SCAN_ORIGIN = "Bamboo";

    static LegacyClient getInstance(CxCredentials credentials,
                                    boolean enableCertificateValidation,
                                    Logger log, boolean isProxy)
            throws MalformedURLException, CxClientException {
        CxScanConfig scanConfig = new CxScanConfig(credentials.getServerUrl(),
                credentials.getUsername(),credentials.getPassword(),
                SCAN_ORIGIN,
                !enableCertificateValidation);

        if (isProxy) {
            scanConfig.setProxyConfig(ProxyHelper.getProxyConfig());
        }

        return getInstance(scanConfig, log);
    }

   public static LegacyClient getInstance(CxScanConfig config, Logger log)
            throws MalformedURLException, CxClientException {
        return new LegacyClient(config, log) {
        };
    }

   public static CxClientDelegator getClientDelegatorInstance(CxScanConfig config, Logger log)
            throws MalformedURLException, CxClientException {
        return new CxClientDelegator(config, log);
    }
}
