package com.cx.plugin.configuration;

import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfigurationPersister;
import com.atlassian.bamboo.configuration.GlobalAdminAction;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.util.concurrent.NotNull;
import com.cx.plugin.utils.CxParam;
import com.google.common.collect.ImmutableMap;

import org.codehaus.plexus.util.StringUtils;

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
    private String globalScanTimeoutInMinutes;
    private String globalThresholdsEnabled;
    private String globalHighThreshold;
    private String globalMediumThreshold;
    private String globalLowThreshold;
    private String globalOsaThresholdsEnabled;
    private String globalOsaHighThreshold;
    private String globalOsaMediumThreshold;
    private String globalOsaLowThreshold;
    private String globalDenyProject;
    private String globalHideResults;
    
    private String globalEnableDependencyScan = OPTION_FALSE;
	private String globalDependencyScanType;
    private String cxGlobalDependencyScanFilterPatterns="";
    private String cxGlobalDependencyScanfolderExclusions;
    

	private String cxGlobalOsaArchiveIncludePatterns = DEFAULT_OSA_ARCHIVE_INCLUDE_PATTERNS;
    private String cxGlobalOsaInstallBeforeScan;
    private String cxScaGlobalAPIUrl = DEFAULT_CXSCA_API_URL;
    private String cxGlobalAccessControlServerUrl = DEFAULT_CXSCA_ACCESS_CONTROL_URL;
    private String cxScaGlobalWebAppUrl = DEFAULT_CXSCA_WEB_APP_URL;
    private String cxScaGlobalAccountName = "";
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
        cxGlobalDependencyScanFilterPatterns = adminConfig.getSystemProperty(GLOBAL_DEPENDENCY_SCAN_FILTER_PATTERNS);
        cxGlobalDependencyScanfolderExclusions = adminConfig.getSystemProperty(GLOBAL_DEPENDENCY_SCAN_FOLDER_EXCLUDE);
        if (adminConfig.getSystemProperty(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS) != null
				&& !(adminConfig.getSystemProperty(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS)).isEmpty()) {
        cxGlobalOsaArchiveIncludePatterns = adminConfig.getSystemProperty(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS);
        }
        cxGlobalOsaInstallBeforeScan = adminConfig.getSystemProperty(GLOBAL_OSA_INSTALL_BEFORE_SCAN);
		if (adminConfig.getSystemProperty(GLOBAL_CXSCA_API_URL) != null
				&& !(adminConfig.getSystemProperty(GLOBAL_CXSCA_API_URL)).isEmpty()) {
			cxScaGlobalAPIUrl = adminConfig.getSystemProperty(GLOBAL_CXSCA_API_URL);
		}
		if (adminConfig.getSystemProperty(GLOBAL_CXSCA_ACCESS_CONTROL_URL) != null
				&& !(adminConfig.getSystemProperty(GLOBAL_CXSCA_ACCESS_CONTROL_URL)).isEmpty()) {
        cxGlobalAccessControlServerUrl = adminConfig.getSystemProperty(GLOBAL_CXSCA_ACCESS_CONTROL_URL);
		}
		if (adminConfig.getSystemProperty(GLOBAL_CXSCA_WEBAPP_URL) != null
				&& !(adminConfig.getSystemProperty(GLOBAL_CXSCA_WEBAPP_URL)).isEmpty()) {
        cxScaGlobalWebAppUrl = adminConfig.getSystemProperty(GLOBAL_CXSCA_WEBAPP_URL);
		}
        cxScaGlobalAccountName = adminConfig.getSystemProperty(GLOBAL_CXSCA_ACCOUNT_NAME);
        						
        globalcxScaUsername = adminConfig.getSystemProperty(GLOBAL_CXSCA_USERNAME);
        globalcxScaPss = adminConfig.getSystemProperty(GLOBAL_CXSCA_PWD);
        
        globalFolderExclusions = adminConfig.getSystemProperty(GLOBAL_FOLDER_EXCLUSION);
        String filterProperty = adminConfig.getSystemProperty(GLOBAL_FILTER_PATTERN);
        if (filterProperty != null) {
            globalFilterPatterns = filterProperty;
        }

        globalScanTimeoutInMinutes = adminConfig.getSystemProperty(GLOBAL_SCAN_TIMEOUT_IN_MINUTES);
        globalIsSynchronous = adminConfig.getSystemProperty(GLOBAL_IS_SYNCHRONOUS);
        globalEnableProxy=adminConfig.getSystemProperty(GLOBAL_ENABLE_PROXY);
        globalHideResults = adminConfig.getSystemProperty(GLOBAL_HIDE_RESULTS);
        globalEnablePolicyViolations = adminConfig.getSystemProperty(GLOBAL_POLICY_VIOLATION_ENABLED);
        globalThresholdsEnabled = adminConfig.getSystemProperty(GLOBAL_THRESHOLDS_ENABLED);
        globalHighThreshold = adminConfig.getSystemProperty(GLOBAL_HIGH_THRESHOLD);
        globalMediumThreshold = adminConfig.getSystemProperty(GLOBAL_MEDIUM_THRESHOLD);
        globalLowThreshold = adminConfig.getSystemProperty(GLOBAL_LOW_THRESHOLD);
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
        
        error |= isScanTimeoutInvalid();
        if ("true".equals(globalEnableDependencyScan)) {
        	if("AST_SCA".equals(globalDependencyScanType)){
        		error |= isURLInvalid(cxScaGlobalAPIUrl, GLOBAL_CXSCA_API_URL);
        		error |= isURLInvalid(cxGlobalAccessControlServerUrl, GLOBAL_CXSCA_ACCESS_CONTROL_URL);
        		error |= isURLInvalid(cxScaGlobalWebAppUrl, GLOBAL_CXSCA_WEBAPP_URL);
        	}
        }
        if ("true".equals(globalIsSynchronous)) {
            if ("true".equals(globalThresholdsEnabled)) {
                error |= isNegative(getGlobalHighThreshold(), GLOBAL_HIGH_THRESHOLD);
                error |= isNegative(getGlobalMediumThreshold(), GLOBAL_MEDIUM_THRESHOLD);
                error |= isNegative(getGlobalLowThreshold(), GLOBAL_LOW_THRESHOLD);
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
        adminConfig.setSystemProperty(GLOBAL_DEPENDENCY_SCAN_FILTER_PATTERNS, cxGlobalDependencyScanFilterPatterns);
        adminConfig.setSystemProperty(GLOBAL_DEPENDENCY_SCAN_FOLDER_EXCLUDE, cxGlobalDependencyScanfolderExclusions);
        
        adminConfig.setSystemProperty(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS, cxGlobalOsaArchiveIncludePatterns);
        adminConfig.setSystemProperty(GLOBAL_OSA_INSTALL_BEFORE_SCAN, cxGlobalOsaInstallBeforeScan);
        adminConfig.setSystemProperty(GLOBAL_CXSCA_API_URL, cxScaGlobalAPIUrl);
        adminConfig.setSystemProperty(GLOBAL_CXSCA_ACCESS_CONTROL_URL, cxGlobalAccessControlServerUrl);
        adminConfig.setSystemProperty(GLOBAL_CXSCA_WEBAPP_URL, cxScaGlobalWebAppUrl);
        adminConfig.setSystemProperty(GLOBAL_CXSCA_ACCOUNT_NAME, cxScaGlobalAccountName);
        
        adminConfig.setSystemProperty(GLOBAL_CXSCA_USERNAME, globalcxScaUsername);
        adminConfig.setSystemProperty(GLOBAL_CXSCA_PWD, encrypt(globalcxScaPss));

        adminConfig.setSystemProperty(GLOBAL_FOLDER_EXCLUSION, globalFolderExclusions);
        adminConfig.setSystemProperty(GLOBAL_FILTER_PATTERN, globalFilterPatterns);
        adminConfig.setSystemProperty(GLOBAL_SCAN_TIMEOUT_IN_MINUTES, globalScanTimeoutInMinutes);

        adminConfig.setSystemProperty(GLOBAL_IS_SYNCHRONOUS, globalIsSynchronous);
        adminConfig.setSystemProperty(GLOBAL_ENABLE_PROXY, globalEnableProxy);
        if (globalIsSynchronous == null) {
            globalThresholdsEnabled = null;
            globalOsaThresholdsEnabled = null;
            globalEnablePolicyViolations = null;
        }
        adminConfig.setSystemProperty(GLOBAL_POLICY_VIOLATION_ENABLED, globalEnablePolicyViolations);
        adminConfig.setSystemProperty(GLOBAL_THRESHOLDS_ENABLED, globalThresholdsEnabled);
        adminConfig.setSystemProperty(GLOBAL_HIGH_THRESHOLD, globalHighThreshold);
        adminConfig.setSystemProperty(GLOBAL_MEDIUM_THRESHOLD, globalMediumThreshold);
        adminConfig.setSystemProperty(GLOBAL_LOW_THRESHOLD, globalLowThreshold);
        adminConfig.setSystemProperty(GLOBAL_OSA_THRESHOLDS_ENABLED, globalOsaThresholdsEnabled);
        adminConfig.setSystemProperty(GLOBAL_OSA_HIGH_THRESHOLD, globalOsaHighThreshold);
        adminConfig.setSystemProperty(GLOBAL_OSA_MEDIUM_THRESHOLD, globalOsaMediumThreshold);
        adminConfig.setSystemProperty(GLOBAL_OSA_LOW_THRESHOLD, globalOsaLowThreshold);
        adminConfig.setSystemProperty(GLOBAL_DENY_PROJECT, globalDenyProject);
        adminConfig.setSystemProperty(GLOBAL_HIDE_RESULTS, globalHideResults);

        ((AdministrationConfigurationPersister) ContainerManager.getComponent("administrationConfigurationPersister")).saveAdministrationConfiguration(adminConfig);

        addActionMessage(getText("cxDefaultConfigSuccess.label"));
        return SUCCESS;
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

    private boolean isNegative(@NotNull String value, @NotNull String key) {
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

	public String getCxGlobalDependencyScanFilterPatterns() {
		return cxGlobalDependencyScanFilterPatterns;
	}

	public void setCxGlobalDependencyScanFilterPatterns(String cxGlobalDependencyScanFilterPatterns) {
		this.cxGlobalDependencyScanFilterPatterns = cxGlobalDependencyScanFilterPatterns;
	}
	public String getCxGlobalOsaArchiveIncludePatterns() {
		return cxGlobalOsaArchiveIncludePatterns;
	}

	public void setCxGlobalOsaArchiveIncludePatterns(String cxGlobalOsaArchiveIncludePatterns) {
		this.cxGlobalOsaArchiveIncludePatterns = cxGlobalOsaArchiveIncludePatterns;
	}

	public String getCxGlobalOsaInstallBeforeScan() {
		return cxGlobalOsaInstallBeforeScan;
	}

	public void setCxGlobalOsaInstallBeforeScan(String cxGlobalOsaInstallBeforeScan) {
		this.cxGlobalOsaInstallBeforeScan = cxGlobalOsaInstallBeforeScan;
	}

	public String getCxScaGlobalAPIUrl() {
		return cxScaGlobalAPIUrl;
	}

	public void setCxScaGlobalAPIUrl(String cxScaGlobalAPIUrl) {
		this.cxScaGlobalAPIUrl = cxScaGlobalAPIUrl;
	}

	public String getCxGlobalAccessControlServerUrl() {
		return cxGlobalAccessControlServerUrl;
	}

	public void setCxGlobalAccessControlServerUrl(String cxGlobalAccessControlServerUrl) {
		this.cxGlobalAccessControlServerUrl = cxGlobalAccessControlServerUrl;
	}

	public String getCxScaGlobalWebAppUrl() {
		return cxScaGlobalWebAppUrl;
	}

	public void setCxScaGlobalWebAppUrl(String cxScaGlobalWebAppUrl) {
		this.cxScaGlobalWebAppUrl = cxScaGlobalWebAppUrl;
	}

	public String getCxScaGlobalAccountName() {
		return cxScaGlobalAccountName;
	}

	public void setCxScaGlobalAccountName(String cxScaGlobalAccountName) {
		this.cxScaGlobalAccountName = cxScaGlobalAccountName;
	}

	public Map<String, String> getGlobalDependencyScanTypeValues() {
		return globalDependencyScanTypeValues;
	}

	public void setGlobalDependencyScanTypeValues(Map<String, String> globalDependencyScanTypeValues) {
		this.globalDependencyScanTypeValues = globalDependencyScanTypeValues;
	}
	public String getCxGlobalDependencyScanfolderExclusions() {
		return cxGlobalDependencyScanfolderExclusions;
	}

	public void setCxGlobalDependencyScanfolderExclusions(String cxGlobalDependencyScanfolderExclusions) {
		this.cxGlobalDependencyScanfolderExclusions = cxGlobalDependencyScanfolderExclusions;
	}

}
