package com.cx.plugin.task;

import org.slf4j.impl.StaticLoggerBinder;
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
import com.cx.plugin.utils.CxParam;
import com.cx.restclient.CxClientDelegator;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.dto.ProxyConfig;
import com.cx.restclient.dto.Results;
import com.cx.restclient.dto.ScanResults;
import com.cx.restclient.dto.ScannerType;
import com.cx.restclient.dto.scansummary.ScanSummary;
import com.cx.restclient.exception.CxClientException;
import com.cx.restclient.sast.dto.SASTResults;
import com.cx.restclient.sast.utils.LegacyClient;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cx.plugin.utils.CxParam.CONNECTION_FAILED_COMPATIBILITY;
import static com.cx.plugin.utils.CxParam.HTML_REPORT;
import static com.cx.plugin.utils.CxPluginUtils.printBuildFailure;
import static com.cx.plugin.utils.CxPluginUtils.printConfiguration;


public class CheckmarxTask implements TaskType {

    private final ArtifactManager artifactManager;
   	   
    public CheckmarxTask(final ArtifactManager artifactManager) {
        this.artifactManager = artifactManager;
    }

    @NotNull
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException {
    	StaticLoggerBinder binder = StaticLoggerBinder.getSingleton(taskContext.getBuildLogger());    	
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

            //Disable the proxy is it is enabled but proxy settings are not available
            if (config.isProxy()) {
                ProxyConfig proxy = config.getProxyConfig();
                String proxyHost = proxy.getHost();
                if (proxyHost == null || proxyHost.isEmpty()) {
                    config.setProxy(false);
                }
            }

            if (!config.isSastEnabled() && !configHelper.isDependencyScanEnabled()) {
                log.error("Both SAST and Dependency Scan are disabled. Exiting.");
                taskResultBuilder.failed().build();
            }
            //create scans and retrieve results
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
              //We have removed log4j support and added custom sl4j which will use CxLoggerAdapter class instead of CxAppender class.
                //Logger.getRootLogger().addAppender(new CxAppender(taskContext.getBuildLogger(), appenderName));                
            }

            ScanResults createScanResults = delegator.initiateScan();
            results.add(createScanResults);
            ScanResults scanResults = config.getSynchronous() ? delegator.waitForScanResults() : delegator.getLatestScanResults();

            ret.put(ScannerType.SAST, scanResults.getSastResults());
            if (config.isOsaEnabled()) {
                ret.put(ScannerType.OSA, scanResults.getOsaResults());
            } else if (config.isAstScaEnabled()) {
                ret.put(ScannerType.AST_SCA, scanResults.getScaResults());
            }
            results.add(scanResults);

            if (config.getEnablePolicyViolations()) {
                delegator.printIsProjectViolated(scanResults);
            }

            //assert if expected exception is thrown  
            ScanSummary scanSummary = new ScanSummary(config, ret.getSastResults(), ret.getOsaResults(), ret.getScaResults());

            if (scanSummary.hasErrors() || ret.getGeneralException() != null ||
                    (config.isSastEnabled() && (ret.getSastResults() == null || ret.getSastResults().getException() != null)) ||
                    (config.isOsaEnabled() && (ret.getOsaResults() == null || ret.getOsaResults().getException() != null)) ||
                    (config.isAstScaEnabled() && (ret.getScaResults() == null || ret.getScaResults().getException() != null))) {

                StringBuilder scanFailedAtServer = new StringBuilder();
                if (config.isSastEnabled() && (ret.getSastResults() == null || !ret.getSastResults().isSastResultsReady()))
                    scanFailedAtServer.append("CxSAST scan results are not found. Scan might have failed at the server or aborted by the server.\n");
                if (config.isOsaEnabled() && (ret.getOsaResults() == null || !ret.getOsaResults().isOsaResultsReady()))
                    scanFailedAtServer.append("CxSAST OSA scan results are not found. Scan might have failed at the server or aborted by the server.\n");
                if (config.isAstScaEnabled() && (ret.getScaResults() == null || !ret.getScaResults().isScaResultReady()))
                    scanFailedAtServer.append("CxAST SCA scan results are not found. Scan might have failed at the server or aborted by the server.\n");

                if (scanSummary.hasErrors() && scanFailedAtServer.toString().isEmpty())
                    scanFailedAtServer.append(scanSummary.toString());
                else if (scanSummary.hasErrors())
                    scanFailedAtServer.append("\n").append(scanSummary.toString());

                printBuildFailure(scanFailedAtServer.toString(), ret, log);

                //handle hard failures. In case of threshold or policy failure, we still need to generate report before returning.
                //Hence, cannot return yet
                if (!scanSummary.hasErrors())
                    return taskResultBuilder.failed().build();
            }

            //Asynchronous MODE
            if (!config.getSynchronous()) {
                log.info("Running in Asynchronous mode. Not waiting for scan to finish.");
                ScanResults finalScanResults = getFinalScanResults(results);
                String scanHTMLSummary = delegator.generateHTMLSummary(finalScanResults);
                ret.getSummary().put(HTML_REPORT, scanHTMLSummary);
                buildContext.getBuildResult().getCustomBuildData().putAll(ret.getSummary());

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

            ScanResults finalScanResults = getFinalScanResults(results);
            if (!config.getHideResults()) {
                SASTResults sastResults = scanResults.getSastResults();

                if (config.getGeneratePDFReport()) {
                    String sastPDFLink = sastResults.getSastPDFLink();
                    String pdfName = sastPDFLink.substring(sastPDFLink.lastIndexOf(File.separator) + 1);
                    Key buildKey = buildContext.getParentBuildContext().getResultKey().getEntityKey();
                    int buildNumber = buildContext.getParentBuildContext().getResultKey().getResultNumber();
                    String buildPath = buildContext.getPlanResultKey().getPlanKey().getKey()
                            .substring(buildContext.getPlanResultKey().getPlanKey().getKey().lastIndexOf("-") + 1);

                    ArtifactDefinitionContext pdfArt = getPDFArt(taskContext);
                    if (pdfArt != null) {
                        if (pdfArt.isSharedArtifact()) {
                            sastResults.setSastPDFLink("/browse/" + buildKey + "-" + buildNumber + "/artifact/shared/" + pdfArt.getName()
                                    + "/" + pdfName);
                        } else {
                            sastResults.setSastPDFLink("/browse/" + buildKey + "-" + buildNumber + "/artifact/" + buildPath
                                    + "/" + pdfArt.getName() + "/" + pdfName);
                        }
                    } else {
                        sastResults.setSastPDFLink("/browse/" + buildKey + "-" + buildNumber + "/artifact/" + buildPath
                                + "/Checkmarx-PDF-Report/" + pdfName);
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
                }

                String showSummaryStr = delegator.generateHTMLSummary(finalScanResults);
                ret.getSummary().put(HTML_REPORT, showSummaryStr);
                buildContext.getBuildResult().getCustomBuildData().putAll(ret.getSummary());
            }

            if (scanSummary.hasErrors()) {
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

    private ArtifactDefinitionContext getPDFArt(TaskContext taskContext) {
        if (!taskContext.getBuildContext().getArtifactContext().getDefinitionContexts().isEmpty()) {
            for (ArtifactDefinitionContext artDef : taskContext.getBuildContext().getArtifactContext().getDefinitionContexts()) {
                if (StringUtils.isNotEmpty(artDef.getCopyPattern()) &&
                        StringUtils.containsIgnoreCase(artDef.getCopyPattern(), "pdf")) {
                    return artDef;
                }
            }
        }
        return null;
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