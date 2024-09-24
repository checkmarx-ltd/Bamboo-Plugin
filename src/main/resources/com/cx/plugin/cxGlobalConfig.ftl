<style type="text/css" xmlns="http://www.w3.org/1999/html">

    form.aui .description {
        width: 90%;
    }

    .cx.center {
        padding: 16px;
        border: solid 1px;
        width: 90%;
        margin-top: 10px;
        margin-bottom: 25px;
        min-width: 500px;
        max-width: 820px;
        box-sizing: border-box;
    }

    input#radioGroupcustomConfigurationServer, input#radioGroupglobalConfigurationServer, input#radioGroupglobalConfigurationCxSAST, input#radioGroupcustomConfigurationCxSAST, input#radioGroupglobalConfigurationControl, input#radioGroupcustomConfigurationControl, input#radioGroupOSA, input#radioGroupAST_SCA {
        width: 20px;
    }

    form.aui.top-label .field-group > label {
        margin: 5px 0;
        font-weight: bold;
    }

    form.aui .radio {
        width: 43%;
        display: inline-block;
    }

    form.aui .checkbox > label, form.aui .radio > label {
        color: #333333;
        font-weight: bold;
    }

    .aui-page-focused .aui-page-panel-content h3:not(.collapsible-header) {
        margin-top: 0;
    }
    
   .hidden{
   visibility:hidden;
   }

    h3 {
        color: #3b73af;
        font-size: 17px;
    }

    form.aui .field-value {
        border-radius: 3.01px;
        max-width: 225px;
        max-height: 17px;
        width: 108%;
        border: 1px solid #cccccc;
        padding-bottom: 6px;
        display: inline-block;
        background-color: #f1f1f1;
        font-weight: bold;
        min-height: 13px;
        padding-top: 5px;
        padding-left: 10px;
    }

    #panel-editor-config form.aui .long-field {
        max-width: 600px;
        width: 90%;
        max-height: 374px;
        min-height: 12px;
    }

    .aui-button.test-connection {
        margin: 10px 0;
    }

    #spinner {
        display: none;
        position: absolute;
        left: 50%;
        top: 20%;
    }


</style>



