package com.cx.plugin.results;

import com.atlassian.bamboo.chains.ChainResultsSummaryImpl;
import com.atlassian.plugin.web.api.model.WebPanel;
//import com.atlassian.plugin.web.model.WebPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import static com.cx.plugin.utils.CxParam.HTML_REPORT;

/**
 * Created by: dorg.
 * Date: 01/02/2017.
 */

public class CxPlanResultsWebPanel implements WebPanel {
    
    private static final Logger log = LoggerFactory.getLogger(CxPlanResultsWebPanel.class);

    public String getHtml(Map<String, Object> map) {
        log.info("CxPlanResultsWebPanel.getHtml() called");
        
        try {
            ChainResultsSummaryImpl a = (ChainResultsSummaryImpl) map.get("resultSummary");
            if (a == null) {
                log.warn("resultSummary is null");
                return null;
            }
            
            if (a.getOrderedJobResultSummaries().isEmpty()) {
                log.warn("No job result summaries found");
                return null;
            }
            
            Map<String, String> results = a.getOrderedJobResultSummaries().get(0).getCustomBuildData();
            log.info("CustomBuildData keys: {}", results.keySet());
            
            String htmlReport = results.get(HTML_REPORT);
            log.info("HTML_REPORT value: {}", htmlReport != null ? "Found (length=" + htmlReport.length() + ")" : "null");
            
            return htmlReport;
            
        } catch (Exception e) {
            log.error("Error in CxPlanResultsWebPanel.getHtml()", e);
            return null;
        }
    }

    public void writeHtml(Writer writer, Map<String, Object> map) throws IOException {
        String html = getHtml(map);
        if (html != null) {
            writer.write(html);
        }
    }


}