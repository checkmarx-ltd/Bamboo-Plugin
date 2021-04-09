package com.cx.plugin.configuration;

import com.cx.restclient.dto.ProxyConfig;

class ProxyHelper {
    /**
     * Gets proxy settings defined globally for current bamboo instance.
     *
     * @return bamboo proxy settings converted to an internal object.
     */
    static ProxyConfig getProxyConfig() {
        ProxyConfig internalProxy = new ProxyConfig();
        // TODO : SUBHADRA : TO implement proxy added this code- Later uncomment and fix the compilation issues
        
        /*Jenkins instance = Jenkins.getInstance();

        // getInstance() is marked as @Nonnull, but it can still return null if we happen to execute this code
        // in a Jenkins agent.
        // noinspection ConstantConditions
        if (instance != null && instance.proxy != null) {
            ProxyConfiguration bambooProxy = instance.proxy;
            internalProxy.setHost(bambooProxy.name);
            internalProxy.setPort(bambooProxy.port);
            internalProxy.setUsername(bambooProxy.getUserName());
            internalProxy.setPassword(bambooProxy.getPassword());
        }*/
        return internalProxy;
    }
}
