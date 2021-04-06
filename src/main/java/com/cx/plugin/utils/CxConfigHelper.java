package com.cx.plugin.utils;

import static com.cx.plugin.utils.CxParam.COMMENT;
import static com.cx.plugin.utils.CxParam.CUSTOM_CONFIGURATION_CONTROL;
import static com.cx.plugin.utils.CxParam.CUSTOM_CONFIGURATION_CXSAST;
import static com.cx.plugin.utils.CxParam.CUSTOM_CONFIGURATION_SERVER;
import static com.cx.plugin.utils.CxParam.CXSAST_SECTION;
import static com.cx.plugin.utils.CxParam.CXSCA_ACCESS_CONTROL_URL;
import static com.cx.plugin.utils.CxParam.CXSCA_ACCOUNT_NAME;
import static com.cx.plugin.utils.CxParam.CXSCA_API_URL;
import static com.cx.plugin.utils.CxParam.CXSCA_PWD;
import static com.cx.plugin.utils.CxParam.CXSCA_USERNAME;
import static com.cx.plugin.utils.CxParam.CXSCA_WEBAPP_URL;
import static com.cx.plugin.utils.CxParam.CX_REPORT_LOCATION;
import static com.cx.plugin.utils.CxParam.DEPENDENCY_SCAN_TYPE;
import static com.cx.plugin.utils.CxParam.ENABLE_DEPENDENCY_SCAN;
import static com.cx.plugin.utils.CxParam.ENABLE_PROXY;
import static com.cx.plugin.utils.CxParam.FILTER_PATTERN;
import static com.cx.plugin.utils.CxParam.FOLDER_EXCLUSION;
import static com.cx.plugin.utils.CxParam.GENERATE_PDF_REPORT;
import static com.cx.plugin.utils.CxParam.GLOBAL_DENY_PROJECT;
import static com.cx.plugin.utils.CxParam.GLOBAL_FILTER_PATTERN;
import static com.cx.plugin.utils.CxParam.GLOBAL_FOLDER_EXCLUSION;
import static com.cx.plugin.utils.CxParam.GLOBAL_HIDE_RESULTS;
import static com.cx.plugin.utils.CxParam.GLOBAL_HIGH_THRESHOLD;
import static com.cx.plugin.utils.CxParam.GLOBAL_IS_SYNCHRONOUS;
import static com.cx.plugin.utils.CxParam.GLOBAL_LOW_THRESHOLD;
import static com.cx.plugin.utils.CxParam.GLOBAL_MEDIUM_THRESHOLD;
import static com.cx.plugin.utils.CxParam.GLOBAL_OSA_HIGH_THRESHOLD;
import static com.cx.plugin.utils.CxParam.GLOBAL_OSA_LOW_THRESHOLD;
import static com.cx.plugin.utils.CxParam.GLOBAL_OSA_MEDIUM_THRESHOLD;
import static com.cx.plugin.utils.CxParam.GLOBAL_OSA_THRESHOLDS_ENABLED;
import static com.cx.plugin.utils.CxParam.GLOBAL_POLICY_VIOLATION_ENABLED;
import static com.cx.plugin.utils.CxParam.GLOBAL_PWD;
import static com.cx.plugin.utils.CxParam.GLOBAL_SCAN_TIMEOUT_IN_MINUTES;
import static com.cx.plugin.utils.CxParam.GLOBAL_SERVER_URL;
import static com.cx.plugin.utils.CxParam.GLOBAL_THRESHOLDS_ENABLED;
import static com.cx.plugin.utils.CxParam.GLOBAL_USER_NAME;
import static com.cx.plugin.utils.CxParam.HIGH_THRESHOLD;
import static com.cx.plugin.utils.CxParam.INTERVAL_BEGINS;
import static com.cx.plugin.utils.CxParam.INTERVAL_ENDS;
import static com.cx.plugin.utils.CxParam.IS_INCREMENTAL;
import static com.cx.plugin.utils.CxParam.IS_INTERVALS;
import static com.cx.plugin.utils.CxParam.IS_SYNCHRONOUS;
import static com.cx.plugin.utils.CxParam.LOW_THRESHOLD;
import static com.cx.plugin.utils.CxParam.MEDIUM_THRESHOLD;
import static com.cx.plugin.utils.CxParam.OPTION_TRUE;
import static com.cx.plugin.utils.CxParam.OSA_ARCHIVE_INCLUDE_PATTERNS;
import static com.cx.plugin.utils.CxParam.DEPENDENCY_SCAN_FILTER_PATTERNS;
import static com.cx.plugin.utils.CxParam.DEPENDENCY_SCAN_FOLDER_EXCLUDE;
import static com.cx.plugin.utils.CxParam.OSA_HIGH_THRESHOLD;
import static com.cx.plugin.utils.CxParam.OSA_INSTALL_BEFORE_SCAN;
import static com.cx.plugin.utils.CxParam.OSA_LOW_THRESHOLD;
import static com.cx.plugin.utils.CxParam.OSA_MEDIUM_THRESHOLD;
import static com.cx.plugin.utils.CxParam.OSA_THRESHOLDS_ENABLED;
import static com.cx.plugin.utils.CxParam.PASSWORD;
import static com.cx.plugin.utils.CxParam.POLICY_VIOLATION_ENABLED;
import static com.cx.plugin.utils.CxParam.PRESET_ID;
import static com.cx.plugin.utils.CxParam.PRESET_NAME;
import static com.cx.plugin.utils.CxParam.PROJECT_NAME;
import static com.cx.plugin.utils.CxParam.SCAN_CONTROL_SECTION;
import static com.cx.plugin.utils.CxParam.SCAN_TIMEOUT_IN_MINUTES;
import static com.cx.plugin.utils.CxParam.SERVER_CREDENTIALS_SECTION;
import static com.cx.plugin.utils.CxParam.SERVER_URL;
import static com.cx.plugin.utils.CxParam.TEAM_PATH_ID;
import static com.cx.plugin.utils.CxParam.TEAM_PATH_NAME;
import static com.cx.plugin.utils.CxParam.THRESHOLDS_ENABLED;
import static com.cx.plugin.utils.CxParam.USER_NAME;
import static com.cx.plugin.utils.CxPluginUtils.decrypt;
import static com.cx.plugin.utils.CxPluginUtils.resolveInt;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.spring.container.ContainerManager;
import com.cx.restclient.ast.dto.sca.AstScaConfig;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.ProxyConfig;
import com.cx.restclient.dto.ScannerType;

