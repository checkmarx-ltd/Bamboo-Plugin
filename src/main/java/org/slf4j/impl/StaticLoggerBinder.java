package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

import com.atlassian.bamboo.build.logger.BuildLogger;


public class StaticLoggerBinder implements LoggerFactoryBinder {

    /**
     * The unique instance of this class.
     */
    private static StaticLoggerBinder SINGLETON
        = null;

    private BuildLogger _logger = null;
    
    private StaticLoggerBinder(BuildLogger logger) {
    	_logger = logger;
    	loggerFactory = new CxLoggerFactory(logger);
    }
    

    /**
     * Return the singleton of this class.
     *
     * @return the StaticLoggerBinder singleton
     */
    public static final StaticLoggerBinder getSingleton() {
    	if(SINGLETON == null) {
    		SINGLETON = new StaticLoggerBinder(null);
    	}
        return SINGLETON;
    }
    
    /**
     * Return the singleton of this class.
     *
     * @return the StaticLoggerBinder singleton
     */
    public static final StaticLoggerBinder getSingleton(BuildLogger logger) {
    	if(SINGLETON == null) {
    		SINGLETON = new StaticLoggerBinder(logger);
    	}
        return SINGLETON;
    }


    /**
     * Declare the version of the SLF4J API this implementation is
     * compiled against. The value of this field is usually modified
     * with each release.
     */
    // To avoid constant folding by the compiler,
    // this field must *not* be final
    public static String REQUESTED_API_VERSION = "1.6.1";  // !final

    private static final String loggerFactoryClassStr
        = CxLoggerFactory.class.getName();

    /**
     * The ILoggerFactory instance returned by the
     * {@link #getLoggerFactory} method should always be the same
     * object.
     */
    private final ILoggerFactory loggerFactory;

    private StaticLoggerBinder() {
        loggerFactory = new CxLoggerFactory();
    }

    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    public String getLoggerFactoryClassStr() {
        return loggerFactoryClassStr;
    }
}