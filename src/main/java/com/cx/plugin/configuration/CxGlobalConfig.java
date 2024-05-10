package com.cx.plugin.configuration;

import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfigurationPersister;
import com.atlassian.bamboo.configuration.GlobalAdminAction;
import com.atlassian.spring.container.ContainerManager;
import com.cx.plugin.utils.CxParam;
import com.google.common.collect.ImmutableMap;
import org.codehaus.plexus.util.StringUtils;

import javax.annotation.Nonnull;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static com.cx.plugin.utils.CxParam.*;
import static com.cx.plugin.utils.CxPluginUtils.encrypt;


/**
 * CxGlobalConfig is a GlobalAdminAction that populates cxGlobalConfig.ftl and handles the incoming data from it
 * <p>
 * It is being activated through the Bamboo framework,
 * as configured under checkmarx-default-config-xwork in atlassian-plugin.xml
 */

public class CxGlobalConfig extends GlobalAdminAction {
    private String globalServerUrl;
    private String globalUsername;
    private String globalPss;

    private String globalcxScaUsername;
    private String globalcxScaPss;
    
    private String globalFilterPatterns = DEFAULT_FILTER_PATTERNS;
    private String globalFolderExclusions;
    private String globalIsSynchronous;
    private String globalEnableProxy;
    private String globalEnablePolicyViolations;
    private String globalEnablePolicyViolationsSCA;
    private String globalScanTimeoutInMinutes;
    private String globalThresholdsEnabled;
    private String globalHighThreshold;
    private String globalMediumThreshold;
    private String globalLowThreshold;
    private String globalCriticalThreshold;
    private String globalOsaThresholdsEnabled;
    private String globalOsaHighThreshold;
    private String globalOsaMediumThreshold;
    private String globalOsaLowThreshold;
    private String globalDenyProject;
    private String globalHideResults;
    private String globalEnableCriticalSeverity = OPTION_FALSE;
    
    private String globalEnableDependencyScan = OPTION_FALSE;

	private String globalDependencyScanType;
	
    private String globalDependencyScanFilterPatterns="";
    
    private boolean criticalSupported = false;

	private String globalDependencyScanfolderExclusions;
	private String globalOsaArchiveIncludePatterns = DEFAULT_OSA_ARCHIVE_INCLUDE_PATTERNS;
    private String globalOsaInstallBeforeScan;
    private String globalcxScaAPIUrl = DEFAULT_CXSCA_API_URL;
    private String globalcxScaAccessControlServerUrl = DEFAULT_CXSCA_ACCESS_CONTROL_URL;
    private String globalcxScaWebAppUrl = DEFAULT_CXSCA_WEB_APP_URL;
    private String globalcxScaAccountName = "";

    private String globalCxScaResolverEnabled;
    private String globalCxScaResolverPath;
    private String globalCxScaResolverAddParam;
  
	private Map<String, String> globalDependencyScanTypeValues = ImmutableMap.of("OSA", "Use CxOSA dependency scanner", "AST_SCA", "Use CxSCA dependency scanner");