[@ww.form action="checkmarxDefaultConfiguration!save.action" method="post" submitLabelKey="cxDefaultConfigSubmit.label" titleKey="cxDefaultConfigTitle.label" cssClass="top-label"]
    [@ui.bambooSection title='Checkmarx Server' cssClass="cx center"]
        [@ww.textfield labelKey="serverUrl.label" name="globalServerUrl"/]
        [@ww.textfield labelKey="username.label" name="globalUsername"/]
        [@ww.password labelKey="password.label" name="globalPss" showPassword='true' /]
		[@ww.checkbox labelKey="globalEnableProxy.label" name="globalEnableProxy" descriptionKey="globalEnableProxy.description" toggle='true' /]
    <button type="button" class="aui-button test-connection" id="g_test_connection" onclick="connectToServer()">Connect to Server</button>
    <div id="gtestConnectionMessage" class="test-connection-message"></div>
    [/@ui.bambooSection]

    [@ui.bambooSection title='Checkmarx Scan CxSAST' cssClass="cx center"]
        [@ww.textfield labelKey="folderExclusions.label" name="globalFolderExclusions" descriptionKey="folderExclusions.description"  cssClass="long-field"/]
        [@ww.textarea labelKey="filterPatterns.label" name="globalFilterPatterns" rows="4"  cssClass="long-field"/]
        [@ww.textfield labelKey="scanTimeoutInMinutes.label" name="globalScanTimeoutInMinutes" required='false'/]
    [/@ui.bambooSection]

    [@ui.bambooSection title='Dependency Scan' cssClass="cx center"]
	[@ww.checkbox labelKey="globalEnableDependencyScan.label" name="globalEnableDependencyScan" toggle='true' /]
	[@ui.bambooSection dependsOn="globalEnableDependencyScan" showOn="true"]
	[@ww.textarea labelKey="globalDependencyScanFilterPatterns.label" name="globalDependencyScanFilterPatterns" descriptionKey="globalDependencyScanFilterPatterns.description" rows="4" cssClass="long-field"/]
	[@ww.textfield labelKey="globalDependencyScanfolderExclusions.label" name="globalDependencyScanfolderExclusions" descriptionKey="globalDependencyScanfolderExclusions.description" cssClass="long-field"/]
		[@ww.radio id = 'radioGroup' name='globalDependencyScanType' listKey='key' listValue='value' toggle='true' list=globalDependencyScanTypeValues /]
		
		[@ui.bambooSection title='Checkmarx Scan CxOSA' dependsOn='globalDependencyScanType' showOn='OSA' cssClass="cx center" ]
			<p class="description">
				<small>
					Open Source Analysis (OSA) helps you manage the security risk involved in using open
					source libraries in your applications
				</small>
			</p>
			
			[@ww.textfield labelKey="globalOsaArchiveIncludePatterns.label" name="globalOsaArchiveIncludePatterns" descriptionKey="globalOsaArchiveIncludePatterns.description"/]
			[@ww.checkbox labelKey="globalOsaInstallBeforeScan.label" name="globalOsaInstallBeforeScan" descriptionKey="globalOsaInstallBeforeScan.description" toggle='true' /]
		[/@ui.bambooSection]
		[@ui.bambooSection title='Checkmarx Scan CxSCA' dependsOn='globalDependencyScanType' showOn='AST_SCA' cssClass="cx center"]
			<p class="description">
				<small>
					Software Composition Analysis (SCA) helps you manage the security risk involved in using open
					source libraries in your applications
				</small>
			</p>
   		[@ww.textfield labelKey="globalcxScaAPIUrl.label" name="globalcxScaAPIUrl"/]
		[@ww.textfield labelKey="globalcxScaAccessControlServerUrl.label" name="globalcxScaAccessControlServerUrl"/]
		[@ww.textfield labelKey="globalcxScaWebAppUrl.label" name="globalcxScaWebAppUrl"/]
		[@ww.textfield labelKey="globalcxScaAccountName.label" name="globalcxScaAccountName"/]
        
        [@ww.textfield labelKey="cxScaGlobalUsername.label" name="globalcxScaUsername"/]
        [@ww.password labelKey="cxScaGlobalPassword.label" name="globalcxScaPss" showPassword='true' /]
        <button type="button" class="aui-button test-cxsca-connection" id="g_test-cxsca-connection" onclick="connectToScaServer()">Connect to Server</button>
		<div id="gtestScaConnectionMessage" class="test-cxsca-connection-message"></div>
        <br/>
		[@ww.checkbox labelKey="cxScaResolverEnabled.label" name="globalCxScaResolverEnabled" id="globalCxScaResolverEnabled" toggle='true' /]
		[@ui.bambooSection cssClass="cx" dependsOn="globalCxScaResolverEnabled" showOn="true"]
			[@ww.textfield labelKey="cxScaResolverPath.label" name="globalCxScaResolverPath" id="globalCxScaResolverPath" descriptionKey="cxScaResolverPath.description" required='true'/]
			[@ww.textarea labelKey="cxScaResolverAddParam.label" name="globalCxScaResolverAddParam" id="globalCxScaResolverAddParam" descriptionKey="cxScaResolverAddParam.description" rows="3" cssClass="long-field"/]
		[/@ui.bambooSection]
		[/@ui.bambooSection]
	[/@ui.bambooSection]
