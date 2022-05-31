(function ($) {
    $(document).on("click", "#test-cxsca-connection", function (event) {
		console.log('testing..');
        $('#test-cxsca-connection-message').html("");
        restRequest();
    });

    function restRequest() {

        if (!validateScaFields()) {
        	return;
        }
        var request = JSON.stringify(getInputData());


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

        var xhr = createRestRequest("POST", "/rest/checkmarx/1.0/test/scaconnection");
        if (!xhr) {
            console.log("Request Failed");
            return;
        }

        xhr.onload = function () {
            var parsed = JSON.parse(xhr.responseText);
            if (xhr.status == 200) {
                $('#test-cxsca-connection-message').css('color', 'green');
            }
            else {
                $('#test-cxsca-connection-message').css('color', '#d22020');
            }
            $('#test-cxsca-connection-message').html(parsed.loginResponse);
        };


        xhr.onerror = function () {
            console.log('There was an error!');
        };
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.send(request);
    }


    function validateScaFields() {

        var messageElement = $('#test-cxsca-connection-message');

        if ($('#cxScaAPIUrl').val().length < 1) {
            messageElement.text('CxSca API Url must not be empty');
            messageElement.css('color', '#d22020');
            return false;
        } else if ($('#cxAccessControlServerUrl').val().length < 1) {
            messageElement.text('CxAccess Control Server Url must not be empty');
            messageElement.css('color', '#d22020');
            return false;
        } else if ($('#cxScaWebAppUrl').val().length < 1) {
            messageElement.text('CxSca Web App Url must not be empty');
            messageElement.css('color', '#d22020');
            return false;
        } else if ($('#cxScaAccountName').val().length < 1) {
            messageElement.text('CxSca Account Name must not be empty');
            messageElement.css('color', '#d22020');
            return false;
        }else if ($('#cxScaUsername').val().length < 1) {
            messageElement.text('CxSca Username must not be empty');
            messageElement.css('color', '#d22020');
            return false;
        }else if ($('#cxScaPassword').val().length < 1) {
            messageElement.text('CxSca Password must not be empty');
            messageElement.css('color', '#d22020');
            return false;
        }
        return true;
    }

    function getInputData() {
    
    	var enableProxy = "false";
    	if ( $( "#enableProxy" ).length ) {
    		if ($('#enableProxy').is(":checked"))
			{
  				enableProxy = "true";
			}
    	}else {
    		if ($('#globalEnableProxy').is(":checked"))
			{
  				enableProxy = "true";
			}
    	}
    	
    
        return {
            "scaServerUrl": $("#cxScaAPIUrl").val(),
            "scaAccessControlUrl": $('#cxAccessControlServerUrl').val(),
            "cxScaWebAppUrl": $('#cxScaWebAppUrl').val(),
			"scaAccountName": $("#cxScaAccountName").val(),
            "scaUserName": $('#cxScaUsername').val(),
            "pss": $('#cxScaPassword').val(),
            "proxyEnable":enableProxy
        };
    }
})
(AJS.$);
