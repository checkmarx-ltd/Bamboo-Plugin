package com.cx.plugin.utils;

import com.atlassian.bamboo.security.EncryptionException;
import com.atlassian.bamboo.security.EncryptionServiceImpl;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.ProxyConfig;
import com.cx.restclient.dto.ScanResults;

import static com.cx.plugin.utils.CxParam.CUSTOM_CONFIGURATION_SERVER;
import static com.cx.plugin.utils.CxParam.SERVER_CREDENTIALS_SECTION;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * Created by Galn on 24/10/2017.
 */

public abstract class CxPluginUtils {

    public static void printConfiguration(CxScanConfig config, CxConfigHelper configBFF, CxLoggerAdapter log) {
        log.info("---------------------------------------Configurations:------------------------------------");
        log.info("Bamboo plugin version: " + configBFF.getPluginVersion());
        log.info("Username: " + config.getUsername());
        log.info(" Server URL: " + config.getUrl());
        if (CUSTOM_CONFIGURATION_SERVER.equals(config.getScannerTypes())) {
            log.info("Using job-specific dependency scan configuration.");
        }else{
            log.info("Using globally defined dependency scan configuration.");
        }
        log.info("---------------------------Proxy Configuration----------------------");
        if(config.isProxy()){
            ProxyConfig proxy = config.getProxyConfig();
            String proxyHost =proxy.getHost();
            int port=proxy.getPort();
            String userName=proxy.getUsername();
            String password= proxy.getPassword();

            if(proxyHost!=null && !proxyHost.isEmpty()){
                log.info("ProxyHost : "+proxyHost);
                log.info("Proxy Port : "+port);
                log.info("Proxy Username : "+userName);
            }else{
                log.info("Proxy Enabled but proxy is not configured");
            }
        }else{
            log.info("Proxy is not enabled");
        }
        log.info("-------------------------------------------------------------------");
        log.info("Project name: " + config.getProjectName());
        log.info("Deny new project creation: " + config.getDenyProject());
        log.info("Hide scan results: " + config.getHideResults());
        log.info("Scan timeout in minutes: " + (config.getSastScanTimeoutInMinutes() <= 0 ? "" : config.getSastScanTimeoutInMinutes()));
        log.info("Full team path: " + config.getTeamPath());
        log.info("Is synchronous scan: " + config.getSynchronous());
        if (config.isSastEnabled()) {
            log.info("SAST scan enabled: "+config.isSastEnabled());
            log.info("preset id: " + config.getPresetId());
            log.info("Preset: " + config.getPresetName());
            log.info("SAST folder exclusions: " + config.getSastFolderExclusions());
            log.info("SAST filter pattern: " + config.getSastFilterPattern());
            log.info("SAST timeout: " + config.getSastScanTimeoutInMinutes());
            log.info("SAST scan comment: " + config.getScanComment());
            log.info("is incremental scan: " + config.getIncremental());
            log.info("is generate full XML report: " + config.getGenerateXmlReport());
            log.info("is generate PDF report: " + config.getGeneratePDFReport());
            log.info("Policy violations enabled: " + config.getEnablePolicyViolations());
            log.info("source code encoding id: " + config.getEngineConfigurationId());
            log.info("CxSAST thresholds enabled: " + config.getSastThresholdsEnabled());
            if (config.getSastThresholdsEnabled()) {
                log.info("CxSAST high threshold: " + (config.getSastHighThreshold() == null ? "[No Threshold]" : config.getSastHighThreshold()));
                log.info("CxSAST medium threshold: " + (config.getSastMediumThreshold() == null ? "[No Threshold]" : config.getSastMediumThreshold()));
                log.info("CxSAST low threshold: " + (config.getSastLowThreshold() == null ? "[No Threshold]" : config.getSastLowThreshold()));
            }
        }
        log.info("avoid duplicated projects scans: " + config.isAvoidDuplicateProjectScans());
        log.info("Is interval full scans enabled: " + configBFF.isIntervals());
        if (configBFF.isIntervals()) {
            log.info("Interval- begins: " + configBFF.getIntervalBegins());
            log.info("Interval- ends: " + configBFF.getIntervalEnds());
            String forceScan = config.getForceScan() ? "" : "NOT ";
            log.info("Override full scan: " + config.getForceScan() + " (Interval based full scan " + forceScan + "activated.)");
        }
        log.info("CxOSA enabled: " + config.isOsaEnabled());
        if (config.isOsaEnabled()) {
            log.info("Dependency scan configuration:");
            log.info("  folder exclusions: " + config.getOsaFolderExclusions());
            log.info("CxOSA filter patterns: " + config.getOsaFilterPattern());
            log.info("CxOSA archive extract patterns: " + config.getOsaArchiveIncludePatterns());
            log.info("Execute dependency managers 'install packages' command before Scan: " + config.getOsaRunInstall());

            log.info("CxOSA thresholds enabled: " + config.getOsaThresholdsEnabled());
            if (config.getOsaThresholdsEnabled()) {
                log.info("CxOSA high threshold: " + (config.getOsaHighThreshold() == null ? "[No Threshold]" : config.getOsaHighThreshold()));
                log.info("CxOSA medium threshold: " + (config.getOsaMediumThreshold() == null ? "[No Threshold]" : config.getOsaMediumThreshold()));
                log.info("CxOSA low threshold: " + (config.getOsaLowThreshold() == null ? "[No Threshold]" : config.getOsaLowThreshold()));
            }
        }


        log.info("------------------------------------------------------------------------------------------");
    }

    public static void printBuildFailure(String thDescription, ScanResults ret, CxLoggerAdapter log) {
        log.error("********************************************");
        log.error(" The Build Failed for the Following Reasons: ");
        log.error("********************************************");
        logError(ret.getGeneralException(), log);
        if (thDescription != null) {
            String[] lines = thDescription.split("\\n");
            for (String s : lines) {
                log.error(s);
            }

        }
        log.error("-----------------------------------------------------------------------------------------\n");
        log.error("");
    }

    private static void logError(Exception ex, Logger log){
        if (ex != null) {
            log.error(ex.getMessage());
        }
    }

    public static String decrypt(String str) {
        String encStr;
        if (isEncrypted(str)) {
            try {
                encStr = new EncryptionServiceImpl().decrypt(str);
            } catch (EncryptionException e) {
                encStr = "";
            }
            return encStr;
        } else {
            return str;
        }
    }

    public static String encrypt(String password) {
        String encPass;
        if (!isEncrypted(password)) {
            try {
                encPass = new EncryptionServiceImpl().encrypt(password);
            } catch (EncryptionException e) {
                encPass = "";
            }
            return encPass;
        } else {
            return password;
        }
    }

    public static boolean isEncrypted(String encryptStr) {
        try {
            new EncryptionServiceImpl().decrypt(encryptStr);
        } catch (EncryptionException e) {
            return false;
        }
        return true;
    }

    public static Integer resolveInt(String value, Logger log) {
        Integer inti = null;
        if (!StringUtils.isEmpty(value)) {
            try {
                inti = Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                log.warn("failed to parse integer value: " + value);
            }
        }
        return inti;
    }
}
