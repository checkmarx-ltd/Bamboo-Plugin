package com.cx.plugin.utils;


import com.atlassian.bamboo.build.logger.BuildLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

/**
 * Created by: Dorg.
 * Date: 14/09/2016.
 */
public class CxLoggerAdapter extends MarkerIgnoringBase {

    private final Logger log = LoggerFactory.getLogger("Checkmarx Build Logger");

    private BuildLogger buildLogger;

    public CxLoggerAdapter(BuildLogger log) {
        this.name = "Build Logger";
        this.buildLogger = log;
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
        buildLogger.addBuildLogEntry(s);
        
    }

    public void debug(String s, Object o) {
        FormattingTuple ft = MessageFormatter.format(s, o);
        buildLogger.addBuildLogEntry(ft.getMessage());
        
    }

    public void debug(String s, Object o, Object o1) {
        FormattingTuple ft = MessageFormatter.format(s, o, o1);
        buildLogger.addBuildLogEntry(ft.getMessage());
        
    }

    public void debug(String s, Object... objects) {
        FormattingTuple ft = MessageFormatter.format(s, objects);
        buildLogger.addBuildLogEntry(ft.getMessage());
        
    }

    public void debug(String s, Throwable throwable) {
        buildLogger.addBuildLogEntry(s);
        
    }

    /****************************************************************/


    public boolean isInfoEnabled() {
        return true;
    }

    public void info(String s) {
        buildLogger.addBuildLogEntry(s);
        
    }

    public void info(String s, Object o) {
        FormattingTuple ft = MessageFormatter.format(s, o);
        buildLogger.addBuildLogEntry(ft.getMessage());
        
    }

    public void info(String s, Object o, Object o1) {
        FormattingTuple ft = MessageFormatter.format(s, o, o1);
        buildLogger.addBuildLogEntry(ft.getMessage());
        
    }

    public void info(String s, Object... objects) {
        FormattingTuple ft = MessageFormatter.format(s, objects);
        buildLogger.addBuildLogEntry(ft.getMessage());
        
    }

    public void info(String s, Throwable throwable) {
        buildLogger.addBuildLogEntry(s);
        
    }


    public boolean isWarnEnabled() {
        return true;
    }

    public void warn(String s) {
        buildLogger.addBuildLogEntry(s);
        
    }

    public void warn(String s, Object o) {
        FormattingTuple ft = MessageFormatter.format(s, o);
        buildLogger.addBuildLogEntry(ft.getMessage());
        
    }

    public void warn(String s, Object... objects) {
        FormattingTuple ft = MessageFormatter.format(s, objects);
        buildLogger.addBuildLogEntry(ft.getMessage());
        
    }

    public void warn(String s, Object o, Object o1) {
        FormattingTuple ft = MessageFormatter.format(s, o, o1);
        buildLogger.addBuildLogEntry(ft.getMessage());
        
    }

    public void warn(String s, Throwable throwable) {
        buildLogger.addBuildLogEntry(s);
        
    }

    public boolean isErrorEnabled() {
        return true;
    }

    public void error(String s) {
        buildLogger.addErrorLogEntry(s);

    }

    public void error(String s, Object o) {
        FormattingTuple ft = MessageFormatter.format(s, o);
        buildLogger.addErrorLogEntry(ft.getMessage(), ft.getThrowable());

    }

    public void error(String s, Object o, Object o1) {
        FormattingTuple ft = MessageFormatter.format(s, o, o1);
        buildLogger.addErrorLogEntry(ft.getMessage(), ft.getThrowable());

    }

    public void error(String s, Object... objects) {
        FormattingTuple ft = MessageFormatter.format(s, objects);
        buildLogger.addErrorLogEntry(ft.getMessage(), ft.getThrowable());

    }

    public void error(String s, Throwable throwable) {
        buildLogger.addErrorLogEntry(s, throwable);


    }
}
