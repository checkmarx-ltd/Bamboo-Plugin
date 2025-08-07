package com.cx.plugin.testConnection;


import static com.cx.plugin.utils.CxParam.CONNECTION_FAILED_COMPATIBILITY;
import static com.cx.plugin.utils.CxParam.CX_ORIGIN;
import static com.cx.plugin.utils.CxParam.NO_PRESET_ID;
import static com.cx.plugin.utils.CxParam.NO_PRESET_MESSAGE;
import static com.cx.plugin.utils.CxParam.NO_TEAM_MESSAGE;
import static com.cx.plugin.utils.CxParam.NO_TEAM_PATH;
import static com.cx.plugin.utils.CxParam.OPTION_TRUE;
import static com.cx.plugin.utils.CxPluginUtils.decrypt;
import static com.cx.restclient.common.CxPARAM.CX_PLUGIN_VERSION;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.SystemUtils;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cx.plugin.configuration.CommonClientFactory;
import com.cx.plugin.testConnection.dto.TestConnectionResponse;
import com.cx.plugin.testConnection.dto.TestScaConnectionResponse;
import com.cx.plugin.utils.CxParam;
import com.cx.plugin.utils.HttpHelper;
import com.cx.restclient.CxClientDelegator;
import com.cx.restclient.ast.dto.sca.AstScaConfig;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.ProxyConfig;
import com.cx.restclient.dto.ScannerType;
import com.cx.restclient.dto.SourceLocationType;
import com.cx.restclient.dto.Team;
import com.cx.restclient.exception.CxClientException;
import com.cx.restclient.sast.dto.Preset;
import com.cx.restclient.sast.utils.LegacyClient;

/**
 * A resource of message.
 */
@Path("/")
public class CxRestResource {

    private List<Preset> presets;
    private List<Team> teams;
    private LegacyClient commonClient;
    private String result = "";
    private Logger logger = LoggerFactory.getLogger(CxRestResource.class);


    @POST
    @Path("test/connection")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public Response testConnection(Map<Object, Object> data) {

    	TestConnectionResponse tcResponse;
        result = "";
        URL url;
        String urlToCheck;
        int statusCode = 400;

        String username = StringUtils.defaultString(data.get("username"));
        String pas = StringUtils.defaultString(data.get("pas"));
        String proxyEnable = StringUtils.defaultString(data.get("proxyEnable"));

        try {
            urlToCheck = StringUtils.defaultString(data.get("url"));
            url = new URL(urlToCheck);
            String version = loginToServer(url, username, decrypt(pas), proxyEnable);
        	if (version!=null) {
                try {
                    teams = commonClient.getTeamList();
                } catch (Exception e) {
                    throw new Exception(CONNECTION_FAILED_COMPATIBILITY + "\nError: " + e.getMessage());
                }
                presets = commonClient.getPresetList();
                if (presets == null || teams == null) {
                    throw new Exception("invalid preset teamPath");
                }
                result = "Connection successful";
                tcResponse = new TestConnectionResponse(result, presets, teams);
                tcResponse.setCxVersion(version);
                statusCode = 200;

            } else {
                result = result.contains("Failed to authenticate") ? "Failed to authenticate" : result;
                result = result.startsWith("Login failed.") ? result : "Login failed. " + result;
                tcResponse = getTCFailedResponse();
            }
        } catch (Exception e) {
            result = "Fail to login: " + e.getMessage();
            tcResponse = getTCFailedResponse();
        }
        return Response.status(statusCode).entity(tcResponse).build();
    }
    
