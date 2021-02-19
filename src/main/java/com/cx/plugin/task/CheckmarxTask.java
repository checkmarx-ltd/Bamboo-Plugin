package com.cx.plugin.task;

import static com.cx.plugin.utils.CxParam.CONNECTION_FAILED_COMPATIBILITY;
import static com.cx.plugin.utils.CxParam.HTML_REPORT;
import static com.cx.plugin.utils.CxPluginUtils.printBuildFailure;
import static com.cx.plugin.utils.CxPluginUtils.printConfiguration;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Created by galn on 18/12/2016.
 */
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.cx.plugin.configuration.CommonClientFactory;
import com.cx.plugin.dto.BambooScanResults;
import com.cx.plugin.utils.CxAppender;
import com.cx.plugin.utils.CxConfigHelper;
import com.cx.plugin.utils.CxLoggerAdapter;
import com.cx.restclient.CxClientDelegator;
import com.cx.restclient.common.summary.SummaryUtils;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.ScanResults;
import com.cx.restclient.dto.ScannerType;
import com.cx.restclient.dto.scansummary.ScanSummary;
import com.cx.restclient.exception.CxClientException;
import com.cx.restclient.sast.utils.LegacyClient;


public class CheckmarxTask implements TaskType {

    @NotNull
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException {
        CxLoggerAdapter log;
        LegacyClient commonClient = null;
        boolean sastCreated = false;
        boolean osaCreated = false;
        BuildContext buildContext = taskContext.getBuildContext();
        final TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(taskContext);
        log = new CxLoggerAdapter(taskContext.getBuildLogger());
        try {
            Map<String, VariableDefinitionContext> effectiveVariables = taskContext.getBuildContext().getVariableContext().getEffectiveVariables();
            for (Map.Entry<String, VariableDefinitionContext> entry : effectiveVariables.entrySet()) {
                if (entry.getKey().contains("CX_MAVEN_PATH") ||
                        entry.getKey().contains("CX_GRADLE_PATH") ||
                        entry.getKey().contains("CX_NPM_PATH") ||
                        entry.getKey().contains("CX_COMPOSER_PATH")) {
                    if (StringUtils.isNotEmpty(entry.getValue().getValue())) {
                        System.setProperty(entry.getKey(), entry.getValue().getValue());
                    }
                }
            }

            //resolve configuration
            CxConfigHelper configHelper = new CxConfigHelper(log);
            CxScanConfig config = configHelper.resolveConfigurationMap(taskContext.getConfigurationMap(), taskContext.getWorkingDirectory());
            CxClientDelegator delegator = CommonClientFactory.getClientDelegatorInstance(config, log);

            //print configuration
            printConfiguration(config, configHelper, log);

            if (!config.isSastEnabled() && !config.isOsaEnabled()) {
                log.error("Both SAST and OSA are disabled. exiting");
                //TODO run.setResult(Result.FAILURE);
            }
            //create scans and retrieve results (in jenkins agent)
            BambooScanResults ret = new BambooScanResults();
            //initialize cx client
            try {
                ScanResults initResults = delegator.init();
                String summaryStr = delegator.generateHTMLSummary(initResults);
                ret.getSummary().put(HTML_REPORT, summaryStr);
                
            } catch (Exception ex) {
                if (ex.getMessage().contains("Server is unavailable")) {
                    try {
                        commonClient.login();
                    } catch (CxClientException e) {
                        throw new TaskException(e.getMessage());
                    }
                    throw new TaskException(CONNECTION_FAILED_COMPATIBILITY);
                }
                ret.setGeneralException(ex);
                throw new TaskException(ex.getMessage());
            }
            ScannerType scannerType = null;
            
            //Create OSA scan
            if (config.isOsaEnabled()) {
                //---------------------------
                //we do this in order to redirect the logs from the filesystem agent component to the build console
                String appenderName = "cxAppender_" + buildContext.getBuildKey().getKey();
                Logger.getRootLogger().addAppender(new CxAppender(taskContext.getBuildLogger(), appenderName));
                //---------------------------
                try {
                    scannerType = ScannerType.OSA;
                    osaCreated = true;
                } catch (CxClientException e) {
                    log.error(e.getMessage());
                } finally {
                    Logger.getRootLogger().removeAppender(appenderName);
                }
            }

            //Create SAST scan
            if (config.isSastEnabled()) {
                try {
                    scannerType = ScannerType.AST_SAST;
                    sastCreated = true;
                } catch (CxClientException e) {
                    log.error(e.getMessage());
                }
            }
            if (scannerType != null) {
                config.addScannerType(scannerType);
            }
            ScanResults createScanResults = delegator.initiateScan();
            String summaryStr = delegator.generateHTMLSummary(createScanResults);
            ret.getSummary().put(HTML_REPORT,summaryStr);
            //Asynchronous MODE
            if (!config.getSynchronous()) {
                log.info("Running in Asynchronous mode. Not waiting for scan to finish");
                String scanSummary = delegator.generateHTMLSummary(createScanResults);
                ret.getSummary().put(HTML_REPORT, scanSummary);

                if (ret.getException() != null || ret.getGeneralException() != null) {
                    printBuildFailure(null, ret, log);
                    return taskResultBuilder.failed().build();
                }

                return taskResultBuilder.success().build();
            }

            //Get SAST results
           /* if (sastCreated) {
                try {
                    SASTResults sastResults = createScanResults.waitForSASTResults();
                    ret.setSastResults(sastResults);
                } catch (CxClientException | IOException e) {
                    ret.setSastWaitException(e);
                    log.error(e.getMessage());
                }
            }

            //Get OSA results
            if (osaCreated) {
                try {
                    OSAResults osaResults = commonClient.waitForOSAResults();
                    ret.setOsaResults(osaResults);
                } catch (CxClientException | IOException e) {
                    ret.setOsaWaitException(e);
                    log.error(e.getMessage());
                }
            }*/
            ScanResults scanResults = config.getSynchronous() ? delegator.waitForScanResults() : delegator.getLatestScanResults();
            String scanSummaryStr = SummaryUtils.generateSummary(ret.getSastResults(), ret.getOsaResults(), ret.getScaResults(), config);
            ret.getSummary().put(HTML_REPORT, scanSummaryStr);
            
            //TODO : CHECK AND UNCOMMENT IF CANCEL SCAN DOES NOT WORK AS EXPECTED FROM BAMBOO
            
            if (config.getSynchronous() && config.isSastEnabled() &&
                    ((createScanResults.getSastResults() != null && createScanResults.getSastResults().getException() != null && createScanResults.getSastResults().getScanId() > 0) || (scanResults.getSastResults() != null && scanResults.getSastResults().getException() != null))) {
                cancelScan(delegator);
            }
            
            if (config.getEnablePolicyViolations()) {
                delegator.printIsProjectViolated(scanResults);
            }
            if (!config.getHideResults()) {
            	String showSummaryStr = SummaryUtils.generateSummary(ret.getSastResults(), ret.getOsaResults(), ret.getScaResults(), config);
                ret.getSummary().put(HTML_REPORT, showSummaryStr);
                buildContext.getBuildResult().getCustomBuildData().putAll(ret.getSummary());
            }

            //assert if expected exception is thrown  OR when vulnerabilities under threshold OR when policy violated
            ScanSummary scanSummary = new ScanSummary(config, ret.getSastResults(), ret.getOsaResults(), ret.getScaResults());
            if (scanSummary.hasErrors() || ret.getGeneralException() != null ||
                    (ret.getSastResults() != null && ret.getSastResults().getException() != null) ||
                    (ret.getOsaResults() != null && ret.getOsaResults().getException() != null) ||
                    (ret.getScaResults() != null && ret.getScaResults().getException() != null)) {
            	printBuildFailure(scanSummary.toString(), ret, log);
            	return taskResultBuilder.failed().build();
            }
        
            ///////////////
            return taskResultBuilder.success().build();
        } catch (InterruptedException e) {
            log.error("Interrupted exception: " + e.getMessage(), e);
            if (commonClient != null && sastCreated) {
                log.error("Canceling scan on the Checkmarx server...");
//                cancelScan(delegator);
            }
            throw new TaskException(e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected exception: " + e.getMessage(), e);
            throw new TaskException(e.getMessage());
        } finally {
            if (commonClient != null) {
                commonClient.close();
            }
        }
    }

    private void cancelScan(CxClientDelegator delegator) {
        try {
            delegator.getSastClient().cancelSASTScan();
        } catch (Exception ignored) {
        }
    }

}