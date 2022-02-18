package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import com.atlassian.bamboo.build.logger.BuildLogger;

import java.util.HashMap;
import java.util.Map;


public class CxLoggerFactory implements ILoggerFactory {
    private Map<String, CheckmarxLoggerAdapter> loggerMap;
    private BuildLogger _logger = null;

    public CxLoggerFactory() {
        loggerMap = new HashMap<String, CheckmarxLoggerAdapter>();
    }

    public CxLoggerFactory(BuildLogger logger) {
        loggerMap = new HashMap<String, CheckmarxLoggerAdapter>();
        _logger = logger;
    }

    
    @Override
    public Logger getLogger(String name) {
        synchronized (loggerMap) {
            if (!loggerMap.containsKey(name)) {
                loggerMap.put(name, new CheckmarxLoggerAdapter(name, _logger));
            }

            return loggerMap.get(name);
        }
    }
}