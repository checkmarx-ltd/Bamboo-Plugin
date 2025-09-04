package com.cx.plugin.results;

import com.opensymphony.xwork.ActionSupport;

import java.io.*;

import org.apache.struts2.ServletActionContext;

public class ViewCheckmarxPdfAction extends ActionSupport {

	private static final long serialVersionUID = -3891298903377272644L;
	private InputStream pdfStream;
    private String pdfReportPath;
    
    public String execute() throws Exception {
        
        File pdfFile = new File(pdfReportPath);
        
        System.out.println("pdfFile "+pdfFile.getAbsolutePath());
        if (!pdfFile.exists() || !pdfFile.getName().endsWith(".pdf")) {
            throw new FileNotFoundException("PDF not found at: " + pdfFile.getAbsolutePath());
        }

        this.pdfStream = new FileInputStream(pdfFile);

        return SUCCESS;
    }


    public InputStream getPdfStream() {
        return pdfStream;
    }
    
    public String getContentDisposition() {
        String title = ServletActionContext.getRequest().getParameter("title");
        System.out.println("getContentDispositiontitle ::"+ServletActionContext.getRequest().getParameter("title"));
        if (title == null || title.trim().isEmpty()) {
            title = "Checkmarx_Report";
        }
        return "inline; filename=\"" + title + ".pdf\"";
    }

    public String getContentType() {
        return "application/pdf";
    }

    public void setPdfReportPath(String pdfReportPath) {
    	this.pdfReportPath = pdfReportPath; 
    }
}