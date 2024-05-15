package com.cx.plugin.utils;

import static com.cx.plugin.utils.CxParam.CX_ORIGIN;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cx.plugin.configuration.CommonClientFactory;
import com.cx.plugin.testConnection.CxRestResource;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.ProxyConfig;
import com.cx.restclient.sast.utils.LegacyClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SASTUtils {
	private static Logger logger = LoggerFactory.getLogger(CxRestResource.class);

	public static String loginToServer(URL url, String username, String password, String proxyEnable) {
    	String version = null;
    	String result = "";
    	LegacyClient commonClient = null;
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
            version = commonClient.login(true);

            return version;
        } catch (Exception cxClientException) {
            logger.error("Fail to login: ", cxClientException);
            result = cxClientException.getMessage();
            return version;
        }
    }
	
	//fetch SAST version from response json of get version api
	public static String getMajorMinorSASTVersion(String version) {
		JsonNode node = null;
		try {
			node = new ObjectMapper().readTree(version);
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String cxVersion = node.path("version").asText();
		String[] sastVersionSplit = cxVersion.split("\\.");
		return sastVersionSplit[0]+"."+sastVersionSplit[1];
	}
	
       
}
