package com.cx.plugin.configuration;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.ww2.actions.build.admin.config.task.ConfigureBuildTasks;
import com.atlassian.spring.container.ContainerManager;
import com.cx.plugin.utils.HttpHelper;
import com.cx.plugin.utils.SASTUtils;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.ProxyConfig;
import com.cx.restclient.dto.ScannerType;
import com.cx.restclient.dto.Team;
import com.cx.restclient.sast.dto.Preset;
import com.cx.restclient.sast.utils.LegacyClient;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.cx.plugin.utils.CxParam.*;
import static com.cx.plugin.utils.CxPluginUtils.decrypt;
import static com.cx.plugin.utils.CxPluginUtils.encrypt;


public class AgentTaskConfigurator extends AbstractTaskConfigurator {
    private LinkedHashMap<String, String> presetList = new LinkedHashMap<String, String>();
    private LinkedHashMap<String, String> teamPathList = new LinkedHashMap<String, String>();
    private LinkedHashMap<String, String> intervalList = new LinkedHashMap<String, String>();
    private LegacyClient commonClient = null;
    private AdministrationConfiguration adminConfig;

	private final static String DEFAULT_SETTING_LABEL = "Use Global Setting";
    private final static String SPECIFIC_SETTING_LABEL = "Use Specific Setting";
    private final static String DEFAULT_SERVER_URL = "http://";
    private final static int MAX_PROJECT_NAME_LENGTH = 200;
    private static final String DEFAULT_INTERVAL_BEGINS = "01:00";
    private static final String DEFAULT_INTERVAL_ENDS = "04:00";
    
    private boolean criticalSupported = false;

    private Map<String, String> CONFIGURATION_MODE_TYPES_MAP_SERVER = ImmutableMap.of(GLOBAL_CONFIGURATION_SERVER, DEFAULT_SETTING_LABEL, CUSTOM_CONFIGURATION_SERVER, SPECIFIC_SETTING_LABEL);
    private Map<String, String> CONFIGURATION_MODE_TYPES_MAP_CXSAST = ImmutableMap.of(GLOBAL_CONFIGURATION_CXSAST, DEFAULT_SETTING_LABEL, CUSTOM_CONFIGURATION_CXSAST, SPECIFIC_SETTING_LABEL);
    private Map<String, String> CONFIGURATION_MODE_TYPES_MAP_CONTROL = ImmutableMap.of(GLOBAL_CONFIGURATION_CONTROL, DEFAULT_SETTING_LABEL, CUSTOM_CONFIGURATION_CONTROL, SPECIFIC_SETTING_LABEL);
    private Map<String, String> DEPENDENCY_SCAN_TYPES_MAP_DEPENDENCY_SCAN = ImmutableMap.of("OSA", "Use CxOSA dependency scanner", "AST_SCA", "Use CxSCA dependency scanner");
    private final Logger log = LoggerFactory.getLogger(AgentTaskConfigurator.class);

    //create task configuration
    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
    	adminConfig = (AdministrationConfiguration) ContainerManager.getComponent(ADMINISTRATION_CONFIGURATION);
    	super.populateContextForCreate(context);
        context.put("configurationModeTypesServer", CONFIGURATION_MODE_TYPES_MAP_SERVER);
        context.put("configurationModeTypesCxSAST", CONFIGURATION_MODE_TYPES_MAP_CXSAST);
        context.put("configurationModeTypesControl", CONFIGURATION_MODE_TYPES_MAP_CONTROL);
        context.put("dependencyScanTypeValues", DEPENDENCY_SCAN_TYPES_MAP_DEPENDENCY_SCAN);
        
        String projectName = resolveProjectName(context);
        context.put(PROJECT_NAME, projectName);
        context.put(SERVER_URL, DEFAULT_SERVER_URL);
        context.put(ENABLE_SAST_SCAN, OPTION_TRUE);
        context.put(ENABLE_PROXY,getAdminConfig(GLOBAL_ENABLE_PROXY));
        context.put(GLOBAL_ENABLE_PROXY,getAdminConfig(GLOBAL_ENABLE_PROXY));
        populateCredentialsFieldsForCreate(context);        
        populateCxSASTFields(context, null, true);
        //context.put(ENABLE_PROXY, OPTION_FALSE);
        context.put(IS_INCREMENTAL, OPTION_FALSE);
        context.put(IS_INTERVALS, OPTION_FALSE);
        context.put(FORCE_SCAN, OPTION_FALSE);
        populateIntervals(context);
        context.put(INTERVAL_BEGINS, DEFAULT_INTERVAL_BEGINS);
        context.put(INTERVAL_ENDS, DEFAULT_INTERVAL_ENDS);

        populateScanControlFields(context, null, true);