[/@ui.bambooSection]
        
    [@ui.bambooSection title='Control Checkmarx Scan' cssClass="cx center"]

        [@ww.checkbox labelKey="isSynchronous.label" name="globalIsSynchronous" descriptionKey="isSynchronous.description" toggle='true' /]
		[@ww.hidden name="globalEnableCriticalSeverity"/]
        [@ui.bambooSection dependsOn='globalIsSynchronous' showOn='true']
            [@ww.checkbox labelKey="enablePolicyViolations.label" name="globalEnablePolicyViolations" descriptionKey="enablePolicyViolations.description" toggle='true' /]
            [@ww.checkbox labelKey="enablePolicyViolationsSCA.label" name="globalEnablePolicyViolationsSCA" descriptionKey="enablePolicyViolationsSCA.description" toggle='true' /]
            [@ww.checkbox labelKey="thresholdsEnabled.label" name="globalThresholdsEnabled" descriptionKey="thresholdsEnabled.description" toggle='true' /]
            [@ui.bambooSection dependsOn='globalThresholdsEnabled' showOn='true']
            	[@ui.bambooSection dependsOn="globalEnableCriticalSeverity" showOn="true"]
                [@ww.textfield labelKey="sastCriticalThreshold.label" name="globalCriticalThreshold" required='false'/]
                [/@ui.bambooSection]
                [@ww.textfield labelKey="sastHighThreshold.label" name="globalHighThreshold" required='false'/]
                [@ww.textfield labelKey="sastMediumThreshold.label" name="globalMediumThreshold" required='false'/]
                [@ww.textfield labelKey="sastLowThreshold.label" name="globalLowThreshold" required='false'/]
            [/@ui.bambooSection]

            [@ui.bambooSection dependsOn='globalEnableDependencyScan' showOn='true']
            [@ww.checkbox labelKey="osaThresholdsEnabled.label" name="globalOsaThresholdsEnabled" descriptionKey="thresholdsEnabled.description" toggle='true' /]
            [@ui.bambooSection dependsOn='globalOsaThresholdsEnabled' showOn='true']
            	[@ui.bambooSection dependsOn='globalDependencyScanType' showOn='AST_SCA']
   				[@ww.textfield labelKey="osaCriticalThreshold.label" name="globalOsaCriticalThreshold" required='false'/]
				[/@ui.bambooSection]
                [@ww.textfield labelKey="osaHighThreshold.label" name="globalOsaHighThreshold" required='false'/]
                [@ww.textfield labelKey="osaMediumThreshold.label" name="globalOsaMediumThreshold" required='false'/]
                [@ww.textfield labelKey="osaLowThreshold.label" name="globalOsaLowThreshold" required='false'/]
            [/@ui.bambooSection]
            [/@ui.bambooSection]
        [/@ui.bambooSection]

        [@ww.checkbox labelKey="isglobalDenyProject.label" name="globalDenyProject" descriptionKey="isglobalDenyProject.description" /]
        [@ww.checkbox labelKey="isglobalHideResults.label" name="globalHideResults" descriptionKey="isglobalHideResults.description" toggle='true' /]

    [/@ui.bambooSection]
[/@ww.form]

