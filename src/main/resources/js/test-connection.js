(function ($) {
	
    $(document).on("click", "#test_connection", function (event) {
        $('#testConnectionMessage').html("");
        restRequest();
    });

    function restRequest() {

        if (!validateFields()) {
            return populateEmptyDropdownList();
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

        var xhr = createRestRequest("POST", "/rest/checkmarx/1.0/test/connection");
        if (!xhr) {
            console.log("Request Failed");
            return;
        }
        xhr.onload = function() {
		  var parsed = JSON.parse(xhr.responseText);
            if (xhr.status == 200) {
                $('#testConnectionMessage').css('color', 'green');
                populateDropdownList(parsed.presetList, "#presetListId");
                populateDropdownList(parsed.teamPathList, "#teamPathListId"); 
   				
   				}
            else {
                $('#testConnectionMessage').css('color', '#d22020');
                populateEmptyDropdownList();
            }
            $('#testConnectionMessage').html(parsed.loginResponse);
        };


        xhr.onerror = function () {
            console.log('There was an error!');
        };
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.send(request);
    }

    function validateFields() {

        var messageElement = $('#testConnectionMessage');
        if ($('#serverUrl').val().length < 1) {
            messageElement.text('URL must not be empty');
            messageElement.css('color', '#d22020');
            return false;
        } else if ($('#username').val().length < 1) {
            messageElement.text('Username must not be empty');
            messageElement.css('color', '#d22020');
            return false;
        } else if ($('#password').val().length < 1) {
            messageElement.text('Password must not be empty');
            messageElement.css('color', '#d22020');
            return false;
        }

        return true;
    }

	window.onload = function() {
            alert('Page is loaded');
        };
    
    
    $(document).on("change", "#osaThresholdsEnabled", function (event) {
        alert('onchange event alert');
        checkVisibility();
    });
    
	function checkVisibility() {
		alert('inside checkVisibility funtion');
		var cxDependencySettingsCustomVar = $('#cxDependencySettingsCustom').prop('checked');
		var checkVisiblityOfScaVar = $('#checkVisiblityOfSca');
		var dependencyScanTypeVar = $("input[name='dependencyScanType']:checked").val();
		var dependencyScanTypeVar2 = $("#dependencyScanType:checked").val();
		var globalDependencyScanTypeVar = $("input[name='globalDependencyScanType']:checked").val();
		
		alert('cxDependencySettingsCustomVar:' + cxDependencySettingsCustomVar);
		alert('checkVisiblityOfScaVar:' + checkVisiblityOfScaVar.prop('value')  + ':'+ checkVisiblityOfScaVar.value);
		alert('dependencyScanTypeVar' + dependencyScanTypeVar);
		alert('dependencyScanTypeVar2' + dependencyScanTypeVar2);
		alert('globalDependencyScanTypeVar' + globalDependencyScanTypeVar);

		if ((cxDependencySettingsCustomVar == true && dependencyScanTypeVar == 'AST_SCA') ||
			(cxDependencySettingsCustomVar == false && globalDependencyScanTypeVar == 'AST_SCA')) {
			alert('inside checkVisibility funtion checkVisiblityOfSca set true');
			checkVisiblityOfScaVar.prop('checked',true);
		} else {
			alert('inside checkVisibility funtion checkVisiblityOfSca set false');
			checkVisiblityOfScaVar.prop('checked',false);
		}
	}
        
        function showAlert() {
			alert('inside alert funtion *****');
		}

    function getInputData() {
        return {
            "url": $("#serverUrl").val(),
            "username": $('#username').val(),
            "pas": $('#password').val(),
            "proxyEnable": $('#enableProxy').prop('checked')            
        };
    }

    function populateEmptyDropdownList() {
        var NO_PRESET_MESSAGE = "Unable to connect to server. Make sure URL and Credentials are valid to see presets list";
        var NO_TEAM_MESSAGE = "Unable to connect to server. Make sure URL and Credentials are valid to see teams list";
        var noPresetList = [{id: 'noPreset', name: NO_PRESET_MESSAGE}];
        var noTeamList = [{id: 'noTeamPath', name: NO_TEAM_MESSAGE}];
        populateDropdownList(noPresetList, "#presetListId");
        populateDropdownList(noTeamList, "#teamPathListId");
        return;

    }


    function populateDropdownList(data, selector) {
        $(selector).empty();
        for (var i in data) {
            var name = data[i].name;
            if (name == null)
                name = data[i].fullName;
            if (i == 0) // select first item
                var itemval = '<option value="' + data[i].id + '" selected>' + name + '</option>';
            else
                var itemval = '<option value="' + data[i].id + '">' +name + '</option>';
            $(selector).append(itemval);
        }
    }

})
(AJS.$);