    @Override
    public String execute() {
        final AdministrationConfiguration adminConfig = (AdministrationConfiguration) ContainerManager.getComponent("administrationConfiguration");

        globalServerUrl = adminConfig.getSystemProperty(GLOBAL_SERVER_URL);
        globalUsername = adminConfig.getSystemProperty(GLOBAL_USER_NAME);
        globalPss = adminConfig.getSystemProperty(GLOBAL_PWD);
        if (adminConfig.getSystemProperty(GLOBAL_ENABLE_DEPENDENCY_SCAN) != null
				&& !(adminConfig.getSystemProperty(GLOBAL_ENABLE_DEPENDENCY_SCAN)).isEmpty()) {
        globalEnableDependencyScan = adminConfig.getSystemProperty(GLOBAL_ENABLE_DEPENDENCY_SCAN);
        }
        globalDependencyScanType = adminConfig.getSystemProperty(GLOBAL_DEPENDENCY_SCAN_TYPE);
        globalDependencyScanFilterPatterns = adminConfig.getSystemProperty(GLOBAL_DEPENDENCY_SCAN_FILTER_PATTERNS);
        globalDependencyScanfolderExclusions = adminConfig.getSystemProperty(GLOBAL_DEPENDENCY_SCAN_FOLDER_EXCLUDE);
        if (adminConfig.getSystemProperty(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS) != null
				&& !(adminConfig.getSystemProperty(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS)).isEmpty()) {
        globalOsaArchiveIncludePatterns = adminConfig.getSystemProperty(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS);
        }
        globalOsaInstallBeforeScan = adminConfig.getSystemProperty(GLOBAL_OSA_INSTALL_BEFORE_SCAN);
		if (adminConfig.getSystemProperty(GLOBAL_CXSCA_API_URL) != null
				&& !(adminConfig.getSystemProperty(GLOBAL_CXSCA_API_URL)).isEmpty()) {
			globalcxScaAPIUrl = adminConfig.getSystemProperty(GLOBAL_CXSCA_API_URL);
		}
		if (adminConfig.getSystemProperty(GLOBAL_CXSCA_ACCESS_CONTROL_URL) != null
				&& !(adminConfig.getSystemProperty(GLOBAL_CXSCA_ACCESS_CONTROL_URL)).isEmpty()) {
        globalcxScaAccessControlServerUrl = adminConfig.getSystemProperty(GLOBAL_CXSCA_ACCESS_CONTROL_URL);
		}
		if (adminConfig.getSystemProperty(GLOBAL_CXSCA_WEBAPP_URL) != null
				&& !(adminConfig.getSystemProperty(GLOBAL_CXSCA_WEBAPP_URL)).isEmpty()) {
        globalcxScaWebAppUrl = adminConfig.getSystemProperty(GLOBAL_CXSCA_WEBAPP_URL);
		}
        globalcxScaAccountName = adminConfig.getSystemProperty(GLOBAL_CXSCA_ACCOUNT_NAME);
        						
        globalcxScaUsername = adminConfig.getSystemProperty(GLOBAL_CXSCA_USERNAME);
        globalcxScaPss = adminConfig.getSystemProperty(GLOBAL_CXSCA_PWD);

        globalCxScaResolverEnabled = adminConfig.getSystemProperty(CXSCA_RESOLVER_ENABLED_GLOBAL);
        globalCxScaResolverPath = adminConfig.getSystemProperty(CXSCA_RESOLVER_PATH_GLOBAL);
        globalCxScaResolverAddParam = adminConfig.getSystemProperty(CXSCA_RESOLVER_ADD_PARAM_GLOBAL);

        globalFolderExclusions = adminConfig.getSystemProperty(GLOBAL_FOLDER_EXCLUSION);
        String filterProperty = adminConfig.getSystemProperty(GLOBAL_FILTER_PATTERN);
        if (filterProperty != null) {
            globalFilterPatterns = filterProperty;
        }

        globalEnableCriticalSeverity = adminConfig.getSystemProperty(GLOBAL_ENABLE_CRITICAL_SEVERITY);
        globalScanTimeoutInMinutes = adminConfig.getSystemProperty(GLOBAL_SCAN_TIMEOUT_IN_MINUTES);
        globalIsSynchronous = adminConfig.getSystemProperty(GLOBAL_IS_SYNCHRONOUS);
        globalEnableProxy=adminConfig.getSystemProperty(GLOBAL_ENABLE_PROXY);
        globalHideResults = adminConfig.getSystemProperty(GLOBAL_HIDE_RESULTS);
        globalEnablePolicyViolations = adminConfig.getSystemProperty(GLOBAL_POLICY_VIOLATION_ENABLED);
        globalEnablePolicyViolationsSCA = adminConfig.getSystemProperty(GLOBAL_POLICY_VIOLATION_ENABLED_SCA);
        globalThresholdsEnabled = adminConfig.getSystemProperty(GLOBAL_THRESHOLDS_ENABLED);
        globalHighThreshold = adminConfig.getSystemProperty(GLOBAL_HIGH_THRESHOLD);
        globalMediumThreshold = adminConfig.getSystemProperty(GLOBAL_MEDIUM_THRESHOLD);
        globalLowThreshold = adminConfig.getSystemProperty(GLOBAL_LOW_THRESHOLD);
        globalCriticalThreshold = adminConfig.getSystemProperty(GLOBAL_CRITICAL_THRESHOLD);
        globalOsaThresholdsEnabled = adminConfig.getSystemProperty(GLOBAL_OSA_THRESHOLDS_ENABLED);
        globalOsaHighThreshold = adminConfig.getSystemProperty(GLOBAL_OSA_HIGH_THRESHOLD);
        globalOsaMediumThreshold = adminConfig.getSystemProperty(GLOBAL_OSA_MEDIUM_THRESHOLD);
        globalOsaLowThreshold = adminConfig.getSystemProperty(GLOBAL_OSA_LOW_THRESHOLD);
        globalDenyProject = adminConfig.getSystemProperty(GLOBAL_DENY_PROJECT);
        globalHideResults = adminConfig.getSystemProperty(GLOBAL_HIDE_RESULTS);
                

        return INPUT;
    }

