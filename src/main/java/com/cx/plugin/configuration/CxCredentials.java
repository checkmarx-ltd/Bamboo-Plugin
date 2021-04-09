package com.cx.plugin.configuration;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.atlassian.bamboo.credentials.UsernamePasswordCredentials;
import com.cx.restclient.common.ErrorMessage;


//resolve between global or specific and username+pssd or credential manager
public class CxCredentials {

    private String serverUrl;
    private String username;
    private String encryptedPassword;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return encryptedPassword;
    }

    public void setPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    @NotNull
    private static CxCredentials getCxCredentials(String username, String pssd, String credId, CxCredentials ret, UsernamePasswordCredentials credentialsById) {
        if (StringUtils.isNotEmpty(credId)) {
            UsernamePasswordCredentials c = credentialsById;
            ret.setUsername(c != null ? c.getUsername() : "");
            ret.setPassword(c != null ? c.getPassword() : "");
            return ret;

        } else {
            ret.setUsername(StringUtils.defaultString(username));
            ret.setPassword(pssd);
            return ret;
        }
    }


    public static void validateCxCredentials(CxCredentials credentials) throws CxCredException {
        if(StringUtils.isEmpty(credentials.getServerUrl()) ||
                StringUtils.isEmpty(credentials.getUsername()) ||
                StringUtils.isEmpty((credentials.getPassword()))){
            throw new CxCredException(ErrorMessage.CHECKMARX_SERVER_CONNECTION_FAILED.getErrorMessage());
        }
    }
}