<script>
function connectToScaServer() {

        document.getElementById("gtestScaConnectionMessage").innerHTML = "";
            restScaRequest();
    }

        function restScaRequest() {
			var request;
			var scaServerUrl = document.getElementById("checkmarxDefaultConfiguration_globalcxScaAPIUrl").value;
            var scaAccessControlUrl = document.getElementById("checkmarxDefaultConfiguration_globalcxScaAccessControlServerUrl").value;
            var scaWebAppUrl = document.getElementById("checkmarxDefaultConfiguration_globalcxScaWebAppUrl").value;
			var scaAccountName = document.getElementById("checkmarxDefaultConfiguration_globalcxScaAccountName").value;
			var scaUserName = document.getElementById("checkmarxDefaultConfiguration_globalcxScaUsername").value;
			var pss = document.getElementById("checkmarxDefaultConfiguration_globalcxScaPss").value;
			var enableProxy = document.getElementById("checkmarxDefaultConfiguration_globalEnableProxy").checked;
			
			
        if (!validateScaFields()) {
        	return;
        }
         request = JSON.stringify(getInputScaData());


        function createScaRestRequest(method, url) {

            var resolvedUrl = AJS.contextPath() + url;

            var xhr = new XMLHttpRequest();
            if ("withCredentials" in xhr) {
                xhr.open(method, resolvedUrl, true);

            } else if (typeof XDomainRequest != "undefined") {
                xhr = new XDomainRequest();
                xhr.open(method, resolvedUrl);
            } else {
                xhr = null;
            }
            return xhr;
        }

        var xhr = createScaRestRequest("POST", "/rest/checkmarx/1.0/test/scaconnection");
        if (!xhr) {
            console.log("Request Failed");
            return;
        }

        xhr.onload = function () {
                var parsed = JSON.parse(xhr.responseText);
                var message = document.getElementById("gtestScaConnectionMessage");
                if (xhr.status == 200) {
                    message.style.color = "green";
                }
                else {
                    message.style.color = "#d22020"
                }
                message.innerHTML = parsed.loginResponse;
            };


        xhr.onerror = function () {
            console.log('There was an error!');
        };
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.send(request);
    


    function validateScaFields() {

		var messageElement = document.getElementById("gtestScaConnectionMessage");
        
				if (scaServerUrl.length < 1) {
                    messageElement.textContent = 'CxSca API Url must not be empty';
                    messageElement.style.color = "#d22020";
                    return false;
                } else if (scaAccessControlUrl.length < 1) {
                    messageElement.textContent = "CxAccess Control Server Url must not be empty";
                    messageElement.style.color = "#d22020";
                    return false;
                } else if (scaWebAppUrl.length < 1) {
                    messageElement.textContent = "CxSca Web App Url must not be empty";
                    messageElement.style.color = "#d22020";
                    return false;
                }
				else if (scaAccountName.length < 1) {
                    messageElement.textContent = "CxSca Account Name must not be empty";
                    messageElement.style.color = "#d22020";
                    return false;
                }
				else if (scaUserName.length < 1) {
                    messageElement.textContent = "CxSca Username must not be empty";
                    messageElement.style.color = "#d22020";
                    return false;
                }
				else if (pss.length < 1) {
                    messageElement.textContent = "CxSca Password must not be empty";
                    messageElement.style.color = "#d22020";
                    return false;
                }
               
        return true;
    }

    function getInputScaData() {
        return {
            "scaServerUrl": scaServerUrl,
            "scaAccessControlUrl": scaAccessControlUrl,
            "scaWebAppUrl": scaWebAppUrl,
			"scaAccountName": scaAccountName,
            "scaUserName": scaUserName,
            "pss": pss,
            "proxyEnable":enableProxy
        };
    }
	}
	
    function connectToServer() {
        document.getElementById("gtestConnectionMessage").innerHTML = "";
            restRequest();
    }

        function restRequest() {
            var request;

            var url = document.getElementById("checkmarxDefaultConfiguration_globalServerUrl").value;
            var username = document.getElementById("checkmarxDefaultConfiguration_globalUsername").value;
            var pas = document.getElementById("checkmarxDefaultConfiguration_globalPss").value;
            var enableProxy = document.getElementById("checkmarxDefaultConfiguration_globalEnableProxy").checked;

            if (!validateGlobalFields()) {
                return;
            }
            request = JSON.stringify(getGlobalInputData());

            function createRestRequest(method, url) {

                var resolvedUrl = AJS.contextPath() + url;

                var xhr = new XMLHttpRequest();
                if ("withCredentials" in xhr) {
                    xhr.open(method, resolvedUrl, true);

                } else if (typeof XDomainRequest != "undefined") {
                    xhr = new XDomainRequest();
                    xhr.open(method, resolvedUrl);
                } else {
                    xhr = null;
                }
                return xhr;
            }

            var xhr = createRestRequest("POST", "/rest/checkmarx/1.0/test/connection");
            if (!xhr) {
                console.log("Request Failed");
                return;
            }

            xhr.onload = function () {
           
                var parsed = JSON.parse(xhr.responseText);
                 
                
                var message = document.getElementById("gtestConnectionMessage");
                if (xhr.status == 200) {
                    message.style.color = "green";
                }
                else {
                    message.style.color = "#d22020"
                }
                message.innerHTML = parsed.loginResponse;
            };


            xhr.onerror = function () {
                console.log('There was an error!');
            };

            xhr.setRequestHeader("Content-Type", "application/json");
            xhr.send(request);


            function validateGlobalFields() {

                var messageElement = document.getElementById("gtestConnectionMessage");
                if (url.length < 1) {
                    messageElement.textContent = 'URL must not be empty';
                    messageElement.style.color = "#d22020";
                    return false;
                } else if (username.length < 1) {
                    messageElement.textContent = "Username must not be empty";
                    messageElement.style.color = "#d22020";
                    return false;
                } else if (pas.length < 1) {
                    messageElement.textContent = "Password must not be empty";
                    messageElement.style.color = "#d22020";
                    return false;
                }
                return true;
            }

            function getGlobalInputData() {
                return {
                    "url": url,
                    "username": username,
                    "pas": pas,
                    "proxyEnable": enableProxy
                };
            }

        }
</script>