    public String save() {
        boolean error = isURLInvalid(globalServerUrl, GLOBAL_SERVER_URL);
        globalEnableCriticalSeverity = OPTION_TRUE;
        float version = (float) 9.7;
        
        error |= isScanTimeoutInvalid();
        if ("true".equals(globalEnableDependencyScan)) {
        	if("AST_SCA".equals(globalDependencyScanType)){
        		error |= isURLInvalid(globalcxScaAPIUrl, GLOBAL_CXSCA_API_URL);
        		error |= isURLInvalid(globalcxScaAccessControlServerUrl, GLOBAL_CXSCA_ACCESS_CONTROL_URL);
        		error |= isURLInvalid(globalcxScaWebAppUrl, GLOBAL_CXSCA_WEBAPP_URL);
        	}
        }
        if ("true".equals(globalIsSynchronous)) {
            if ("true".equals(globalThresholdsEnabled)) {
                error |= isNegative(getGlobalHighThreshold(), GLOBAL_HIGH_THRESHOLD);
                error |= isNegative(getGlobalMediumThreshold(), GLOBAL_MEDIUM_THRESHOLD);
                error |= isNegative(getGlobalLowThreshold(), GLOBAL_LOW_THRESHOLD);
                if(version >= 9.7 && OPTION_FALSE.equalsIgnoreCase(globalEnableCriticalSeverity)) {
                	globalEnableCriticalSeverity = OPTION_TRUE;
                	error |= isCriticalSupported("9.7",GLOBAL_CRITICAL_THRESHOLD);
                	
                }else {
                error |= isNegative(getGlobalCriticalThreshold(), GLOBAL_CRITICAL_THRESHOLD);
                }
            }
            if ("true".equals(globalOsaThresholdsEnabled)) {
                error |= isNegative(getGlobalOsaHighThreshold(), GLOBAL_OSA_HIGH_THRESHOLD);
                error |= isNegative(getGlobalOsaMediumThreshold(), GLOBAL_OSA_MEDIUM_THRESHOLD);
                error |= isNegative(getGlobalOsaLowThreshold(), GLOBAL_OSA_LOW_THRESHOLD);
            }
        }
        if (error) {
            return ERROR;
        }
        final AdministrationConfiguration adminConfig = (AdministrationConfiguration) ContainerManager.getComponent(ADMINISTRATION_CONFIGURATION);
        adminConfig.setSystemProperty(GLOBAL_SERVER_URL, globalServerUrl);
        adminConfig.setSystemProperty(GLOBAL_USER_NAME, globalUsername);
        adminConfig.setSystemProperty(GLOBAL_PWD, encrypt(globalPss));
        
        adminConfig.setSystemProperty(GLOBAL_ENABLE_DEPENDENCY_SCAN, globalEnableDependencyScan);
        adminConfig.setSystemProperty(GLOBAL_DEPENDENCY_SCAN_TYPE, globalDependencyScanType);
        adminConfig.setSystemProperty(GLOBAL_DEPENDENCY_SCAN_FILTER_PATTERNS, globalDependencyScanFilterPatterns);
        adminConfig.setSystemProperty(GLOBAL_DEPENDENCY_SCAN_FOLDER_EXCLUDE, globalDependencyScanfolderExclusions);
        
        adminConfig.setSystemProperty(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS, globalOsaArchiveIncludePatterns);
        adminConfig.setSystemProperty(GLOBAL_OSA_INSTALL_BEFORE_SCAN, globalOsaInstallBeforeScan);
        adminConfig.setSystemProperty(GLOBAL_CXSCA_API_URL, globalcxScaAPIUrl);
        adminConfig.setSystemProperty(GLOBAL_CXSCA_ACCESS_CONTROL_URL, globalcxScaAccessControlServerUrl);
        adminConfig.setSystemProperty(GLOBAL_CXSCA_WEBAPP_URL, globalcxScaWebAppUrl);
        adminConfig.setSystemProperty(GLOBAL_CXSCA_ACCOUNT_NAME, globalcxScaAccountName);
        
        adminConfig.setSystemProperty(GLOBAL_CXSCA_USERNAME, globalcxScaUsername);
        adminConfig.setSystemProperty(GLOBAL_CXSCA_PWD, encrypt(globalcxScaPss));

        adminConfig.setSystemProperty(CXSCA_RESOLVER_ENABLED_GLOBAL, globalCxScaResolverEnabled);
        adminConfig.setSystemProperty(CXSCA_RESOLVER_PATH_GLOBAL, globalCxScaResolverPath);
        adminConfig.setSystemProperty(CXSCA_RESOLVER_ADD_PARAM_GLOBAL, globalCxScaResolverAddParam);

        adminConfig.setSystemProperty(GLOBAL_FOLDER_EXCLUSION, globalFolderExclusions);
        adminConfig.setSystemProperty(GLOBAL_FILTER_PATTERN, globalFilterPatterns);
        adminConfig.setSystemProperty(GLOBAL_SCAN_TIMEOUT_IN_MINUTES, globalScanTimeoutInMinutes);

        adminConfig.setSystemProperty(GLOBAL_IS_SYNCHRONOUS, globalIsSynchronous);
        adminConfig.setSystemProperty(GLOBAL_ENABLE_PROXY, globalEnableProxy);
        if (globalIsSynchronous == null) {
            globalThresholdsEnabled = null;
            globalOsaThresholdsEnabled = null;
            globalEnablePolicyViolations = null;
            globalEnablePolicyViolationsSCA = null;
        }
        adminConfig.setSystemProperty(GLOBAL_POLICY_VIOLATION_ENABLED, globalEnablePolicyViolations);
        adminConfig.setSystemProperty(GLOBAL_POLICY_VIOLATION_ENABLED_SCA, globalEnablePolicyViolationsSCA);
        adminConfig.setSystemProperty(GLOBAL_THRESHOLDS_ENABLED, globalThresholdsEnabled);
        adminConfig.setSystemProperty(GLOBAL_HIGH_THRESHOLD, globalHighThreshold);
        adminConfig.setSystemProperty(GLOBAL_MEDIUM_THRESHOLD, globalMediumThreshold);
        adminConfig.setSystemProperty(GLOBAL_LOW_THRESHOLD, globalLowThreshold);
        adminConfig.setSystemProperty(GLOBAL_CRITICAL_THRESHOLD, globalCriticalThreshold);
        adminConfig.setSystemProperty(GLOBAL_OSA_THRESHOLDS_ENABLED, globalOsaThresholdsEnabled);
        adminConfig.setSystemProperty(GLOBAL_OSA_HIGH_THRESHOLD, globalOsaHighThreshold);
        adminConfig.setSystemProperty(GLOBAL_OSA_MEDIUM_THRESHOLD, globalOsaMediumThreshold);
        adminConfig.setSystemProperty(GLOBAL_OSA_LOW_THRESHOLD, globalOsaLowThreshold);
        adminConfig.setSystemProperty(GLOBAL_DENY_PROJECT, globalDenyProject);
        adminConfig.setSystemProperty(GLOBAL_HIDE_RESULTS, globalHideResults);
        adminConfig.setSystemProperty(GLOBAL_ENABLE_CRITICAL_SEVERITY, globalEnableCriticalSeverity);

        ((AdministrationConfigurationPersister) ContainerManager.getComponent("administrationConfigurationPersister")).saveAdministrationConfiguration(adminConfig);

        addActionMessage(getText("cxDefaultConfigSuccess.label"));
        return SUCCESS;
    }
    
