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
    
    $(document).on("change", "#osaThresholdsEnabled", function (event) {
        checkVisibility();
    });
    
    $(document).on("change", "#cxDependencySettingsCustom", function (event) {
        checkVisibility();
    });
    
    $(document).on("change", "input[name='dependencyScanType']", function (event) {
    checkVisibility();
	});
	
	$(document).on("change", "input[name='scanControlSection']", function (event) {
    checkVisibility();
	});
	
	$(document).on("change", "input[name='globalDependencyScanType']", function (event) {
		alert('globalDependencyScanType value before calling checkVisibility func ' + $("input[name='globalDependencyScanType']:checked").val());
		alert('globalDependencyScanType value before calling checkVisibility func 1 ' + $("input[name='globalDependencyScanType']:checked").value());
    checkVisibility();
	});
    
	function checkVisibility() {
		var cxDependencySettingsCustomVar = $('#cxDependencySettingsCustom').prop('checked');
		var checkVisiblityOfScaVar = $('#checkVisiblityOfSca');
		var globalMediumThresholdVar = $('#globalMediumThreshold').val();
		var mediumThresholdVar = $('#mediumThreshold').val();
		var dependencyScanTypeVar = $("input[name='dependencyScanType']:checked").val();
		var globalDependencyScanTypeVar = $("input[name='globalDependencyScanType']:checked").val();

		alert('cxDependencySettingsCustomVar:' + cxDependencySettingsCustomVar);
		alert('globalDependencyScanTypeVar' + globalDependencyScanTypeVar);
		alert('dependencyScanTypeVar 1' + dependencyScanTypeVar);
		alert('globalDependencyScanTypeVar' + globalDependencyScanTypeVar);
		alert('globalMediumThresholdVar ' + globalMediumThresholdVar);
		alert('mediumThresholdVar ' + mediumThresholdVar);
		alert('dependencyScanTypeVar 1' + dependencyScanTypeVar);
		if ((cxDependencySettingsCustomVar == true && dependencyScanTypeVar == 'AST_SCA') ||
			(cxDependencySettingsCustomVar == false && globalDependencyScanTypeVar == 'AST_SCA')) {
			checkVisiblityOfScaVar.prop('checked',true);
			$('#checkVisiblityOfSca').trigger('change');
		} else {
			checkVisiblityOfScaVar.prop('checked',false);
			$('#checkVisiblityOfSca').trigger('change');
		}
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