/**
 * Created by Galn on 25/10/2017.
 */
public class CxConfigHelper {

    private CxScanConfig scanConfig;
    private AdministrationConfiguration adminConfig;
    private boolean isIntervals;
    private String intervalBegins;
    private String intervalEnds;
    private CxLoggerAdapter log;

    public static final String HTTP_HOST = System.getProperty("http.proxyHost");
    public static final String HTTP_PORT = System.getProperty("http.proxyPort");
    public static final String HTTP_USERNAME = System.getProperty("http.proxyUser");
    public static final String HTTP_PASSWORD = System.getProperty("http.proxyPassword");
    
    public static final String NON_PROXY_HOSTS = System.getProperty("http.nonProxyHosts");

    public static final String HTTPS_HOST = System.getProperty("https.proxyHost");
    public static final String HTTPS_PORT = System.getProperty("https.proxyPort");
    public static final String HTTPS_USERNAME = System.getProperty("https.proxyUser");
    public static final String HTTPS_PASSWORD = System.getProperty("https.proxyPassword");

    public CxConfigHelper(CxLoggerAdapter log) {
        this.log = log;
    }

    public CxScanConfig resolveConfigurationMap(ConfigurationMap configMap, File workDir, TaskContext taskContext) throws TaskException {
        log.info("Resolving Cx configuration");

        Object a = ContainerManager.getComponent("administrationConfigurationAccessor");
        try {
            Method getAdminConfig = a.getClass().getDeclaredMethod("getAdministrationConfiguration");
            adminConfig = (AdministrationConfiguration) getAdminConfig.invoke(a);
        } catch (Exception e) {
            throw new TaskException("Failed to resolve global configuration", e);
        }

        scanConfig = new CxScanConfig();
		
        // adding proxy details
		boolean isProxy = resolveBool(configMap, ENABLE_PROXY);
		scanConfig.setProxy(isProxy);
		if (isProxy) {
			ProxyConfig proxyConfig = HttpHelper.getProxyConfig();
			if (proxyConfig != null) {
				scanConfig.setProxyConfig(proxyConfig);
			}else {
				ProxyConfig proxy = new ProxyConfig();
				scanConfig.setProxyConfig(proxy);
			}
		}
	
        String originUrl = getCxOriginUrl(adminConfig, taskContext);
        scanConfig.setCxOriginUrl(originUrl);
        String cxOrigin = getOrigin(adminConfig, taskContext);
        scanConfig.setCxOrigin(cxOrigin);
        log.info("CxOrigin : "+ cxOrigin);
        log.info("CxOrigin URL : "+ originUrl);
        
        scanConfig.setSourceDir(workDir.getAbsolutePath());
        scanConfig.setReportsDir(workDir);
        
        scanConfig.setSastEnabled(true);
        scanConfig.setDisableCertificateValidation(true);
        if (CUSTOM_CONFIGURATION_SERVER.equals(configMap.get(SERVER_CREDENTIALS_SECTION))) {
            scanConfig.setUrl(configMap.get(SERVER_URL));
            scanConfig.setUsername(configMap.get(USER_NAME));
            scanConfig.setPassword(decrypt(configMap.get(PASSWORD)));
        } else {
            scanConfig.setUrl(getAdminConfig(GLOBAL_SERVER_URL));
            scanConfig.setUsername(getAdminConfig(GLOBAL_USER_NAME));
            scanConfig.setPassword(decrypt(getAdminConfig(GLOBAL_PWD)));
        }

        scanConfig.setProjectName(configMap.get(PROJECT_NAME).trim());

		String presetIdStr = configMap.get(PRESET_ID);
		int presetId = parseInt(presetIdStr, log, "Invalid presetId: [%s]. Using default preset.", 0);

		String teamName = configMap.get(TEAM_PATH_NAME);

		if (StringUtils.isEmpty(teamName)) {
			throw new TaskException("Invalid team path");
		}

		scanConfig.setPresetId(presetId);
        scanConfig.setPresetName(StringUtils.defaultString(configMap.get(PRESET_NAME)));
        scanConfig.setTeamId(StringUtils.defaultString(configMap.get(TEAM_PATH_ID)));
        scanConfig.setTeamPath(teamName);

        if (CUSTOM_CONFIGURATION_CXSAST.equals(configMap.get(CXSAST_SECTION))) {
            scanConfig.setSastFolderExclusions(configMap.get(FOLDER_EXCLUSION));
            scanConfig.setSastFilterPattern(configMap.get(FILTER_PATTERN));
            scanConfig.setSastScanTimeoutInMinutes(resolveInt(configMap.get(SCAN_TIMEOUT_IN_MINUTES), log));

        } else {
            scanConfig.setSastFolderExclusions(getAdminConfig(GLOBAL_FOLDER_EXCLUSION));
            scanConfig.setSastFilterPattern(getAdminConfig(GLOBAL_FILTER_PATTERN));
            scanConfig.setSastScanTimeoutInMinutes(resolveInt(getAdminConfig(GLOBAL_SCAN_TIMEOUT_IN_MINUTES), log));
        }

        scanConfig.setScanComment(configMap.get(COMMENT));
        scanConfig.setIncremental(resolveBool(configMap, IS_INCREMENTAL));

        if (scanConfig.getIncremental()) {
            isIntervals = resolveBool(configMap, IS_INTERVALS);
            if (isIntervals) {
                intervalBegins = configMap.get(INTERVAL_BEGINS);
                intervalEnds = configMap.get(INTERVAL_ENDS);
                scanConfig = resolveIntervalFullScan(scanConfig);
            }
        }
        scanConfig.setGeneratePDFReport(resolveBool(configMap, GENERATE_PDF_REPORT));
        //add AST_SCA or OSA based on what user has selected
        ScannerType scannerType = null;
        if(resolveBool(configMap, ENABLE_DEPENDENCY_SCAN)) {
        	scanConfig.setOsaFilterPattern(configMap.get(DEPENDENCY_SCAN_FILTER_PATTERNS));
        	scanConfig.setOsaFolderExclusions(configMap.get(DEPENDENCY_SCAN_FOLDER_EXCLUDE));
        	if(configMap.get(DEPENDENCY_SCAN_TYPE).equalsIgnoreCase(ScannerType.AST_SCA.toString())) {        		
        		scannerType = ScannerType.AST_SCA;
        		scanConfig.setAstScaConfig(getScaConfig(configMap));
        	}else {
        		scannerType = ScannerType.OSA;
                scanConfig.setOsaArchiveIncludePatterns(configMap.get(OSA_ARCHIVE_INCLUDE_PATTERNS));
                scanConfig.setOsaRunInstall(resolveBool(configMap, OSA_INSTALL_BEFORE_SCAN));
        	}
        	if (scannerType != null) {
                scanConfig.addScannerType(scannerType);
            }
        }
        scanConfig.setDisableCertificateValidation(true);

        if (CUSTOM_CONFIGURATION_CONTROL.equals(configMap.get(SCAN_CONTROL_SECTION))) {
            scanConfig.setSynchronous(resolveBool(configMap, IS_SYNCHRONOUS));
            scanConfig.setEnablePolicyViolations(resolveBool(configMap, POLICY_VIOLATION_ENABLED));
            scanConfig.setSastThresholdsEnabled(resolveBool(configMap, THRESHOLDS_ENABLED));
            scanConfig.setSastHighThreshold(resolveInt(configMap.get(HIGH_THRESHOLD), log));
            scanConfig.setSastMediumThreshold(resolveInt(configMap.get(MEDIUM_THRESHOLD), log));
            scanConfig.setSastLowThreshold(resolveInt(configMap.get(LOW_THRESHOLD), log));
            scanConfig.setOsaThresholdsEnabled(resolveBool(configMap, OSA_THRESHOLDS_ENABLED));
            scanConfig.setOsaHighThreshold(resolveInt(configMap.get(OSA_HIGH_THRESHOLD), log));
            scanConfig.setOsaMediumThreshold(resolveInt(configMap.get(OSA_MEDIUM_THRESHOLD), log));
            scanConfig.setOsaLowThreshold(resolveInt(configMap.get(OSA_LOW_THRESHOLD), log));
        } else {
            scanConfig.setSynchronous(resolveGlobalBool(GLOBAL_IS_SYNCHRONOUS));
            scanConfig.setEnablePolicyViolations(resolveGlobalBool(GLOBAL_POLICY_VIOLATION_ENABLED));
            scanConfig.setSastThresholdsEnabled(resolveGlobalBool(GLOBAL_THRESHOLDS_ENABLED));
            scanConfig.setSastHighThreshold(resolveInt(getAdminConfig(GLOBAL_HIGH_THRESHOLD), log));
            scanConfig.setSastMediumThreshold(resolveInt(getAdminConfig(GLOBAL_MEDIUM_THRESHOLD), log));
            scanConfig.setSastLowThreshold(resolveInt(getAdminConfig(GLOBAL_LOW_THRESHOLD), log));
            scanConfig.setOsaThresholdsEnabled(resolveGlobalBool(GLOBAL_OSA_THRESHOLDS_ENABLED));
            scanConfig.setOsaHighThreshold(resolveInt(getAdminConfig(GLOBAL_OSA_HIGH_THRESHOLD), log));
            scanConfig.setOsaMediumThreshold(resolveInt(getAdminConfig(GLOBAL_OSA_MEDIUM_THRESHOLD), log));
            scanConfig.setOsaLowThreshold(resolveInt(getAdminConfig(GLOBAL_OSA_LOW_THRESHOLD), log));
        }
        scanConfig.setDenyProject(resolveGlobalBool(GLOBAL_DENY_PROJECT));
        scanConfig.setHideResults(resolveGlobalBool(GLOBAL_HIDE_RESULTS));
        scanConfig.setDisableCertificateValidation(true);
        return scanConfig;
    }
    
