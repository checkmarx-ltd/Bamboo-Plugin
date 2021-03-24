package com.cx.plugin.task;

import static com.cx.plugin.utils.CxParam.CONNECTION_FAILED_COMPATIBILITY;
import static com.cx.plugin.utils.CxParam.HTML_REPORT;
import static com.cx.plugin.utils.CxPluginUtils.printBuildFailure;
import static com.cx.plugin.utils.CxPluginUtils.printConfiguration;

import java.util.ArrayList;
import java.util.List;
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

import com.atlassian.bamboo.Key;
import com.atlassian.bamboo.build.artifact.ArtifactManager;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContext;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContextImpl;
import com.atlassian.bamboo.plan.artifact.ArtifactPublishingResult;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.cx.plugin.configuration.CommonClientFactory;
import com.cx.plugin.dto.BambooScanResults;
import com.cx.plugin.utils.CxAppender;
import com.cx.plugin.utils.CxConfigHelper;
import com.cx.plugin.utils.CxLoggerAdapter;

import com.cx.restclient.CxClientDelegator;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.Results;
import com.cx.restclient.dto.ScanResults;
import com.cx.restclient.dto.ScannerType;
import com.cx.restclient.dto.scansummary.ScanSummary;
import com.cx.restclient.exception.CxClientException;
import com.cx.restclient.sast.utils.LegacyClient;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CheckmarxTask implements TaskType {
	
    private final ArtifactManager artifactManager;

    public CheckmarxTask(final ArtifactManager artifactManager) {
        this.artifactManager = artifactManager;
    }

    @NotNull
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException {
        CxLoggerAdapter log;
        LegacyClient commonClient = null;
        BuildContext buildContext = taskContext.getBuildContext();
        final TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(taskContext);
        log = new CxLoggerAdapter(taskContext.getBuildLogger());
        
        try {
            Map<String, VariableDefinitionContext> effectiveVariables = taskContext.getBuildContext().getVariableContext().getEffectiveVariables();
            for (Map.Entry<String, VariableDefinitionContext> entry : effectiveVariables.entrySet()) {
                if (entry.getKey().contains("CX_")) {
                    if (StringUtils.isNotEmpty(entry.getValue().getValue())) {
                        System.setProperty(entry.getKey(), entry.getValue().getValue());
                    }
                }
            }
            //resolve configuration
            CxConfigHelper configHelper = new CxConfigHelper(log);
            CxScanConfig config = configHelper.resolveConfigurationMap(taskContext.getConfigurationMap(), taskContext.getWorkingDirectory(), taskContext);
            CxClientDelegator delegator = CommonClientFactory.getClientDelegatorInstance(config, log);

            //print configuration
            printConfiguration(config, configHelper, log);

            if (!config.isSastEnabled() && !config.isOsaEnabled()) {
                log.error("Both SAST and OSA are disabled. exiting");
                taskResultBuilder.failed().build();
            }
            //create scans and retrieve results (in jenkins agent)
            BambooScanResults ret = new BambooScanResults();
            List<ScanResults> results = new ArrayList<>();
            //initialize cx client
            try {
                
            	commonClient = CommonClientFactory.getInstance(config, log);
                ScanResults initResults = delegator.init();
                results.add(initResults);
            } catch (Exception ex) {
                if (ex.getMessage().contains("Server is unavailable")) {
                    try {
                        delegator.getSastClient().login();
                    } catch (CxClientException e) {
                        throw new TaskException(e.getMessage());
                    }
                    throw new TaskException(CONNECTION_FAILED_COMPATIBILITY);
                }
                ret.setGeneralException(ex);
                throw new TaskException(ex.getMessage());
            }
            if (config.isOsaEnabled()) {
                //we do this in order to redirect the logs from the filesystem agent component to the build console
                String appenderName = "cxAppender_" + buildContext.getBuildKey().getKey();
                Logger.getRootLogger().addAppender(new CxAppender(taskContext.getBuildLogger(), appenderName));
            }

            ScanResults createScanResults = delegator.initiateScan();
            results.add(createScanResults);
            ScanResults scanResults = config.getSynchronous() ? delegator.waitForScanResults() : delegator.getLatestScanResults();
            results.add(scanResults);
            
            //Asynchronous MODE
            if (!config.getSynchronous()) {
                log.info("Running in Asynchronous mode. Not waiting for scan to finish");
                ScanResults finalScanResults = getFinalScanResults(results);
                String scanSummary = delegator.generateHTMLSummary(finalScanResults);
                ret.getSummary().put(HTML_REPORT, scanSummary);

                if (ret.getException() != null || ret.getGeneralException() != null) {
                    printBuildFailure(null, ret, log);
                    return taskResultBuilder.failed().build();
                }

                return taskResultBuilder.success().build();
            }
            
            if (config.getSynchronous() && config.isSastEnabled() &&
                    ((createScanResults.getSastResults() != null && createScanResults.getSastResults().getException() != null && createScanResults.getSastResults().getScanId() > 0) || (scanResults.getSastResults() != null && scanResults.getSastResults().getException() != null))) {
                cancelScan(delegator);
            }
            
            if (config.getEnablePolicyViolations()) {
                delegator.printIsProjectViolated(scanResults);
            }
            ScanResults finalScanResults = getFinalScanResults(results);
            if (!config.getHideResults()) {
                String showSummaryStr = delegator.generateHTMLSummary(finalScanResults);
                ret.getSummary().put(HTML_REPORT, showSummaryStr);
                SASTResults sastResults = ret.getSastResults();
                String sastPDFLink = sastResults.getSastPDFLink();
                String pdfName = sastPDFLink.substring(sastPDFLink.lastIndexOf(File.separator) + 1);
                Key buildKey = buildContext.getParentBuildContext().getResultKey().getEntityKey();
                int buildNumber = buildContext.getParentBuildContext().getResultKey().getResultNumber();
                String buildPath = buildContext.getPlanResultKey().getPlanKey().getKey().substring(buildContext.getPlanResultKey().getPlanKey().getKey().lastIndexOf("-") + 1);
                shraga.updateSASTPdfLink("/artifact/" + buildKey + "/" + buildPath + "/build-" + buildNumber + "/Checkmarx-PDF-Report/" + pdfName);

                
                buildContext.getBuildResult().getCustomBuildData().putAll(ret.getSummary());

/*                String workDir = buildContext.getBuildResult().getCustomBuildData().get("build.working.directory");
                workDir = workDir.substring(workDir.lastIndexOf(File.separator) + 1) + CxParam.CX_REPORT_LOCATION;*/
                ArtifactDefinitionContext artifact = new ArtifactDefinitionContextImpl("Checkmarx PDF Report", false, null);
                artifact.setCopyPattern("**/" + pdfName);

                ArtifactPublishingResult result = artifactManager.publish(taskContext.getBuildLogger(),
                        buildContext.getPlanResultKey(),
                        new File(taskContext.getWorkingDirectory(), CxParam.CX_REPORT_LOCATION),
                        artifact,
                        new HashMap<>(),
                        15);
                taskContext.getBuildContext().getArtifactContext().addPublishingResult(result);
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
    private ScanResults getFinalScanResults(List<ScanResults> results) {
        ScanResults scanResults = new ScanResults();

        for (int i = 0; i < results.size(); i++) {
            Map<ScannerType, Results> resultsMap = results.get(i).getResults();
            for (Map.Entry<ScannerType, Results> entry : resultsMap.entrySet()) {
                if (entry != null && entry.getValue() != null && entry.getValue().getException() != null && scanResults.get(entry.getKey()) == null) {
                    scanResults.put(entry.getKey(), entry.getValue());
                }
                if (i == results.size() - 1 && entry != null && entry.getValue() != null && entry.getValue().getException() == null) {
                    scanResults.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return scanResults;
    }
    private void cancelScan(CxClientDelegator delegator) {
        try {
            delegator.getSastClient().cancelSASTScan();
        } catch (Exception ignored) {
        }
    }

}