    private boolean isCriticalSupported(@Nonnull String value, @Nonnull String key) {
        boolean ret = false;
        if (!StringUtils.isEmpty(value)) {
            try {
                double num = Double.parseDouble(value);
                if (num >= 9.7) {
                    addFieldError(key, getText(key + ".supported"));
                    ret = true;
                }

            } catch (Exception e) {
                addFieldError(key, getText(key + ".supported"));
                ret = true;
            }
        }
        return ret;
    }

    private boolean isURLInvalid(final String value, final String fieldName) {
        boolean ret = false;
        if (!StringUtils.isEmpty(value)) {
            try {
                URL url = new URL(value);
                if (url.getPath().length() > 0) {
                    addFieldError(fieldName, ("URL must not contain path"));
                    ret = true;
                }
            } catch (MalformedURLException e) {
                addFieldError(fieldName, getText(fieldName + "." + ERROR + ".malformed"));
                ret = true;
            }
        }
        return ret;
    }

    private boolean isScanTimeoutInvalid() {
        boolean ret = false;
        String scanTimeout = getGlobalScanTimeoutInMinutes();
        if (!StringUtils.isEmpty(scanTimeout)) {
            try {
                int num = Integer.parseInt(scanTimeout);
                if (num <= 0) {
                    addFieldError(CxParam.GLOBAL_SCAN_TIMEOUT_IN_MINUTES, getText(CxParam.GLOBAL_SCAN_TIMEOUT_IN_MINUTES + ".notPositive"));
                    ret = true;
                }

            } catch (Exception e) {
                addFieldError(CxParam.GLOBAL_SCAN_TIMEOUT_IN_MINUTES, getText(CxParam.GLOBAL_SCAN_TIMEOUT_IN_MINUTES + ".notPositive"));
                ret = true;
            }
        }
        return ret;

    }