    private int parseInt(String number, CxLoggerAdapter log, String templateMessage, int defaultVal) {
        int ret = defaultVal;
        try {
            ret = Integer.parseInt(number);
        } catch (Exception e) {
            log.warn(String.format(templateMessage, number));
        }
        return ret;
    }
    private String getOrigin(AdministrationConfiguration adminConfig, TaskContext taskContext) {
        String origin = "";
        try {
            BuildContext buildContext = taskContext.getBuildContext();
            String planName =  buildContext.getPlanName();
            planName = URLDecoder.decode(planName, "UTF-8");
            planName = planName.replaceAll("[^.a-zA-Z0-9\\s]", "");
            String bambooURL = adminConfig.getBaseUrl();
            bambooURL = bambooURL.substring((bambooURL.lastIndexOf("://")) + 3);
            String hostName = "";
            if(bambooURL.indexOf(":")!=-1) {
                hostName = bambooURL.substring(0, bambooURL.lastIndexOf(":"));
            } else {
                hostName = bambooURL;
            }
            origin = "Bamboo " + hostName + " " + planName;
            // 50 is the maximum number of characters allowed by SAST server
            if(origin!=null && !origin.isEmpty()){
            if(origin.length()>50){
            	origin=origin.substring(0,50);
            	}
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to get BAMBOO URL of the PLAN: " + e.getMessage());
        }
        return origin;
    }


    private String getCxOriginUrl(AdministrationConfiguration adminConfig, TaskContext taskContext) {
        String baseURL = adminConfig.getBaseUrl();
        BuildContext buildContext = taskContext.getBuildContext();
        String planKey = buildContext.getPlanResultKey().getPlanKey().getKey();
        String originUrl = baseURL+"/browse/"+planKey;
        return originUrl;
    }
    
    private boolean resolveBool(ConfigurationMap configMap, String value) {
        return OPTION_TRUE.equals(configMap.get(value));
    }

    private boolean resolveGlobalBool(String value) {
        return OPTION_TRUE.equals(getAdminConfig(value));
    }


    private CxScanConfig resolveIntervalFullScan(CxScanConfig scanConfig) {

        try {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");

            final Calendar calendarBeginsHourMinute = Calendar.getInstance();
            calendarBeginsHourMinute.setTime(dateFormat.parse(intervalBegins));
            final Calendar calendarBegins = Calendar.getInstance();
            calendarBegins.set(Calendar.HOUR_OF_DAY, calendarBeginsHourMinute.get(Calendar.HOUR_OF_DAY));
            calendarBegins.set(Calendar.MINUTE, calendarBeginsHourMinute.get(Calendar.MINUTE));
            calendarBegins.set(Calendar.SECOND, 0);
            Date dateBegins = calendarBegins.getTime();

            final Calendar calendarEndsHourMinute = Calendar.getInstance();
            calendarEndsHourMinute.setTime(dateFormat.parse(intervalEnds));
            final Calendar calendarEnds = Calendar.getInstance();
            calendarEnds.set(Calendar.HOUR_OF_DAY, calendarEndsHourMinute.get(Calendar.HOUR_OF_DAY));
            calendarEnds.set(Calendar.MINUTE, calendarEndsHourMinute.get(Calendar.MINUTE));
            calendarEnds.set(Calendar.SECOND, 0);
            Date dateEnds = calendarEnds.getTime();

            final Date dateNow = Calendar.getInstance().getTime();

            if (dateBegins.after(dateEnds)) {
                if (dateBegins.after(dateNow)) {
                    calendarBegins.add(Calendar.DATE, -1);
                    dateBegins = calendarBegins.getTime();
                } else {
                    calendarEnds.add(Calendar.DATE, 1);
                    dateEnds = calendarEnds.getTime();
                }
            }

            if (dateNow.after(dateBegins) && dateNow.before(dateEnds)) {
                scanConfig.setIncremental(false);
            }

        } catch (final ParseException e) {
            log.error("Full scan interval parse exception");
        }
        return scanConfig;
    }


    private String getAdminConfig(String key) {
        return StringUtils.defaultString(adminConfig.getSystemProperty(key));
    }

    private AstScaConfig getScaConfig(ConfigurationMap configMap) {
		AstScaConfig result = new AstScaConfig();
		result.setApiUrl(configMap.get(CXSCA_API_URL));
		result.setAccessControlUrl(configMap.get(CXSCA_ACCESS_CONTROL_URL));
		result.setWebAppUrl(configMap.get(CXSCA_WEBAPP_URL));
		result.setTenant(configMap.get(CXSCA_ACCOUNT_NAME));

		result.setUsername(configMap.get(CXSCA_USERNAME));
		result.setPassword(configMap.get(CXSCA_PWD));

		return result;
    }
    
    public String getPluginVersion() {
        String version = "";
        try {
            Properties properties = new Properties();
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("english.properties");
            if (is != null) {
                properties.load(is);
                version = properties.getProperty("version");
            }
        } catch (Exception e) {

        }
        return version;
    }

    public boolean isIntervals() {
        return isIntervals;
    }

    public void setIntervals(boolean intervals) {
        isIntervals = intervals;
    }

    public String getIntervalBegins() {
        return intervalBegins;
    }

    public void setIntervalBegins(String intervalBegins) {
        this.intervalBegins = intervalBegins;
    }

    public String getIntervalEnds() {
        return intervalEnds;
    }

    public void setIntervalEnds(String intervalEnds) {
        this.intervalEnds = intervalEnds;
    }
}
