package com.cx.plugin.results;

import com.atlassian.bamboo.resultsummary.BuildResultsSummaryImpl;
import com.atlassian.plugin.web.api.model.WebPanel;
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

public class CxJobResultsWebPanel implements WebPanel {
    
    private static final Logger log = LoggerFactory.getLogger(CxJobResultsWebPanel.class);

    public String getHtml(Map<String, Object> map) {
        log.info("CxJobResultsWebPanel.getHtml() called");
        
        try {
            BuildResultsSummaryImpl buildResultsSummaryImpl = (BuildResultsSummaryImpl) map.get("resultSummary");
            if (buildResultsSummaryImpl == null) {
                log.warn("resultSummary is null");
                return null;
            }

            Map<String, String> results = buildResultsSummaryImpl.getCustomBuildData();
            log.debug("CustomBuildData keys: {}", results.keySet());
            
            String htmlReport = results.get(HTML_REPORT);
            log.info("HTML_REPORT value: {}", htmlReport != null ? "Found (length=" + htmlReport.length() + ")" : "null");
            
            return htmlReport;
            
        } catch (Exception e) {
            log.error("Error in CxJobResultsWebPanel.getHtml()", e);
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