    private boolean isNegative(@Nonnull String value, @Nonnull String key) {
        boolean ret = false;
        if (!StringUtils.isEmpty(value)) {
            try {
                int num = Integer.parseInt(value);
                if (num < 0) {
                    addFieldError(key, getText(key + ".notPositive"));
                    ret = true;
                }

            } catch (Exception e) {
                addFieldError(key, getText(key + ".notPositive"));
                ret = true;
            }
        }
        return ret;
    }


    /*************** Setters & Getters  ****************************/
    public String getGlobalServerUrl() {
        return globalServerUrl;
    }

    public void setGlobalServerUrl(String globalServerUrl) {
        this.globalServerUrl = globalServerUrl;
    }

    public String getGlobalUsername() {
        return globalUsername;
    }

    public void setGlobalUsername(String globalUsername) {
        this.globalUsername = globalUsername.trim();
    }

    public String getGlobalPss() {
        return globalPss;
    }

    public void setGlobalPss(String globalPss) {
        this.globalPss = globalPss;
    }

    public String getGlobalcxScaUsername() {
		return globalcxScaUsername;
	}

	public void setGlobalcxScaUsername(String globalcxScaUsername) {
		this.globalcxScaUsername = globalcxScaUsername;
	}

	public String getGlobalcxScaPss() {
		return globalcxScaPss;
	}

	public void setGlobalcxScaPss(String globalcxScaPss) {
		this.globalcxScaPss = globalcxScaPss;
	}

	public String getGlobalFilterPatterns() {
        return globalFilterPatterns;
    }

    public void setGlobalFilterPatterns(String globalFilterPatterns) {
        this.globalFilterPatterns = globalFilterPatterns;
    }

    public String getGlobalFolderExclusions() {
        return globalFolderExclusions;
    }

    public void setGlobalFolderExclusions(String globalFolderExclusions) {
        this.globalFolderExclusions = globalFolderExclusions;
    }

