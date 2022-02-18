package com.cx.plugin.utils;


import com.atlassian.bamboo.build.logger.BuildLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.impl.CxLoggerFactory;

/**
 * Created by: Dorg.
 * Date: 14/09/2016.
 */
public class CxLoggerAdapter extends MarkerIgnoringBase {

	private CxLoggerFactory logfactory ;
    private final Logger log;

    private BuildLogger buildLogger;

    public CxLoggerAdapter(BuildLogger logger) {
        this.name = "Build Logger";
        this.buildLogger = logger;
        logfactory =  new CxLoggerFactory(this.buildLogger);
        log = logfactory.getLogger("Checkmarx Build Logger");
    }

    public boolean isTraceEnabled() {
        return false;
    }

    public void trace(String s) {

    }

    public void trace(String s, Object o) {
    }

    public void trace(String s, Object o, Object o1) {
    }

    public void trace(String s, Object... objects) {
    }

    public void trace(String s, Throwable throwable) {
    }

    public boolean isDebugEnabled() {
        return true;
    }

    public void debug(String s) {
        log.debug(s);
    }

    public void debug(String s, Object o) {
        log.debug(s, o);
    }

    public void debug(String s, Object o, Object o1) {
        log.debug(s, o, o1);
    }

    public void debug(String s, Object... objects) {
        log.debug(s, objects);
    }

    public void debug(String s, Throwable throwable) {
        log.debug(s, throwable);
    }

    /****************************************************************/


    public boolean isInfoEnabled() {
        return true;
    }

    public void info(String s) {
        log.info(s);
    }

    public void info(String s, Object o) {
        log.info(s, o);
    }

    public void info(String s, Object o, Object o1) {
        log.info(s, o, o1);
    }

    public void info(String s, Object... objects) {
        log.info(s, objects);
    }

    public void info(String s, Throwable throwable) {
        log.info(s, throwable);
    }


    public boolean isWarnEnabled() {
        return true;
    }

    public void warn(String s) {
        log.warn(s);
    }

    public void warn(String s, Object o) {
        log.warn(s, o);
    }

    public void warn(String s, Object... objects) {
        log.warn(s, objects);
    }

    public void warn(String s, Object o, Object o1) {
        log.warn(s, o, o1);
    }

    public void warn(String s, Throwable throwable) {
        buildLogger.addBuildLogEntry(s);
        log.warn(s, throwable);
    }

    public boolean isErrorEnabled() {
        return true;
    }

    public void error(String s) {
        buildLogger.addErrorLogEntry(s);
        log.error(s);
    }

    public void error(String s, Object o) {
        log.error(s, o);
    }

    public void error(String s, Object o, Object o1) {
        log.error(s, o, o1);
    }

    public void error(String s, Object... objects) {
        log.error(s, objects);
    }

    public void error(String s, Throwable throwable) {
        log.error(s, throwable);

    }
}
