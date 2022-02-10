package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import com.atlassian.bamboo.build.logger.BuildLogger;

import java.util.HashMap;
import java.util.Map;


public class MyLoggerFactory implements ILoggerFactory {
    private Map<String, MyLoggerAdapter> loggerMap;
    private BuildLogger _logger = null;

    public MyLoggerFactory() {
        loggerMap = new HashMap<String, MyLoggerAdapter>();
    }

    public MyLoggerFactory(BuildLogger logger) {
        loggerMap = new HashMap<String, MyLoggerAdapter>();
        _logger = logger;
    }

    
    @Override
    public Logger getLogger(String name) {
        synchronized (loggerMap) {
            if (!loggerMap.containsKey(name)) {
                loggerMap.put(name, new MyLoggerAdapter(name, _logger));
            }

            return loggerMap.get(name);
        }
    }
}