    public String getGlobalIsSynchronous() {
        return globalIsSynchronous;
    }
    

    public String getGlobalEnableProxy() {
		return globalEnableProxy;
	}

	public void setGlobalEnableProxy(String globalEnableProxy) {
		this.globalEnableProxy = globalEnableProxy;
	}

	public String getGlobalEnablePolicyViolations() {
        return globalEnablePolicyViolations;
    }

    public void setGlobalEnablePolicyViolations(String globalEnablePolicyViolations) {
        this.globalEnablePolicyViolations = globalEnablePolicyViolations;
    }
    
    public String getGlobalEnablePolicyViolationsSCA() {
        return globalEnablePolicyViolationsSCA;
    }

    public void setGlobalEnablePolicyViolationsSCA(String globalEnablePolicyViolationsSCA) {
        this.globalEnablePolicyViolationsSCA = globalEnablePolicyViolationsSCA;
    }

    public void setGlobalIsSynchronous(String globalIsSynchronous) {
        this.globalIsSynchronous = globalIsSynchronous;
    }

    public String getGlobalScanTimeoutInMinutes() {
        return globalScanTimeoutInMinutes;
    }

    public void setGlobalScanTimeoutInMinutes(String globalScanTimeoutInMinutes) {
        this.globalScanTimeoutInMinutes = globalScanTimeoutInMinutes.trim();
    }

    public String getGlobalThresholdsEnabled() {
        return globalThresholdsEnabled;
    }

    public void setGlobalThresholdsEnabled(String globalThresholdsEnabled) {
        this.globalThresholdsEnabled = globalThresholdsEnabled;
    }

    public String getGlobalHighThreshold() {
        return globalHighThreshold;
    }

    public void setGlobalHighThreshold(String globalHighThreshold) {
        this.globalHighThreshold = globalHighThreshold;
    }

    public String getGlobalMediumThreshold() {
        return globalMediumThreshold;
    }

    public void setGlobalMediumThreshold(String globalMediumThreshold) {
        this.globalMediumThreshold = globalMediumThreshold;
    }

    public String getGlobalLowThreshold() {
        return globalLowThreshold;
    }

    public void setGlobalLowThreshold(String globalLowThreshold) {
        this.globalLowThreshold = globalLowThreshold;
    }
    
    public String getGlobalCriticalThreshold() {
        return globalCriticalThreshold;
    }

    public void setGlobalCriticalThreshold(String globalCriticalThreshold) {
        this.globalCriticalThreshold = globalCriticalThreshold;
    }

    public String getGlobalOsaThresholdsEnabled() {
        return globalOsaThresholdsEnabled;
    }

    public void setGlobalOsaThresholdsEnabled(String globalOsaThresholdsEnabled) {
        this.globalOsaThresholdsEnabled = globalOsaThresholdsEnabled;
    }

    public String getGlobalOsaHighThreshold() {
        return globalOsaHighThreshold;
    }

    public void setGlobalOsaHighThreshold(String globalOsaHighThreshold) {
        this.globalOsaHighThreshold = globalOsaHighThreshold;
    }

    public String getGlobalOsaMediumThreshold() {
        return globalOsaMediumThreshold;
    }

    public void setGlobalOsaMediumThreshold(String globalOsaMediumThreshold) {
        this.globalOsaMediumThreshold = globalOsaMediumThreshold;
    }

    public String getGlobalOsaLowThreshold() {
        return globalOsaLowThreshold;
    }

    public void setGlobalOsaLowThreshold(String globalOsaLowThreshold) {
        this.globalOsaLowThreshold = globalOsaLowThreshold;
    }

    public String getGlobalDenyProject() {
        return globalDenyProject;
    }

    public void setGlobalDenyProject(String globalDenyProject) {
        this.globalDenyProject = globalDenyProject;
    }

    public String getGlobalHideResults() {
        return globalHideResults;
    }

    public void setGlobalHideResults(String globalHideResults) {
        this.globalHideResults = globalHideResults;
    }
    
    public String getGlobalEnableCriticalSeverity() {
		return globalEnableCriticalSeverity;
	}

