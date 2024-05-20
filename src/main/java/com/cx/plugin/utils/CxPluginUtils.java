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
        log.info("Server URL: " + config.getUrl());
		
        if (configBFF.isUsingGlobalSASTServer()) {
        	log.info("Using globally defined CxSAST Server settings.");        	
        }else{
        	log.info("Using job-specific CxSAST Server settings.");
        }
        
        if (configBFF.isUsingGlobalSASTSettings()) {
        	log.info("Using globally defined CxSAST settings.");        	
        }else{
        	log.info("Using job-specific CxSAST settings.");
        }
        
        if (configBFF.isUsingGlobalScanControlSettings()) {
        	log.info("Using globally defined Scan Control settings.");        	
        }else{
        	log.info("Using job-specific Scan Control settings.");
        }
        
        if (configBFF.isUsingGlobalDependencyScan()) {
        	log.info("Using globally defined Dependency Scan settings.");        	
        }else{
        	log.info("Using job-specific Dependency Scan settings.");
        }        
        
        log.info("---------------------------Proxy Configuration----------------------");
        if(config.isProxy()){
            ProxyConfig proxy = config.getProxyConfig();
            String proxyHost =proxy.getHost();
                        
            if(proxyHost!=null && !proxyHost.isEmpty()){
				log.debug("Proxy host: " + proxy.getHost());
				log.debug("Proxy port: " + proxy.getPort());
				log.debug("Proxy user: " + proxy.getUsername());
				log.debug("Proxy password: *************");
				log.debug("Proxy Scheme: " + (proxy.isUseHttps() ? "https" : "http"));
				log.debug("Non Proxy Hosts: " + proxy.getNoproxyHosts());

            }else{
                log.warn("Proxy is enabled but proxy is not configured. Ignoring proxy status.");
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
        log.info("SAST scan enabled: "+config.isSastEnabled());
        if (config.isSastEnabled()) {            
            log.info("Preset id: " + config.getPresetId());
            log.info("Preset: " + config.getPresetName());
            log.info("SAST folder exclusions: " + config.getSastFolderExclusions());
            log.info("SAST filter pattern: " + config.getSastFilterPattern());
            log.info("SAST timeout: " + config.getSastScanTimeoutInMinutes());
            log.info("SAST scan comment: " + config.getScanComment());
            log.info("Is incremental scan(Effective): " + configBFF.isEffectiveIncrementalScan());
            log.info("is force scan: " + config.getForceScan());
            log.info("Is generate full XML report: " + config.getGenerateXmlReport());
            log.info("Is generate PDF report: " + config.getGeneratePDFReport());
            log.info("SAST and OSA Policy violations enabled: " + config.getEnablePolicyViolations());
            log.info("SCA Policy violations enabled: " + config.getEnablePolicyViolationsSCA());
            log.info("Source code encoding id: " + config.getEngineConfigurationId());
            log.info("CxSAST thresholds enabled: " + config.getSastThresholdsEnabled());
            if (config.getSastThresholdsEnabled()) {
                log.info("CxSAST critical threshold: " + (config.getSastCriticalThreshold() == null ? "[No Threshold]" : config.getSastCriticalThreshold()));
                log.info("CxSAST high threshold: " + (config.getSastHighThreshold() == null ? "[No Threshold]" : config.getSastHighThreshold()));
                log.info("CxSAST medium threshold: " + (config.getSastMediumThreshold() == null ? "[No Threshold]" : config.getSastMediumThreshold()));
                log.info("CxSAST low threshold: " + (config.getSastLowThreshold() == null ? "[No Threshold]" : config.getSastLowThreshold()));
            }
        }
        log.info("Avoid duplicated projects scans: " + config.isAvoidDuplicateProjectScans());
        log.info("Is interval full scans enabled: " + configBFF.isIntervals());
        if (configBFF.isIntervals()) {
            log.info("Interval- begins: " + configBFF.getIntervalBegins());
            log.info("Interval- ends: " + configBFF.getIntervalEnds());
            String fullScan = configBFF.isEffectiveIncrementalScan() ? "NOT " : "";
            log.info("Override full scan: " + !configBFF.isEffectiveIncrementalScan() + " (Interval based full scan " + fullScan + "activated.)");
        }
        
        log.info("Dependency Scan enabled : " + configBFF.isDependencyScanEnabled());        
        if(config.isOsaEnabled() || config.isAstScaEnabled()) {
	        log.info("Dependency Scan type : " + configBFF.getDependencyScanType().getDisplayName() );
	        log.info("Dependency scan configuration:");
	        log.info(" Folder exclusions: " + config.getOsaFolderExclusions());
	        log.info(" Include/Exclude Filter patterns: " + config.getOsaFilterPattern());	        
	        log.info(" Dependency Scan thresholds enabled: " + config.getOsaThresholdsEnabled());
	        if (config.getOsaThresholdsEnabled()) {
	            log.info(" Dependency Scan high threshold: " + (config.getOsaHighThreshold() == null ? "[No Threshold]" : config.getOsaHighThreshold()));
	            log.info(" Dependency Scan medium threshold: " + (config.getOsaMediumThreshold() == null ? "[No Threshold]" : config.getOsaMediumThreshold()));
	            log.info(" Dependency Scan low threshold: " + (config.getOsaLowThreshold() == null ? "[No Threshold]" : config.getOsaLowThreshold()));
	        }
	        if (config.isOsaEnabled()) {	            
	            log.info(" CxOSA archive extract patterns: " + config.getOsaArchiveIncludePatterns());
	            log.info(" Execute dependency managers 'install packages' command before CxOSA Scan: " + config.getOsaRunInstall());            
	        } else if(config.isAstScaEnabled())	{
	        	log.info(" CxSCA Tenant: " + config.getAstScaConfig().getTenant());
	        	log.info(" CxSCA TeamPath: " + config.getAstScaConfig().getTeamPath());
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
