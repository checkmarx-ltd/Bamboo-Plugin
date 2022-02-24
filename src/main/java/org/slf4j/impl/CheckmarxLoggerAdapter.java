package org.slf4j.impl;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.FormattingTuple;

import com.atlassian.bamboo.build.logger.BuildLogger;

public class CheckmarxLoggerAdapter implements Logger, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private BuildLogger logger;
	protected boolean infoCapable = true;
	protected boolean errorCapable = true;
	protected boolean warnCapable = true;
	protected boolean debugCapable = false;
	protected boolean traceCapable = false;
	
	protected final String FQCN = getClass().getName();
	 
	CheckmarxLoggerAdapter(String name){
		this.name = name;
		setLogLevel();
	}
	
	CheckmarxLoggerAdapter(String name, BuildLogger logger){
		this.name = name;
		this.logger = logger;
		setLogLevel();
		 
	}

	protected void setLogLevel() {
		String level  = System.getProperty("cx.bamboo.log_level");
		if(!StringUtils.isEmpty(level)) {
			if(level.equalsIgnoreCase("DEBUG"))
				debugCapable = true;
			else if(level.equalsIgnoreCase("TRACE")) {
				debugCapable = true;
				traceCapable = true;
			}
		}
	}
	
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isTraceEnabled() {
		if(logger == null)
			return false;
			else
		 return traceCapable;
	}

	@Override
	public void trace(String msg) {
		if(!isTraceEnabled())
			return;
		
		logger.addBuildLogEntry(msg);		
	}

	@Override
	public void trace(String format, Object arg) {
		if(!isTraceEnabled())
			return;
		FormattingTuple ft = MessageFormatter.format(format,arg);
		logger.addBuildLogEntry(ft.getMessage());		
	}

	@Override
	public void trace(String format, Object arg1, Object arg2) {
		if(!isTraceEnabled())
			return;
		FormattingTuple ft = MessageFormatter.format(format,arg1,arg2);
		logger.addBuildLogEntry(ft.getMessage());			
	}

	@Override
	public void trace(String format, Object[] argArray) {
		if(!isTraceEnabled())
			return ;
		FormattingTuple ft = MessageFormatter.format(format,argArray);
		logger.addBuildLogEntry(ft.getMessage());	
	}

	@Override
	public void trace(String msg, Throwable t) {
		if(!isTraceEnabled())
			return ;
		logger.addErrorLogEntry(msg, t);
	}

	@Override
	public boolean isTraceEnabled(Marker marker) {
		return this.isTraceEnabled();
	}

	@Override
	public void trace(Marker marker, String msg) {			
		
	}

	@Override
	public void trace(Marker marker, String format, Object arg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trace(Marker marker, String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trace(Marker marker, String format, Object[] argArray) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trace(Marker marker, String msg, Throwable t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isDebugEnabled() {
		if(logger == null)
			return false;
			else
		return debugCapable;
	}

	@Override
	public void debug(String msg) {
		if(!isDebugEnabled())
			return ;
		logger.addBuildLogEntry(msg);		
	}

	@Override
	public void debug(String format, Object arg) {
		if(!isDebugEnabled())
			return ;
		FormattingTuple ft = MessageFormatter.format(format,arg);
		logger.addBuildLogEntry(ft.getMessage());	
	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		if(!isDebugEnabled())
			return ;
		FormattingTuple ft = MessageFormatter.format(format,arg1,arg2);
		logger.addBuildLogEntry(ft.getMessage());			
	}

	@Override
	public void debug(String format, Object[] argArray) {
		if(!isDebugEnabled())
			return ;
		FormattingTuple ft = MessageFormatter.format(format,argArray);
		logger.addBuildLogEntry(ft.getMessage());			
	}

	@Override
	public void debug(String msg, Throwable t) {
		if(!isDebugEnabled())
			return ;
		logger.addErrorLogEntry(msg, t);
	}

	@Override
	public boolean isDebugEnabled(Marker marker) {
		return this.isDebugEnabled();
	}

	@Override
	public void debug(Marker marker, String msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void debug(Marker marker, String format, Object arg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void debug(Marker marker, String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void debug(Marker marker, String format, Object[] argArray) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void debug(Marker marker, String msg, Throwable t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isInfoEnabled() {
		if(logger == null)
			return false;
			else
		return infoCapable;
	}

	@Override
	public void info(String msg) {
		if(logger == null)
			return ;
		logger.addBuildLogEntry(msg);		
	}

	@Override
	public void info(String format, Object arg) {
		if(logger == null)
			return ;
		FormattingTuple ft = MessageFormatter.format(format,arg);
		logger.addBuildLogEntry(ft.getMessage());		
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		if(logger == null)
			return ;
		FormattingTuple ft = MessageFormatter.format(format,arg1,arg2);
		logger.addBuildLogEntry(ft.getMessage());		
	}

	@Override
	public void info(String format, Object[] argArray) {
		if(logger == null)
			return ;
		FormattingTuple ft = MessageFormatter.format(format,argArray);
		logger.addBuildLogEntry(ft.getMessage());			
	}

	@Override
	public void info(String msg, Throwable t) {
		if(logger == null)
			return ;
		logger.addErrorLogEntry(msg, t);
	}

	@Override
	public boolean isInfoEnabled(Marker marker) {
		return this.isInfoEnabled();
	}

	@Override
	public void info(Marker marker, String msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(Marker marker, String format, Object arg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(Marker marker, String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(Marker marker, String format, Object[] argArray) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(Marker marker, String msg, Throwable t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isWarnEnabled() {
		if(logger == null)
			return false;
		else
		return warnCapable;
	}

	@Override
	public void warn(String msg) {
		if(logger == null)
			return ;
		logger.addBuildLogEntry(msg);		
	}

	@Override
	public void warn(String format, Object arg) {
		if(logger == null)
			return ;
		FormattingTuple ft = MessageFormatter.format(format,arg);
		logger.addBuildLogEntry(ft.getMessage());		
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		if(logger == null)
			return ;
		FormattingTuple ft = MessageFormatter.format(format,arg1,arg2);
		logger.addBuildLogEntry(ft.getMessage());			
	}

	@Override
	public void warn(String format, Object[] argArray) {
		if(logger == null)
			return ;
		FormattingTuple ft = MessageFormatter.format(format,argArray);
		logger.addBuildLogEntry(ft.getMessage());		
	}

	@Override
	public void warn(String msg, Throwable t) {
		if(logger == null)
			return ;
		logger.addErrorLogEntry(msg, t);
	}

	@Override
	public boolean isWarnEnabled(Marker marker) {
		return this.isWarnEnabled();
	}

	@Override
	public void warn(Marker marker, String msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void warn(Marker marker, String format, Object arg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void warn(Marker marker, String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void warn(Marker marker, String format, Object[] argArray) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void warn(Marker marker, String msg, Throwable t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isErrorEnabled() {
		if(logger == null)
			return false;
			else
		 return errorCapable;
	}

	

	@Override
	public boolean isErrorEnabled(Marker marker) {
		return this.isErrorEnabled();
	}

	@Override
	public void error(Marker marker, String msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void error(Marker marker, String format, Object arg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void error(Marker marker, String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void error(Marker marker, String format, Object[] argArray) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void error(Marker marker, String msg, Throwable t) {
		// TODO Auto-generated method stub
		
	}
    // Bunch of inherited methods here. Let your IDE generate this.
    // Implement these methods to do your own logging.

	@Override
	public void error(String msg) {
		if(logger == null)
			return ;
		logger.addBuildLogEntry(msg);		
	}

	@Override
	public void error(String format, Object arg) {
		if(logger == null)
			return ;
		FormattingTuple ft = MessageFormatter.format(format,arg);
		logger.addBuildLogEntry(ft.getMessage());		
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		if(logger == null)
			return ;
		FormattingTuple ft = MessageFormatter.format(format,arg1,arg2);
		logger.addBuildLogEntry(ft.getMessage());		
	}

	@Override
	public void error(String format, Object[] argArray) {
		if(logger == null)
			return ;
		FormattingTuple ft = MessageFormatter.format(format,argArray);
		logger.addBuildLogEntry(ft.getMessage());			
	}

	@Override
	public void error(String msg, Throwable t) {
		if(logger == null)
			return ;
		logger.addErrorLogEntry(msg, t);
	}
}