	public void setGlobalEnableCriticalSeverity(String globalEnableCriticalSeverity) {
		this.globalEnableCriticalSeverity = globalEnableCriticalSeverity;
	}
    

	public String getGlobalEnableDependencyScan() {
		return globalEnableDependencyScan;
	}

	public void setGlobalEnableDependencyScan(String globalEnableDependencyScan) {
		this.globalEnableDependencyScan = globalEnableDependencyScan;
	}

	public String getGlobalDependencyScanType() {
		return globalDependencyScanType;
	}

	public void setGlobalDependencyScanType(String globalDependencyScanType) {
		this.globalDependencyScanType = globalDependencyScanType;
	}

	public String getGlobalDependencyScanFilterPatterns() {
		return globalDependencyScanFilterPatterns;
	}

	public void setGlobalDependencyScanFilterPatterns(String globalDependencyScanFilterPatterns) {
		this.globalDependencyScanFilterPatterns = globalDependencyScanFilterPatterns;
	}

	public String getGlobalDependencyScanfolderExclusions() {
		return globalDependencyScanfolderExclusions;
	}

	public void setGlobalDependencyScanfolderExclusions(String globalDependencyScanfolderExclusions) {
		this.globalDependencyScanfolderExclusions = globalDependencyScanfolderExclusions;
	}

	public String getGlobalOsaArchiveIncludePatterns() {
		return globalOsaArchiveIncludePatterns;
	}

	public void setGlobalOsaArchiveIncludePatterns(String globalOsaArchiveIncludePatterns) {
		this.globalOsaArchiveIncludePatterns = globalOsaArchiveIncludePatterns;
	}

	public String getGlobalOsaInstallBeforeScan() {
		return globalOsaInstallBeforeScan;
	}

	public void setGlobalOsaInstallBeforeScan(String globalOsaInstallBeforeScan) {
		this.globalOsaInstallBeforeScan = globalOsaInstallBeforeScan;
	}

	public String getGlobalcxScaAPIUrl() {
		return globalcxScaAPIUrl;
	}

	public void setGlobalcxScaAPIUrl(String globalcxScaAPIUrl) {
		this.globalcxScaAPIUrl = globalcxScaAPIUrl;
	}

	public String getGlobalcxScaAccessControlServerUrl() {
		return globalcxScaAccessControlServerUrl;
	}

	public void setGlobalcxScaAccessControlServerUrl(String globalcxScaAccessControlServerUrl) {
		this.globalcxScaAccessControlServerUrl = globalcxScaAccessControlServerUrl;
	}

	public String getGlobalcxScaWebAppUrl() {
		return globalcxScaWebAppUrl;
	}

	public void setGlobalcxScaWebAppUrl(String globalcxScaWebAppUrl) {
		this.globalcxScaWebAppUrl = globalcxScaWebAppUrl;
	}

	public String getGlobalcxScaAccountName() {
		return globalcxScaAccountName;
	}

	public void setGlobalcxScaAccountName(String globalcxScaAccountName) {
		this.globalcxScaAccountName = globalcxScaAccountName;
	}

    public String getGlobalCxScaResolverEnabled() { return globalCxScaResolverEnabled; }

    public void setGlobalCxScaResolverEnabled(String globalCxScaResolverEnabled) {
        this.globalCxScaResolverEnabled = globalCxScaResolverEnabled;
    }

    public String getGlobalCxScaResolverPath() { return globalCxScaResolverPath; }

    public void setGlobalCxScaResolverPath(String globalCxScaResolverPath) {
        this.globalCxScaResolverPath = globalCxScaResolverPath;
    }

    public String getGlobalCxScaResolverAddParam() { return globalCxScaResolverAddParam; }

    public void setGlobalCxScaResolverAddParam(String globalCxScaResolverAddParam) {
        this.globalCxScaResolverAddParam = globalCxScaResolverAddParam;
    }

	public Map<String, String> getGlobalDependencyScanTypeValues() {
		return globalDependencyScanTypeValues;
	}

	public void setGlobalDependencyScanTypeValues(Map<String, String> globalDependencyScanTypeValues) {
		this.globalDependencyScanTypeValues = globalDependencyScanTypeValues;
	}

}