        context.put(GENERATE_PDF_REPORT, OPTION_FALSE);
        populateOSA_SCA_FieldsForCreate(context);
        		
    }

    private String resolveProjectName(@NotNull Map<String, Object> context) {
        String projectName;
        try {
            Object plan = context.get("plan");
            Method getName = plan.getClass().getDeclaredMethod("getName");
            projectName = (String) getName.invoke(plan);
        } catch (Exception e) {
            projectName = "";
        }
        return projectName;
    }

    private void populateCredentialsFieldsForCreate(final Map<String, Object> context) {
        String cxServerUrl = getAdminConfig(GLOBAL_SERVER_URL);
        String cxUser = getAdminConfig(GLOBAL_USER_NAME);
        String cxPass = getAdminConfig(GLOBAL_PWD);
        String proxyEnable = getAdminConfig(GLOBAL_ENABLE_PROXY);

        context.put(SERVER_URL, "");
        context.put(USER_NAME, "");
        context.put(PASSWORD, "");
        context.put(GLOBAL_SERVER_URL, cxServerUrl);
        context.put(GLOBAL_USER_NAME, cxUser);
        context.put(GLOBAL_PWD, cxPass);
        context.put(SERVER_CREDENTIALS_SECTION, GLOBAL_CONFIGURATION_SERVER);

        populateTeamAndPresetFields(cxServerUrl, cxUser, cxPass,proxyEnable, null, null, context);
    }

    private void populateTeamAndPresetFields(final String serverUrl, final String username, final String password, String proxyEnable, String preset, String teamPath, @NotNull final Map<String, Object> context) {
        try {
            //the method initialized the CxClient service
            if (tryLogin(username, decrypt(password), serverUrl, proxyEnable)) {

                presetList = convertPresetToMap(commonClient.getPresetList());
                context.put(PRESET_LIST, presetList);
                if (!StringUtils.isEmpty(preset)) {
                    context.put(PRESET_ID, preset);
                } else if (!presetList.isEmpty()) {
                    context.put(PRESET_ID, presetList.entrySet().iterator().next());
                }

                teamPathList = convertTeamPathToMap(commonClient.getTeamList());
                context.put(TEAM_PATH_LIST, teamPathList);
                if (!StringUtils.isEmpty(teamPath)) {
                    context.put(TEAM_PATH_ID, teamPath);
                } else if (!teamPathList.isEmpty())
                    context.put(TEAM_PATH_ID, teamPathList.entrySet().iterator().next());

            } else {

                final Map<String, String> noPresets = new HashMap<String, String>();
                noPresets.put(NO_PRESET, NO_PRESET_MESSAGE);
                context.put(PRESET_LIST, noPresets);

                final Map<String, String> noTeams = new HashMap<String, String>();
                noTeams.put(NO_TEAM_PATH, NO_TEAM_MESSAGE);
                context.put(TEAM_PATH_LIST, noTeams);
            }
        } catch (Exception e) {
            log.error("Exception caught during populateTeamAndPresetFields: '" + e.getMessage() + "'", e);
        }
    }

    //edit task configuration
    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition) {
    	adminConfig = (AdministrationConfiguration) ContainerManager.getComponent(ADMINISTRATION_CONFIGURATION);
    	super.populateContextForEdit(context, taskDefinition);
        Map<String, String> configMap = taskDefinition.getConfiguration();

        context.put("configurationModeTypesServer", CONFIGURATION_MODE_TYPES_MAP_SERVER);
        context.put("configurationModeTypesCxSAST", CONFIGURATION_MODE_TYPES_MAP_CXSAST);
        context.put("configurationModeTypesControl", CONFIGURATION_MODE_TYPES_MAP_CONTROL);
        context.put("dependencyScanTypeValues", DEPENDENCY_SCAN_TYPES_MAP_DEPENDENCY_SCAN);
        
        context.put(PROJECT_NAME, configMap.get(PROJECT_NAME));

        populateCredentialsFieldsForEdit(context, configMap);
        if(configMap.get(ENABLE_SAST_SCAN) == null) {
        	context.put(ENABLE_SAST_SCAN, OPTION_TRUE);
        }
        else {
        context.put(ENABLE_SAST_SCAN, configMap.get(ENABLE_SAST_SCAN));
        }
        populateCxSASTFields(context, configMap, false);
        context.put(IS_INCREMENTAL, configMap.get(IS_INCREMENTAL));
        context.put(FORCE_SCAN, configMap.get(FORCE_SCAN));
        context.put(ENABLE_PROXY, configMap.get(ENABLE_PROXY));
        final String isIntervals = configMap.get(IS_INTERVALS);
        context.put(IS_INTERVALS, isIntervals);
        populateIntervals(context);

        String intervalBegins = StringUtils.isEmpty(configMap.get(INTERVAL_BEGINS)) ? DEFAULT_INTERVAL_BEGINS : configMap.get(INTERVAL_BEGINS);
        String intervalEnds = StringUtils.isEmpty(configMap.get(INTERVAL_ENDS)) ? DEFAULT_INTERVAL_ENDS : configMap.get(INTERVAL_ENDS);

        context.put(INTERVAL_BEGINS, intervalBegins);
        context.put(INTERVAL_ENDS, intervalEnds);

        context.put(GENERATE_PDF_REPORT, configMap.get(GENERATE_PDF_REPORT));
        
        populateOSA_SCA_FieldsForEdit(context, configMap);

        populateScanControlFields(context, configMap, false);
    }
    
    private boolean checkVisiblityOfSCACriticalThreshold(String enableDependencyScan, String useCustomDependencySettings, 
    		String dependencyScanType, String globalDependencyScanType) {
    	boolean visiblity = false;
    	if("true".equalsIgnoreCase(enableDependencyScan) 
    		&& (("true".equalsIgnoreCase(useCustomDependencySettings) && ("AST_SCA".equalsIgnoreCase(dependencyScanType)))
    				|| (("false".equalsIgnoreCase(useCustomDependencySettings) && ("AST_SCA".equalsIgnoreCase(globalDependencyScanType)))))) {
    		visiblity = true;
    	}
    
    	return true;
    }

    private void populateOSA_SCA_FieldsForEdit(Map<String, Object> context, Map<String, String> configMap) {
		context.put(CX_USE_CUSTOM_DEPENDENCY_SETTINGS,configMap.get(CX_USE_CUSTOM_DEPENDENCY_SETTINGS));
		String useCustomdependencyScanSettings = configMap.get(CX_USE_CUSTOM_DEPENDENCY_SETTINGS);
		
		boolean enableDependencyScan = Boolean.parseBoolean(configMap.get(ENABLE_DEPENDENCY_SCAN));
		context.put(ENABLE_DEPENDENCY_SCAN, configMap.get(ENABLE_DEPENDENCY_SCAN));
		
		if(!StringUtils.isEmpty(useCustomdependencyScanSettings) && useCustomdependencyScanSettings.equalsIgnoreCase("true")) {
		
	        String dependencyScanType = configMap.get(DEPENDENCY_SCAN_TYPE);
	    	String osaEnabled = "false";
	    	
	        context.put(DEPENDENCY_SCAN_TYPE, configMap.get(DEPENDENCY_SCAN_TYPE));
	        
	        if(enableDependencyScan && dependencyScanType.equalsIgnoreCase(ScannerType.OSA.toString()))
	        	osaEnabled = "true";
	        
			context.put(OSA_ENABLED, osaEnabled);
	        context.put(OSA_INSTALL_BEFORE_SCAN, configMap.get(OSA_INSTALL_BEFORE_SCAN));
	        context.put(DEPENDENCY_SCAN_FILTER_PATTERNS, configMap.get(DEPENDENCY_SCAN_FILTER_PATTERNS));
	        context.put(DEPENDENCY_SCAN_FOLDER_EXCLUDE, configMap.get(DEPENDENCY_SCAN_FOLDER_EXCLUDE));
	        
	        context.put(OSA_ARCHIVE_INCLUDE_PATTERNS, configMap.get(OSA_ARCHIVE_INCLUDE_PATTERNS));
			
			context.put(CXSCA_API_URL,configMap.get(CXSCA_API_URL));
	        context.put(CXSCA_ACCESS_CONTROL_URL,configMap.get(CXSCA_ACCESS_CONTROL_URL));
			context.put(CXSCA_WEBAPP_URL,configMap.get(CXSCA_WEBAPP_URL));
			context.put(CXSCA_ACCOUNT_NAME,configMap.get(CXSCA_ACCOUNT_NAME));
			context.put(CXSCA_USERNAME,configMap.get(CXSCA_USERNAME));
			context.put(CXSCA_PWD,configMap.get(CXSCA_PWD));       
			context.put(CXSCA_RESOLVER_PATH,configMap.get(CXSCA_RESOLVER_PATH));
            context.put(CXSCA_RESOLVER_ADD_PARAM,configMap.get(CXSCA_RESOLVER_ADD_PARAM));       
            context.put(CXSCA_RESOLVER_ENABLED,configMap.get(CXSCA_RESOLVER_ENABLED)); 
            
		}else {
						
	        String dependencyScanType = getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE);
	    	String osaEnabled = "false";
	    	
	        context.put(DEPENDENCY_SCAN_TYPE, getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE));
	        
	        if(enableDependencyScan && dependencyScanType.equalsIgnoreCase(ScannerType.OSA.toString()))
	        	osaEnabled = "true";
	        
			context.put(OSA_ENABLED, osaEnabled);
	        context.put(OSA_INSTALL_BEFORE_SCAN, getAdminConfig(GLOBAL_OSA_INSTALL_BEFORE_SCAN));
	        context.put(DEPENDENCY_SCAN_FILTER_PATTERNS, getAdminConfig(GLOBAL_DEPENDENCY_SCAN_FILTER_PATTERNS));
	        context.put(DEPENDENCY_SCAN_FOLDER_EXCLUDE, getAdminConfig(GLOBAL_DEPENDENCY_SCAN_FOLDER_EXCLUDE));
	        
	        context.put(OSA_ARCHIVE_INCLUDE_PATTERNS, getAdminConfig(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS));
			
			context.put(CXSCA_API_URL,getAdminConfig(GLOBAL_CXSCA_API_URL));
	        context.put(CXSCA_ACCESS_CONTROL_URL,getAdminConfig(GLOBAL_CXSCA_ACCESS_CONTROL_URL));
			context.put(CXSCA_WEBAPP_URL,getAdminConfig(GLOBAL_CXSCA_WEBAPP_URL));
			context.put(CXSCA_ACCOUNT_NAME,getAdminConfig(GLOBAL_CXSCA_ACCOUNT_NAME));
			context.put(CXSCA_USERNAME,getAdminConfig(GLOBAL_CXSCA_USERNAME));
			context.put(CXSCA_PWD,getAdminConfig(GLOBAL_CXSCA_PWD));	
			context.put(CXSCA_RESOLVER_PATH_GLOBAL,configMap.get(CXSCA_RESOLVER_PATH_GLOBAL));
            context.put(CXSCA_RESOLVER_ADD_PARAM_GLOBAL,configMap.get(CXSCA_RESOLVER_ADD_PARAM_GLOBAL));       
            context.put(CXSCA_RESOLVER_ENABLED_GLOBAL,configMap.get(CXSCA_RESOLVER_ENABLED_GLOBAL)); 
			
		}	
		
		//These are to show global readonly values when user decide to use global
		//values on the job. Else they need to browse global section everytime
    	context.put(GLOBAL_ENABLE_DEPENDENCY_SCAN, getAdminConfig(GLOBAL_ENABLE_DEPENDENCY_SCAN));
    	context.put(GLOBAL_DEPENDENCY_SCAN_TYPE, getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE));
        context.put(GLOBAL_DEPENDENCY_SCAN_FILTER_PATTERNS, getAdminConfig(GLOBAL_DEPENDENCY_SCAN_FILTER_PATTERNS));
        context.put(GLOBAL_DEPENDENCY_SCAN_FOLDER_EXCLUDE, getAdminConfig(GLOBAL_DEPENDENCY_SCAN_FOLDER_EXCLUDE));
        context.put(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS, getAdminConfig(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS));
		
        context.put(GLOBAL_CXSCA_API_URL,getAdminConfig(GLOBAL_CXSCA_API_URL));
        context.put(GLOBAL_CXSCA_ACCESS_CONTROL_URL,getAdminConfig(GLOBAL_CXSCA_ACCESS_CONTROL_URL));
		context.put(GLOBAL_CXSCA_WEBAPP_URL,getAdminConfig(GLOBAL_CXSCA_WEBAPP_URL));
		context.put(GLOBAL_CXSCA_ACCOUNT_NAME,getAdminConfig(GLOBAL_CXSCA_ACCOUNT_NAME));
		context.put(GLOBAL_CXSCA_USERNAME,getAdminConfig(GLOBAL_CXSCA_USERNAME));
		context.put(GLOBAL_CXSCA_PWD,getAdminConfig(GLOBAL_CXSCA_PWD));
		context.put(CXSCA_RESOLVER_PATH_GLOBAL,configMap.get(CXSCA_RESOLVER_PATH_GLOBAL));
        context.put(CXSCA_RESOLVER_ADD_PARAM_GLOBAL,configMap.get(CXSCA_RESOLVER_ADD_PARAM_GLOBAL));       
        context.put(CXSCA_RESOLVER_ENABLED_GLOBAL,configMap.get(CXSCA_RESOLVER_ENABLED_GLOBAL)); 
        context.put(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS, getAdminConfig(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS));
        context.put(GLOBAL_OSA_INSTALL_BEFORE_SCAN, getAdminConfig(GLOBAL_OSA_INSTALL_BEFORE_SCAN));
       
        boolean visiblity = checkVisiblityOfSCACriticalThreshold(configMap.get(ENABLE_DEPENDENCY_SCAN),
        		configMap.get(CX_USE_CUSTOM_DEPENDENCY_SETTINGS),configMap.get(DEPENDENCY_SCAN_TYPE),getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE));
        log.info("visiblity " + visiblity);
        log.info("ENABLE_DEPENDENCY_SCAN " + configMap.get(ENABLE_DEPENDENCY_SCAN));
        log.info("CX_USE_CUSTOM_DEPENDENCY_SETTINGS " + configMap.get(CX_USE_CUSTOM_DEPENDENCY_SETTINGS));
        log.info("DEPENDENCY_SCAN_TYPE " + configMap.get(DEPENDENCY_SCAN_TYPE));
        log.info("GLOBAL_DEPENDENCY_SCAN_TYPE " + getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE));
        context.put("checkVisiblityOfSca", visiblity);
	}

    private void populateOSA_SCA_FieldsForCreate(Map<String, Object> context) {
    	
    	context.put(ENABLE_DEPENDENCY_SCAN, getAdminConfig(GLOBAL_ENABLE_DEPENDENCY_SCAN));
    	context.put(DEPENDENCY_SCAN_TYPE, getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE));
        context.put(DEPENDENCY_SCAN_FILTER_PATTERNS, getAdminConfig(GLOBAL_DEPENDENCY_SCAN_FILTER_PATTERNS));
        context.put(DEPENDENCY_SCAN_FOLDER_EXCLUDE, getAdminConfig(GLOBAL_DEPENDENCY_SCAN_FOLDER_EXCLUDE));
        
        context.put(OSA_ARCHIVE_INCLUDE_PATTERNS, getAdminConfig(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS));
        context.put(OSA_INSTALL_BEFORE_SCAN, getAdminConfig(GLOBAL_OSA_INSTALL_BEFORE_SCAN));
		
        context.put(CXSCA_API_URL,getAdminConfig(GLOBAL_CXSCA_API_URL));
        context.put(CXSCA_ACCESS_CONTROL_URL,getAdminConfig(GLOBAL_CXSCA_ACCESS_CONTROL_URL));
		context.put(CXSCA_WEBAPP_URL,getAdminConfig(GLOBAL_CXSCA_WEBAPP_URL));
		context.put(CXSCA_ACCOUNT_NAME,getAdminConfig(GLOBAL_CXSCA_ACCOUNT_NAME));
		
		context.put(CX_USE_CUSTOM_DEPENDENCY_SETTINGS,OPTION_FALSE);		
		context.put(CXSCA_USERNAME,"");
		context.put(CXSCA_PWD,"");
		
		
        
		//These are to show global readonly values when user decide to use global
		//values on the job. Else they need to browse global section everytime
    	context.put(GLOBAL_ENABLE_DEPENDENCY_SCAN, getAdminConfig(GLOBAL_ENABLE_DEPENDENCY_SCAN));
    	context.put(GLOBAL_DEPENDENCY_SCAN_TYPE, getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE));
        context.put(GLOBAL_DEPENDENCY_SCAN_FILTER_PATTERNS, getAdminConfig(GLOBAL_DEPENDENCY_SCAN_FILTER_PATTERNS));
        context.put(GLOBAL_DEPENDENCY_SCAN_FOLDER_EXCLUDE, getAdminConfig(GLOBAL_DEPENDENCY_SCAN_FOLDER_EXCLUDE));
        context.put(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS, getAdminConfig(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS));
		
        context.put(GLOBAL_CXSCA_API_URL,getAdminConfig(GLOBAL_CXSCA_API_URL));
        context.put(GLOBAL_CXSCA_ACCESS_CONTROL_URL,getAdminConfig(GLOBAL_CXSCA_ACCESS_CONTROL_URL));
		context.put(GLOBAL_CXSCA_WEBAPP_URL,getAdminConfig(GLOBAL_CXSCA_WEBAPP_URL));
		context.put(GLOBAL_CXSCA_ACCOUNT_NAME,getAdminConfig(GLOBAL_CXSCA_ACCOUNT_NAME));
		context.put(GLOBAL_CXSCA_USERNAME,getAdminConfig(GLOBAL_CXSCA_USERNAME));
		context.put(GLOBAL_CXSCA_PWD,getAdminConfig(GLOBAL_CXSCA_PWD));

        context.put(CXSCA_RESOLVER_ENABLED_GLOBAL,getAdminConfig(CXSCA_RESOLVER_ENABLED_GLOBAL));
        context.put(CXSCA_RESOLVER_PATH_GLOBAL,getAdminConfig(CXSCA_RESOLVER_PATH_GLOBAL));
        context.put(CXSCA_RESOLVER_ADD_PARAM_GLOBAL,getAdminConfig(CXSCA_RESOLVER_ADD_PARAM_GLOBAL));

        context.put(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS, getAdminConfig(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS));
        context.put(GLOBAL_OSA_INSTALL_BEFORE_SCAN, getAdminConfig(GLOBAL_OSA_INSTALL_BEFORE_SCAN));
        
        boolean visiblity = checkVisiblityOfSCACriticalThreshold(getAdminConfig(GLOBAL_ENABLE_DEPENDENCY_SCAN),
        		OPTION_FALSE ,getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE),getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE));
        log.info("visiblity " + visiblity);
        log.info("ENABLE_DEPENDENCY_SCAN " + getAdminConfig(GLOBAL_ENABLE_DEPENDENCY_SCAN));
        log.info("CX_USE_CUSTOM_DEPENDENCY_SETTINGS " + OPTION_FALSE);
        log.info("DEPENDENCY_SCAN_TYPE " + getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE));
        log.info("GLOBAL_DEPENDENCY_SCAN_TYPE " + getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE));
        context.put("checkVisiblityOfSca", visiblity);
			
	}
    
	private void populateCredentialsFieldsForEdit(@NotNull final Map<String, Object> context, Map<String, String> configMap) {
        String cxServerUrl;
        String cxUser;
        String cxPass;
        String cxProxyEnabled;
        String configType = configMap.get(SERVER_CREDENTIALS_SECTION);        
        if ((GLOBAL_CONFIGURATION_SERVER.equals(configType))) {
            cxServerUrl = getAdminConfig(GLOBAL_SERVER_URL);
            cxUser = getAdminConfig(GLOBAL_USER_NAME);
            cxPass = getAdminConfig(GLOBAL_PWD);            
            cxProxyEnabled = getAdminConfig(GLOBAL_ENABLE_PROXY);
            
            context.put(SERVER_URL, configMap.get(GLOBAL_SERVER_URL));
            context.put(USER_NAME, configMap.get(GLOBAL_USER_NAME));
            context.put(PASSWORD, configMap.get(GLOBAL_PWD));
            context.put(ENABLE_PROXY, configMap.get(GLOBAL_ENABLE_PROXY));

        } else {        	
        	context.put(ENABLE_SAST_SCAN, configMap.get(ENABLE_SAST_SCAN));
            cxServerUrl = configMap.get(SERVER_URL);
            cxUser = configMap.get(USER_NAME);
            cxPass = configMap.get(PASSWORD);
            cxProxyEnabled = configMap.get(ENABLE_PROXY);
            context.put(SERVER_URL, configMap.get(SERVER_URL));
            context.put(USER_NAME, configMap.get(USER_NAME));
            context.put(PASSWORD, configMap.get(PASSWORD));
            context.put(ENABLE_PROXY, configMap.get(ENABLE_PROXY));
        }

        context.put(GLOBAL_SERVER_URL, getAdminConfig(GLOBAL_SERVER_URL));
        context.put(GLOBAL_USER_NAME, getAdminConfig(GLOBAL_USER_NAME));
        context.put(GLOBAL_PWD, getAdminConfig(GLOBAL_PWD));
        context.put(GLOBAL_ENABLE_PROXY, getAdminConfig(GLOBAL_ENABLE_PROXY));


        context.put(SERVER_CREDENTIALS_SECTION, configType);

        String cxPreset = configMap.get(PRESET_ID);
        String cxTeam = configMap.get(TEAM_PATH_ID);

        populateTeamAndPresetFields(cxServerUrl, cxUser, cxPass,cxProxyEnabled,  cxPreset, cxTeam, context);
    }

    private void populateCxSASTFields(@NotNull final Map<String, Object> context, Map<String, String> configMap, boolean forCreate) {
        if (forCreate) {
            context.put(CXSAST_SECTION, GLOBAL_CONFIGURATION_CXSAST);
            context.put(FOLDER_EXCLUSION, "");
            context.put(FILTER_PATTERN, DEFAULT_FILTER_PATTERNS);
            context.put(SCAN_TIMEOUT_IN_MINUTES, "");
            context.put(COMMENT, "");

        } else {
            context.put(CXSAST_SECTION, configMap.get(CXSAST_SECTION));
            context.put(FOLDER_EXCLUSION, configMap.get(FOLDER_EXCLUSION));
            context.put(FILTER_PATTERN, configMap.get(FILTER_PATTERN));
            context.put(SCAN_TIMEOUT_IN_MINUTES, configMap.get(SCAN_TIMEOUT_IN_MINUTES));
            context.put(COMMENT, configMap.get(COMMENT));
        }

        context.put(GLOBAL_FILTER_PATTERN, getAdminConfig(GLOBAL_FILTER_PATTERN));
        context.put(GLOBAL_FOLDER_EXCLUSION, getAdminConfig(GLOBAL_FOLDER_EXCLUSION));
        context.put(GLOBAL_FILTER_PATTERN, getAdminConfig(GLOBAL_FILTER_PATTERN));
        context.put(GLOBAL_SCAN_TIMEOUT_IN_MINUTES, getAdminConfig(GLOBAL_SCAN_TIMEOUT_IN_MINUTES));
    }

    private void populateScanControlFields(@NotNull final Map<String, Object> context, Map<String, String> configMap, boolean forCreate) {
        if (forCreate) {
            context.put(SCAN_CONTROL_SECTION, GLOBAL_CONFIGURATION_CONTROL);
            context.put(IS_SYNCHRONOUS, OPTION_TRUE);
            context.put(POLICY_VIOLATION_ENABLED, OPTION_FALSE);
            context.put(POLICY_VIOLATION_ENABLED_SCA, OPTION_FALSE);
            context.put(THRESHOLDS_ENABLED, OPTION_FALSE);
            context.put(ENABLE_CRITICAL_SEVERITY, OPTION_FALSE);
            context.put(CRITICAL_THRESHOLD, "");
            context.put(HIGH_THRESHOLD, "");
            context.put(MEDIUM_THRESHOLD, "");
            context.put(LOW_THRESHOLD, "");
            context.put(OSA_THRESHOLDS_ENABLED, OPTION_FALSE);
            context.put(OSA_CRITICAL_THRESHOLD, "");
            context.put(OSA_HIGH_THRESHOLD, "");
            context.put(OSA_MEDIUM_THRESHOLD, "");
            context.put(OSA_LOW_THRESHOLD, "");
        } else {
            context.put(SCAN_CONTROL_SECTION, configMap.get(SCAN_CONTROL_SECTION));
            context.put(IS_SYNCHRONOUS, configMap.get(IS_SYNCHRONOUS));
            context.put(POLICY_VIOLATION_ENABLED, configMap.get(POLICY_VIOLATION_ENABLED));
            context.put(POLICY_VIOLATION_ENABLED_SCA, configMap.get(POLICY_VIOLATION_ENABLED_SCA));
            context.put(THRESHOLDS_ENABLED, configMap.get(THRESHOLDS_ENABLED));
            context.put(ENABLE_CRITICAL_SEVERITY, configMap.get(ENABLE_CRITICAL_SEVERITY));
            context.put(CRITICAL_THRESHOLD, configMap.get(CRITICAL_THRESHOLD));
            context.put(HIGH_THRESHOLD, configMap.get(HIGH_THRESHOLD));
            context.put(MEDIUM_THRESHOLD, configMap.get(MEDIUM_THRESHOLD));
            context.put(LOW_THRESHOLD, configMap.get(LOW_THRESHOLD));
            context.put(OSA_THRESHOLDS_ENABLED, configMap.get(OSA_THRESHOLDS_ENABLED));
            context.put(OSA_CRITICAL_THRESHOLD, configMap.get(OSA_CRITICAL_THRESHOLD));
            context.put(OSA_HIGH_THRESHOLD, configMap.get(OSA_HIGH_THRESHOLD));
            context.put(OSA_MEDIUM_THRESHOLD, configMap.get(OSA_MEDIUM_THRESHOLD));
            context.put(OSA_LOW_THRESHOLD, configMap.get(OSA_LOW_THRESHOLD));
        }

        context.put(GLOBAL_IS_SYNCHRONOUS, getAdminConfig(GLOBAL_IS_SYNCHRONOUS));
        context.put(GLOBAL_POLICY_VIOLATION_ENABLED, getAdminConfig(GLOBAL_POLICY_VIOLATION_ENABLED));
        context.put(GLOBAL_POLICY_VIOLATION_ENABLED_SCA, getAdminConfig(GLOBAL_POLICY_VIOLATION_ENABLED_SCA));
        context.put(GLOBAL_THRESHOLDS_ENABLED, getAdminConfig(GLOBAL_THRESHOLDS_ENABLED));
        context.put(GLOBAL_CRITICAL_THRESHOLD, getAdminConfig(GLOBAL_CRITICAL_THRESHOLD));
        context.put(GLOBAL_HIGH_THRESHOLD, getAdminConfig(GLOBAL_HIGH_THRESHOLD));
        context.put(GLOBAL_MEDIUM_THRESHOLD, getAdminConfig(GLOBAL_MEDIUM_THRESHOLD));
        context.put(GLOBAL_LOW_THRESHOLD, getAdminConfig(GLOBAL_LOW_THRESHOLD));
        context.put(GLOBAL_OSA_THRESHOLDS_ENABLED, getAdminConfig(GLOBAL_OSA_THRESHOLDS_ENABLED));
        context.put(GLOBAL_OSA_CRITICAL_THRESHOLD, getAdminConfig(GLOBAL_OSA_CRITICAL_THRESHOLD));
        context.put(GLOBAL_OSA_HIGH_THRESHOLD, getAdminConfig(GLOBAL_OSA_HIGH_THRESHOLD));
        context.put(GLOBAL_OSA_MEDIUM_THRESHOLD, getAdminConfig(GLOBAL_OSA_MEDIUM_THRESHOLD));
        context.put(GLOBAL_OSA_LOW_THRESHOLD, getAdminConfig(GLOBAL_OSA_LOW_THRESHOLD));
        context.put(GLOBAL_HIDE_RESULTS, getAdminConfig(GLOBAL_HIDE_RESULTS));
        context.put(GLOBAL_DENY_PROJECT, getAdminConfig(GLOBAL_DENY_PROJECT));
        
//        boolean visiblity = checkVisiblityOfSCACriticalThreshold(configMap.get(ENABLE_DEPENDENCY_SCAN),
//        		configMap.get(CX_USE_CUSTOM_DEPENDENCY_SETTINGS),configMap.get(DEPENDENCY_SCAN_TYPE),getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE));
//        log.info("visiblity " + visiblity);
//        log.info("ENABLE_DEPENDENCY_SCAN " + configMap.get(ENABLE_DEPENDENCY_SCAN));
//        log.info("CX_USE_CUSTOM_DEPENDENCY_SETTINGS " + configMap.get(CX_USE_CUSTOM_DEPENDENCY_SETTINGS));
//        log.info("DEPENDENCY_SCAN_TYPE " + configMap.get(DEPENDENCY_SCAN_TYPE));
//        log.info("GLOBAL_DEPENDENCY_SCAN_TYPE " + getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE));
//        context.put("checkVisiblityOfSca", visiblity);
    }

    //save task configuration
    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @javax.annotation.Nullable final TaskDefinition previousTaskDefinition) {
    	Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config = generateCredentialsFields(params, config);
        config.put(ENABLE_SAST_SCAN, params.getString(ENABLE_SAST_SCAN));
        config.put(PROJECT_NAME, getDefaultString(params, PROJECT_NAME).trim());
        config.put(GENERATE_PDF_REPORT, params.getString(GENERATE_PDF_REPORT));
        config.put(ENABLE_PROXY,params.getString(ENABLE_PROXY));
        String presetId = params.getString(PRESET_ID);
        String presetName = "";


        if (!(NO_PRESET).equals(presetId)) {
            config.put(PRESET_ID, presetId);
            if (presetList.isEmpty()) {
                if (commonClient != null || tryLogin(params.getString(USER_NAME), decrypt(params.getString(PASSWORD)), params.getString(SERVER_URL),  params.getString(ENABLE_PROXY))) {
                    try {
                        Preset preset = commonClient.getPresetById(Integer.parseInt(presetId));
                        presetName = preset.getName();
                    } catch (Exception e) {
                        presetName = "";
                    }
                }
            } else {
                presetName = presetList.get(presetId);
            }
            config.put(PRESET_NAME, presetName);
        }

        String teamId = params.getString(TEAM_PATH_ID);
        String teaName = "";
        if (!NO_TEAM_PATH.equals(teamId)) {
            config.put(TEAM_PATH_ID, teamId);
            if (teamPathList.isEmpty()) {
                if (commonClient != null || tryLogin(params.getString(USER_NAME), decrypt(params.getString(PASSWORD)), params.getString(SERVER_URL), params.getString(ENABLE_PROXY))) {
                    try {
                        teaName = commonClient.getTeamNameById(teamId);
                    } catch (Exception e) {
                        teaName = "";
                    }
                }
            } else {
                teaName = teamPathList.get(teamId);
            }
            config.put(TEAM_PATH_NAME, teaName);
        }

        config.put(IS_SYNCHRONOUS, params.getString(IS_SYNCHRONOUS));
        config.put(POLICY_VIOLATION_ENABLED, params.getString(POLICY_VIOLATION_ENABLED));
        config.put(POLICY_VIOLATION_ENABLED_SCA, params.getString(POLICY_VIOLATION_ENABLED_SCA));

        config.put(IS_INCREMENTAL, params.getString(IS_INCREMENTAL));
        config.put(FORCE_SCAN, params.getString(FORCE_SCAN));
        config.put(IS_INTERVALS, params.getString(IS_INTERVALS));
        config.put(INTERVAL_BEGINS, getDefaultString(params, INTERVAL_BEGINS));
        config.put(INTERVAL_ENDS, getDefaultString(params, INTERVAL_ENDS));

        //save 'CxSAST Scan' fields
        config = generateCxSASTFields(params, config);

        //save Scan Control  fields
        config = generateScanControlFields(params, config);
        
        //save SCA and OSA fields
        config = generateCxOSAAndSCAFields(params, config);

        return config;
    }

    private Map<String, String> generateCredentialsFields(@NotNull final ActionParametersMap params, Map<String, String> config) {
        final String configType = getDefaultString(params, SERVER_CREDENTIALS_SECTION);
        config.put(SERVER_CREDENTIALS_SECTION, configType);
        config.put(SERVER_URL, getDefaultString(params, SERVER_URL).trim());
        config.put(USER_NAME, getDefaultString(params, USER_NAME).trim());
        config.put(PASSWORD, encrypt(getDefaultString(params, PASSWORD)));

        return config;
    }

    private Map<String, String> generateCxOSAAndSCAFields(@NotNull final ActionParametersMap params, Map<String, String> config) {
        
    	config.put(CX_USE_CUSTOM_DEPENDENCY_SETTINGS,getDefaultString(params, CX_USE_CUSTOM_DEPENDENCY_SETTINGS).trim());
        config.put(ENABLE_DEPENDENCY_SCAN, getDefaultString(params, ENABLE_DEPENDENCY_SCAN).trim());
		String useCustomdependencyScanSettings = getDefaultString(params, CX_USE_CUSTOM_DEPENDENCY_SETTINGS).trim();
		if(!StringUtils.isEmpty(useCustomdependencyScanSettings) && useCustomdependencyScanSettings.equalsIgnoreCase("true")) {
			final String configType = getDefaultString(params, DEPENDENCY_SCAN_TYPE);	    	
	        config.put(DEPENDENCY_SCAN_TYPE, configType);
	        
	        config.put(OSA_INSTALL_BEFORE_SCAN, getDefaultString(params, OSA_INSTALL_BEFORE_SCAN).trim());
	        config.put(DEPENDENCY_SCAN_FILTER_PATTERNS, getDefaultString(params, DEPENDENCY_SCAN_FILTER_PATTERNS).trim());
	        config.put(DEPENDENCY_SCAN_FOLDER_EXCLUDE, getDefaultString(params, DEPENDENCY_SCAN_FOLDER_EXCLUDE).trim());
	        config.put(OSA_ARCHIVE_INCLUDE_PATTERNS, getDefaultString(params, OSA_ARCHIVE_INCLUDE_PATTERNS).trim());
			config.put(CXSCA_API_URL,getDefaultString(params, CXSCA_API_URL).trim());
	        config.put(CXSCA_ACCESS_CONTROL_URL,getDefaultString(params, CXSCA_ACCESS_CONTROL_URL).trim());
	        config.put(CXSCA_WEBAPP_URL,getDefaultString(params, CXSCA_WEBAPP_URL).trim());
	        config.put(CXSCA_ACCOUNT_NAME,getDefaultString(params, CXSCA_ACCOUNT_NAME).trim());
	        
			config.put(CXSCA_USERNAME,getDefaultString(params, CXSCA_USERNAME).trim());
			config.put(CXSCA_PWD,encrypt(getDefaultString(params, CXSCA_PWD).trim()));
			
			config.put(CXSCA_RESOLVER_ENABLED,getDefaultString(params, CXSCA_RESOLVER_ENABLED).trim());
			config.put(CXSCA_RESOLVER_PATH,getDefaultString(params, CXSCA_RESOLVER_PATH).trim());
			config.put(CXSCA_RESOLVER_ADD_PARAM,getDefaultString(params, CXSCA_RESOLVER_ADD_PARAM).trim());
	        
			
		}else {
			final String configType = getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE);	    	
	        config.put(DEPENDENCY_SCAN_TYPE, configType);
	        
	        config.put(OSA_INSTALL_BEFORE_SCAN, getAdminConfig(GLOBAL_OSA_INSTALL_BEFORE_SCAN).trim());
	        config.put(DEPENDENCY_SCAN_FILTER_PATTERNS, getAdminConfig(GLOBAL_DEPENDENCY_SCAN_FILTER_PATTERNS).trim());
	        config.put(DEPENDENCY_SCAN_FOLDER_EXCLUDE,getAdminConfig(GLOBAL_DEPENDENCY_SCAN_FOLDER_EXCLUDE).trim());
	        
	        config.put(OSA_ARCHIVE_INCLUDE_PATTERNS, getAdminConfig(GLOBAL_OSA_ARCHIVE_INCLUDE_PATTERNS).trim());
	        
			config.put(CXSCA_API_URL,getAdminConfig(GLOBAL_CXSCA_API_URL).trim());
	        config.put(CXSCA_ACCESS_CONTROL_URL,getAdminConfig(GLOBAL_CXSCA_ACCESS_CONTROL_URL).trim());
	        config.put(CXSCA_WEBAPP_URL,getAdminConfig(GLOBAL_CXSCA_WEBAPP_URL).trim());
	        config.put(CXSCA_ACCOUNT_NAME,getAdminConfig(GLOBAL_CXSCA_ACCOUNT_NAME).trim());
			
			config.put(CXSCA_USERNAME,getAdminConfig(GLOBAL_CXSCA_USERNAME).trim());
			config.put(CXSCA_PWD,getAdminConfig(GLOBAL_CXSCA_PWD).trim());
			config.put(CXSCA_RESOLVER_ENABLED_GLOBAL,getAdminConfig(CXSCA_RESOLVER_ENABLED_GLOBAL).trim());
			config.put(CXSCA_RESOLVER_PATH_GLOBAL,getAdminConfig(CXSCA_RESOLVER_PATH_GLOBAL).trim());
			config.put(CXSCA_RESOLVER_ADD_PARAM_GLOBAL,getAdminConfig(CXSCA_RESOLVER_ADD_PARAM_GLOBAL).trim());
			
		}
		
		
		boolean visiblity = checkVisiblityOfSCACriticalThreshold(getDefaultString(params, CX_USE_CUSTOM_DEPENDENCY_SETTINGS).trim(),
				getDefaultString(params, CX_USE_CUSTOM_DEPENDENCY_SETTINGS).trim(),getDefaultString(params, DEPENDENCY_SCAN_TYPE),getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE));
				log.info("visiblity " + visiblity);
				log.info("ENABLE_DEPENDENCY_SCAN " + getDefaultString(params, CX_USE_CUSTOM_DEPENDENCY_SETTINGS).trim());
				log.info("CX_USE_CUSTOM_DEPENDENCY_SETTINGS " + getDefaultString(params, CX_USE_CUSTOM_DEPENDENCY_SETTINGS).trim());
				log.info("DEPENDENCY_SCAN_TYPE " + getDefaultString(params, DEPENDENCY_SCAN_TYPE));
				log.info("GLOBAL_DEPENDENCY_SCAN_TYPE " + getAdminConfig(GLOBAL_DEPENDENCY_SCAN_TYPE));
				config.put("checkVisiblityOfSca", ""+ visiblity);
		
        return config;
    }
    
    private Map<String, String> generateCxSASTFields(@NotNull final ActionParametersMap params, Map<String, String> config) {
    	config.put(ENABLE_SAST_SCAN, getDefaultString(params, ENABLE_SAST_SCAN).trim());
        final String configType = getDefaultString(params, CXSAST_SECTION);
        config.put(CXSAST_SECTION, configType);
        config.put(FOLDER_EXCLUSION, getDefaultString(params, FOLDER_EXCLUSION).trim());
        config.put(FILTER_PATTERN, getDefaultString(params, FILTER_PATTERN).trim());
        config.put(SCAN_TIMEOUT_IN_MINUTES, getDefaultString(params, SCAN_TIMEOUT_IN_MINUTES).trim());
        config.put(COMMENT, getDefaultString(params, COMMENT).trim());

        return config;
    }

    private Map<String, String> generateScanControlFields(@NotNull final ActionParametersMap params, Map<String, String> config) {
        final String configType = getDefaultString(params, SCAN_CONTROL_SECTION);
        config.put(SCAN_CONTROL_SECTION, configType);
        config.put(THRESHOLDS_ENABLED, params.getString(THRESHOLDS_ENABLED));
        config.put(ENABLE_CRITICAL_SEVERITY, params.getString(ENABLE_CRITICAL_SEVERITY));
        config.put(CRITICAL_THRESHOLD, getDefaultString(params, CRITICAL_THRESHOLD));
        config.put(HIGH_THRESHOLD, getDefaultString(params, HIGH_THRESHOLD));
        config.put(MEDIUM_THRESHOLD, getDefaultString(params, MEDIUM_THRESHOLD));
        config.put(LOW_THRESHOLD, getDefaultString(params, LOW_THRESHOLD));
        config.put(OSA_THRESHOLDS_ENABLED, params.getString(OSA_THRESHOLDS_ENABLED));
        config.put(OSA_CRITICAL_THRESHOLD, getDefaultString(params, OSA_CRITICAL_THRESHOLD));
        config.put(OSA_HIGH_THRESHOLD, getDefaultString(params, OSA_HIGH_THRESHOLD));
        config.put(OSA_MEDIUM_THRESHOLD, getDefaultString(params, OSA_MEDIUM_THRESHOLD));
        config.put(OSA_LOW_THRESHOLD, getDefaultString(params, OSA_LOW_THRESHOLD));


        return config;
    }

    private String getDefaultString(@NotNull final ActionParametersMap params, String key) {
        return StringUtils.defaultString(params.getString(key));
    }

    //the method initialized common client
    private boolean tryLogin(String username, String cxPass, String serverUrl, String proxyEnable) {
        log.debug("Testing login with: server URL: " + serverUrl + " username" + username);

        if (!StringUtils.isEmpty(serverUrl) && !StringUtils.isEmpty(username) && !StringUtils.isEmpty(cxPass)) {
            try {
                 try {
					CxScanConfig scanConfig = new CxScanConfig(serverUrl, username, decrypt(cxPass),
							CommonClientFactory.SCAN_ORIGIN, true);
					ProxyConfig proxyConfig = HttpHelper.getProxyConfig();					
					if (proxyEnable != null && proxyEnable.equalsIgnoreCase("true") && proxyConfig != null) {
						scanConfig.setProxy(true);
						scanConfig.setProxyConfig(proxyConfig);
			            log.debug("Testing login with proxy details:");
						log.debug("Proxy host: " + proxyConfig.getHost());
						log.debug("Proxy port: " + proxyConfig.getPort());
						log.debug("Proxy user: " + proxyConfig.getUsername());
						log.debug("Proxy password: *************");
						log.debug("Proxy Scheme: " + (proxyConfig.isUseHttps() ? "https" : "http"));
						log.debug("Non Proxy Hosts: " + proxyConfig.getNoproxyHosts());
					}else {
						scanConfig.setProxy(false);
			            log.debug("Testing login.");
					}
					
                    commonClient = CommonClientFactory.getInstance(scanConfig, log);
                } catch (Exception e) {
                    log.debug("Failed to initialize cx client " + e.getMessage(), e);
                    commonClient = null;
                }
                commonClient.login();
                return true;
            } catch (Exception e) {
                log.debug("Failed to login to retrieve data from server. " + e.getMessage(), e);
                commonClient = null;
            }
        }
        return false;
    }

    private LinkedHashMap<String, String> convertPresetToMap(List<Preset> oldType) {
        LinkedHashMap<String, String> newType = new LinkedHashMap<String, String>();
        for (Preset preset : oldType) {
            newType.put(Long.toString(preset.getId()), preset.getName());
        }
        return newType;
    }

    private LinkedHashMap<String, String> convertTeamPathToMap(List<Team> oldType) {
        LinkedHashMap<String, String> newType = new LinkedHashMap<String, String>();
        for (Team team : oldType) {
            newType.put(team.getId(), team.getFullName());
        }
        return newType;
    }

    //validate configuration fields
    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection) {
    	super.validate(params, errorCollection);
        if (params.getBoolean(IS_INCREMENTAL) && params.getBoolean(FORCE_SCAN)) {
        	errorCollection.addError(FORCE_SCAN, ((ConfigureBuildTasks) errorCollection).getText("errorForceIncrementalScan.equals"));
        	errorCollection.addError(IS_INCREMENTAL, ((ConfigureBuildTasks) errorCollection).getText("errorForceIncrementalScan.equals"));
        }
        String useSpecific = params.getString(SERVER_CREDENTIALS_SECTION);
        boolean scaResolverEnabled =  params.getBoolean(CXSCA_RESOLVER_ENABLED);
        boolean scaResolverEnabledGlobal = params.getBoolean(CXSCA_RESOLVER_ENABLED_GLOBAL);
        boolean useGlobalSettings = params.getBoolean(CX_USE_CUSTOM_DEPENDENCY_SETTINGS);
        boolean enableDependancyScan = params.getBoolean(ENABLE_DEPENDENCY_SCAN);        
        if (CUSTOM_CONFIGURATION_SERVER.equals(useSpecific)) {
            validateNotEmpty(params, errorCollection, USER_NAME);
            validateNotEmpty(params, errorCollection, PASSWORD);
            validateNotEmpty(params, errorCollection, SERVER_URL);
            validateUrl(params, errorCollection, SERVER_URL);
        }
    
        validateNotEmpty(params, errorCollection, PROJECT_NAME);
        if(scaResolverEnabled && useGlobalSettings && enableDependancyScan){
            validateNotEmpty(params, errorCollection, CXSCA_RESOLVER_PATH);
        }
        if(scaResolverEnabledGlobal && !useGlobalSettings && enableDependancyScan){
            validateNotEmpty(params, errorCollection, CXSCA_RESOLVER_PATH_GLOBAL);
        }
        containsIllegals(params, errorCollection, PROJECT_NAME);
        validateProjectNameLength(params, errorCollection, PROJECT_NAME);

        if (params.getBoolean(IS_INCREMENTAL) && params.getBoolean(IS_INTERVALS)) {
            if (params.getString(INTERVAL_BEGINS).equals(params.getString(INTERVAL_ENDS))) {
                errorCollection.addError(INTERVAL_ENDS, ((ConfigureBuildTasks) errorCollection).getText("intervals.equals"));
            }
        }
        useSpecific = params.getString(CXSAST_SECTION);
        if (CUSTOM_CONFIGURATION_CXSAST.equals(useSpecific)) {
            validatePositive(params, errorCollection, SCAN_TIMEOUT_IN_MINUTES);
        }

        useSpecific = params.getString(SCAN_CONTROL_SECTION);
        if (CUSTOM_CONFIGURATION_CONTROL.equals(useSpecific)) {
            validateNotNegative(params, errorCollection, CRITICAL_THRESHOLD);
            validateCriticalSupport(params, errorCollection, CRITICAL_THRESHOLD);
            validateNotNegative(params, errorCollection, HIGH_THRESHOLD);
            validateNotNegative(params, errorCollection, MEDIUM_THRESHOLD);
            validateNotNegative(params, errorCollection, LOW_THRESHOLD);
            validateNotNegative(params, errorCollection, OSA_CRITICAL_THRESHOLD);
            validateNotNegative(params, errorCollection, OSA_HIGH_THRESHOLD);
            validateNotNegative(params, errorCollection, OSA_MEDIUM_THRESHOLD);
            validateNotNegative(params, errorCollection, OSA_LOW_THRESHOLD);
        }
        if (errorCollection.hasAnyErrors()) {
            errorCollection.addError(ERROR_OCCURRED, ERROR_OCCURRED_MESSAGE);
        }
        
    }

    private void validateNotEmpty(@NotNull ActionParametersMap params, @NotNull final ErrorCollection errorCollection, @NotNull String key) {
        final String value = params.getString(key);
        if (StringUtils.isEmpty(value)) {
            errorCollection.addError(key, ((ConfigureBuildTasks) errorCollection).getText(key + ".error"));
        }
    }

    private void validateUrl(@NotNull ActionParametersMap params, @NotNull final ErrorCollection errorCollection, @NotNull String key) {
        final String value = params.getString(key);
        if (!StringUtils.isEmpty(value)) {
            try {
                URL url = new URL(value);
                if (url.getPath().length() > 0) {
                    errorCollection.addError(key, ((ConfigureBuildTasks) errorCollection).getText(key + ".error.malformed"));
                }
            } catch (MalformedURLException e) {
                errorCollection.addError(key, ((ConfigureBuildTasks) errorCollection).getText(key + ".error.malformed"));
            }
        }
    }

    private void containsIllegals(@NotNull ActionParametersMap params, @NotNull final ErrorCollection errorCollection, @NotNull String key) {
        String toExamine = params.getString(key);
        Pattern pattern = Pattern.compile("[/?<>\\:*|\"]");
        if (toExamine != null) {
            Matcher matcher = pattern.matcher(toExamine);

            if (matcher.find()) {
                errorCollection.addError(key, ((ConfigureBuildTasks) errorCollection).getText(key + ".containsIllegals"));
            }
        }
    }

    private void validateProjectNameLength(@NotNull ActionParametersMap params, @NotNull final ErrorCollection errorCollection, @NotNull String key) {
        String toExamine = params.getString(key);
        if (toExamine != null && toExamine.length() > MAX_PROJECT_NAME_LENGTH) {
            errorCollection.addError(key, ((ConfigureBuildTasks) errorCollection).getText(key + ".invalidLength"));
        }
    }

    private void validatePositive(@NotNull ActionParametersMap params, @NotNull final ErrorCollection errorCollection, @NotNull String key) {
        final String value = params.getString(key);
        if (!StringUtils.isEmpty(value)) {
            try {
                int num = Integer.parseInt(value.trim());
                if (num > 0) {
                    return;
                }
                errorCollection.addError(key, ((ConfigureBuildTasks) errorCollection).getText(key + ".notPositive"));
            } catch (Exception e) {
                errorCollection.addError(key, ((ConfigureBuildTasks) errorCollection).getText(key + ".notPositive"));
            }
        }
    }

    private void validateNotNegative(@NotNull ActionParametersMap params, @NotNull final ErrorCollection errorCollection, @NotNull String key) {
        final String value = params.getString(key);
        if (!StringUtils.isEmpty(value)) {
            try {
                int num = Integer.parseInt(value);
                if (num >= 0) {
                    return;
                }
                errorCollection.addError(key, ((ConfigureBuildTasks) errorCollection).getText(key + ".notPositive"));
            } catch (Exception e) {
                errorCollection.addError(key, ((ConfigureBuildTasks) errorCollection).getText(key + ".notPositive"));
            }
        }
    }
    
    private void validateCriticalSupport(@NotNull ActionParametersMap params, @NotNull final ErrorCollection errorCollection, @NotNull String key) {
    	Double version = 9.0;
    	if(OPTION_TRUE.equalsIgnoreCase(params.getString(IS_SYNCHRONOUS)) 
    			&& OPTION_TRUE.equalsIgnoreCase(params.getString(THRESHOLDS_ENABLED))){        	
        		String cxServerUrl = "";
                String cxUser = "";
                String cxPass = "";
                String proxyEnable = "";
                if(GLOBAL_CONFIGURATION_SERVER.equalsIgnoreCase(params.getString(SERVER_CREDENTIALS_SECTION))) {//For global configuration case        			
        			cxServerUrl = getAdminConfig(GLOBAL_SERVER_URL);
        	        cxUser = getAdminConfig(GLOBAL_USER_NAME);
        	        cxPass = getAdminConfig(GLOBAL_PWD);
        	        proxyEnable = getAdminConfig(GLOBAL_ENABLE_PROXY);        			
        		}
        		else { // For use specific case
        			cxUser = params.getString(USER_NAME);
        			cxPass = params.getString(PASSWORD);
        			cxServerUrl = params.getString(SERVER_URL);
        			proxyEnable = params.getString(ENABLE_PROXY);        			
        		}
        		String sastVersion;
    			//fetch SAST version using api call
				try {
					sastVersion = SASTUtils.loginToServer(new URL(cxServerUrl),cxUser,decrypt(cxPass),proxyEnable);
					String[] sastVersionSplit = sastVersion.split("\\.");
					version = Double.parseDouble(sastVersionSplit[0]+"."+sastVersionSplit[1]);
				} catch (Exception e) {
					e.printStackTrace();
				}
				//check if SAST version support critical threshold
        		if(version >= 9.7) {
        			if(OPTION_FALSE.equalsIgnoreCase(params.getString(ENABLE_CRITICAL_SEVERITY))) {//This condition represents version of SAST is changed from SAST < 9.7 to SAST >=9.7
        				//following param will make critical threshold visible in UI
        				params.put(ENABLE_CRITICAL_SEVERITY, OPTION_TRUE);
        				//reset the value of critical threshold
        				params.put(CRITICAL_THRESHOLD,"");
            			errorCollection.addError(CRITICAL_THRESHOLD,"The configured SAST version supports Critical severity. Critical threshold can also be configured.");
            		} else {
        				//This condition represents version of SAST remains same as before i.e SAST >=9.7. Thus no change is needed.
            		}
        		}else {
        			if( OPTION_TRUE.equalsIgnoreCase(params.getString(ENABLE_CRITICAL_SEVERITY))){//This condition represents version of SAST is changed from SAST >= 9.7 to SAST < 9.7
        				//if SAST version is prior to 9.7 then do not show critical threshold on UI
        				params.put(ENABLE_CRITICAL_SEVERITY, OPTION_FALSE);
        				//reset the critical threshold value to empty
        				params.put(CRITICAL_THRESHOLD,"");
        				// following message is displayed on UI to make user aware of the critical threshold is not supported by this version of SAST.
        				errorCollection.addError(HIGH_THRESHOLD,"The configured SAST version does not support Critical severity. Critical threshold will not be applicable.");
        				}else {
        					//This condition represents version of SAST remains same as before i.e SAST < 9.7. Thus no change is needed.
        					}        			
        		}        		        		
        	        	
        }
    }

    private String getAdminConfig(String key) {
        if (adminConfig == null) {
            adminConfig = (AdministrationConfiguration) ContainerManager.getComponent(ADMINISTRATION_CONFIGURATION);
        }
        return StringUtils.defaultString(adminConfig.getSystemProperty(key));
    }

    private void populateIntervals(@NotNull final Map<String, Object> context) {

        intervalList.put("00:00", "12.00 am");
        intervalList.put("01:00", "01.00 am");
        intervalList.put("02:00", "02.00 am");
        intervalList.put("03:00", "03.00 am");
        intervalList.put("04:00", "04.00 am");
        intervalList.put("05:00", "05.00 am");
        intervalList.put("06:00", "06.00 am");
        intervalList.put("08:00", "08.00 am");
        intervalList.put("09:00", "09.00 am");
        intervalList.put("10:00", "10.00 am");
        intervalList.put("11:00", "11.00 am");
        intervalList.put("12:00", "12.00 pm");
        intervalList.put("13:00", "01.00 pm");
        intervalList.put("14:00", "02.00 pm");
        intervalList.put("15:00", "03.00 pm");
        intervalList.put("16:00", "04.00 pm");
        intervalList.put("17:00", "05.00 pm");
        intervalList.put("18:00", "06.00 pm");
        intervalList.put("19:00", "07.00 pm");
        intervalList.put("20:00", "08.00 pm");
        intervalList.put("21:00", "09.00 pm");
        intervalList.put("22:00", "10.00 pm");
        intervalList.put("23:00", "11.00 pm");

        context.put(INTERVAL_BEGINS_LIST, intervalList);
        context.put(INTERVAL_ENDS_LIST, intervalList);
    }

}