    @POST
    @Path("test/scaconnection")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public Response testScaConnection(Map<Object, Object> data) {
    	TestScaConnectionResponse tcResponse;
    	 int statusCode = 400;
			try {

				String scaAccessControlUrl = StringUtils.defaultString(data.get("scaAccessControlUrl"));
				String scaServerUrl = StringUtils.defaultString(data.get("scaServerUrl"));
				String scaWebAppUrl = StringUtils.defaultString(data.get("scaWebAppUrl"));
				String scaTenant = StringUtils.defaultString(data.get("scaAccountName"));
				String username = StringUtils.defaultString(data.get("scaUserName"));
				String pss = decrypt(StringUtils.defaultString(data.get("pss")));
				String proxyEnable = StringUtils.defaultString(data.get("proxyEnable"));
				
				CxScanConfig config = new CxScanConfig();
				config.setDisableCertificateValidation(true);
				config.setOsaGenerateJsonReport(false);

				ProxyConfig proxyConfig = HttpHelper.getProxyConfig();
				if (proxyEnable != null && proxyEnable.equalsIgnoreCase("true") && proxyConfig != null) {
					config.setProxy(true);
					config.setProxyConfig(proxyConfig);
		            logger.debug("Testing login with proxy details:");
					logger.debug("Proxy host: " + proxyConfig.getHost());
					logger.debug("Proxy port: " + proxyConfig.getPort());
					logger.debug("Proxy user: " + proxyConfig.getUsername());
					logger.debug("Proxy password: *************");
					logger.debug("Proxy Scheme: " + (proxyConfig.isUseHttps() ? "https" : "http"));
					logger.debug("Non Proxy Hosts: " + proxyConfig.getNoproxyHosts());
				}else {
					config.setProxy(false);
		            logger.debug("Testing login.");
				}
				
				AstScaConfig scaConfig = new AstScaConfig();
				scaConfig.setAccessControlUrl(scaAccessControlUrl);
				scaConfig.setWebAppUrl(scaWebAppUrl);
				scaConfig.setApiUrl(scaServerUrl);
				scaConfig.setTenant(scaTenant);
				scaConfig.setUsername(username);
				scaConfig.setPassword(pss);
				scaConfig.setSourceLocationType(SourceLocationType.LOCAL_DIRECTORY);
				scaConfig.setRemoteRepositoryInfo(null);
				config.setAstScaConfig(scaConfig);
				config.addScannerType(ScannerType.AST_SCA);

				CxClientDelegator commonClient = CommonClientFactory.getClientDelegatorInstance(config, logger);
				commonClient.getScaClient().testScaConnection();
				tcResponse =  new TestScaConnectionResponse("Connection successful.");
				statusCode=200;
			} catch (Exception e) {				
				tcResponse =  new TestScaConnectionResponse("Failed to login: " + e.getMessage());
			}
    	return Response.status(statusCode).entity(tcResponse).build();
    }

    private Response getInvalidUrlResponse(int statusCode) {
        TestConnectionResponse tcResponse;
        result = "Invalid URL";
        tcResponse = new TestConnectionResponse(result, null, null);
        return Response.status(statusCode).entity(tcResponse).build();
    }

    @NotNull
    private TestConnectionResponse getTCFailedResponse() {
        presets = new ArrayList<Preset>() {{
            new Preset(NO_PRESET_ID, NO_PRESET_MESSAGE);
        }};
        teams = new ArrayList<Team>() {{
            new Team(NO_TEAM_PATH, NO_TEAM_MESSAGE);
        }};

        return new TestConnectionResponse(result, presets, teams);
    }

    private String loginToServer(URL url, String username, String password, String proxyEnable) {
    	String version = null;
        try {
            
        	ProxyConfig proxyConfig = HttpHelper.getProxyConfig();        	
			CxScanConfig scanConfig = new CxScanConfig(url.toString().trim(), username, password, CX_ORIGIN, true);
			if (proxyEnable != null && proxyEnable.equalsIgnoreCase("true") && proxyConfig != null) {
				scanConfig.setProxy(true);
				scanConfig.setProxyConfig(proxyConfig);
	            logger.debug("Testing login with proxy details:");
				logger.debug("Proxy host: " + proxyConfig.getHost());
				logger.debug("Proxy port: " + proxyConfig.getPort());
				logger.debug("Proxy user: " + proxyConfig.getUsername());
				logger.debug("Proxy password: *************");
				logger.debug("Proxy Scheme: " + (proxyConfig.isUseHttps() ? "https" : "http"));
				logger.debug("Non Proxy Hosts: " + proxyConfig.getNoproxyHosts());				
			}else {
				scanConfig.setProxy(false);
	            logger.debug("Testing login.");
			}
			
            commonClient = CommonClientFactory.getInstance(scanConfig, logger);
            
            String pluginVersion = "";
            try {
                Properties properties = new Properties();
                java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("english.properties");
                if (is != null) {
                    properties.load(is);
                    pluginVersion = properties.getProperty("version");
                }
            } catch (Exception e) {

            }
            System.setProperty(CxParam.CX_PLUGIN_VERSION, pluginVersion);
            version = commonClient.login(true);
            return version;
        } catch (Exception cxClientException) {
            logger.error("Fail to login: ", cxClientException);
            result = cxClientException.getMessage();
            return version;
        }
    }
}