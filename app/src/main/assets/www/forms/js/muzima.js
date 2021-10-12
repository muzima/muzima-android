/*
 *Copyright (c) The Trustees of Indiana University, Moi University
 *and Vanderbilt University Medical Center.

 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

/* Start - Minimal one element selected
 * Parameter:
 * * Fieldset element where the input must be selected at least one.
 * * Message to be displayed when none of the elements in the fieldset is selected.
 */

var validateSelected = function (source) {
    var errors = {};
    var fieldSet = $(source).filter(':visible');
    if (fieldSet.length) {
        var tag = fieldSet.prop('tagName');
        // check if the source element is a field set (or other container in the future).
        if (tag.toLowerCase() != 'fieldset') {
            // if not, then this is an input element. And then get the closest field set.
            fieldSet = $(source).closest('fieldset');
            // if we can't find the fieldset container, then just return the empty validation errors.
            if (!fieldSet.length) {
                return errors;
            }
        }
        // get the visible inputs element that are checked
        var checkedInput = $(fieldSet).find('input:checked');
        if (checkedInput.length == 0) {
            var mandatoryQuestionValidationFailureMessage = htmlDataStore.getStringResource("hint_mandatory_question_validation_failure");
            errors[$(fieldSet).attr('name')] = mandatoryQuestionValidationFailureMessage;
        }
    }
    return errors;
};
/* End - Minimal one element selected */

/* Start - Selecting an element should be single selection
 * * Parameter:
 * * Fieldset element where the input should be a single selection or the collection of checkboxes elements.
 * * Values which should be a single selection.
 * * Message to be displayed when the element is selected with other element.
 */
var validateAlone = function (source, values, message) {
    var errors = {};
    var fieldSet = $(source).filter(':visible');
    if (fieldSet.length) {
        var tag = fieldSet.prop('tagName');
        // if the tag is not fieldset, then it is an input.
        // then get the closest fieldset for the inputs (which must be a fieldset).
        if (tag.toLowerCase() != 'fieldset') {
            fieldSet = $(source).closest('fieldset');
            // if we can't find the fieldset container, then just return the empty validation errors.
            if (!fieldSet.length) {
                return errors;
            }
        }
        var checkedBoxes = $(fieldSet).find('input:checkbox:checked');
        if (checkedBoxes.length > 1) {
            $.each(checkedBoxes, function (i, checkBox) {
                $.each(values, function (j, value) {
                    if ($(checkBox).val() == value) {
                        errors[$(fieldSet).attr('name')] = message;
                    }
                });
            });
        }
    }
    return errors;
};
/* End - Selecting an element should be single selection */

/* Start - Show and hide validation error messages */
var showValidationMessages = function (errors) {
    var validator = $('form').validate();
    if (!$.isEmptyObject(errors)) {
        validator.showErrors(errors);
    }
};

var toggleValidationMessages = function (errors) {
    var validator = $('form').validate();
    if ($.isEmptyObject(errors)) {
        // remove the error messages
        validator.resetForm();
    } else {
        // show the error messages
        validator.showErrors(errors);
    }
};

/* End - Show and hide validation error messages */

$(document).ready(function () {
    'use strict';
    var dateFormat = "dd-mm-yy";

    if (htmlDataStore.getStatus().toLowerCase() == 'complete') {
        $('input, select, textarea').prop('disabled', true);
    }

    var locations = $("#encounter\\.location_id");
    var locationNamesResults = htmlDataStore.getLocationNamesFromDevice();
    locationNamesResults = JSON.parse(locationNamesResults);
    locations.empty();
    locations.append($("<option>").text("..."));
    $.each(locationNamesResults, function () {
        locations.append($("<option>").attr('value', this.id).text(this.name));
    });

    // setting font size as per user preference
    var font_size = htmlDataStore.getFontSizePreference();
    $('body').addClass(font_size);


    var providers = $("#select_providers");
    var providerNamesResults = htmlDataStore.getProviderNamesFromDevice();
    providerNamesResults = JSON.parse(providerNamesResults);
    providers.empty();
    providers.append($("<option>").text("..."));
    $.each(providerNamesResults, function () {
        providers.append($("<option>").attr('value', this.identifier).text(this.name));
    });

//    /*setting default encounter provider*/
    var encounterProvider = htmlDataStore.getDefaultEncounterProvider();
    encounterProvider = JSON.parse(encounterProvider);
    $.each(encounterProvider, function () {
        $("#encounter\\.provider_id_select").val(this.name);
        $("#encounter\\.provider_id").val(this.identifier);
    });

    /* Start - Toggle free text element */
    var hasFreetext = $('.has-freetext');
    hasFreetext.change(function () {
        var freetext = $(this).closest('.section').find('.freetext');
        if ($(this).is(':checkbox')) {
            if ($(this).is(':checked')) {
                freetext.show();
            } else {
                freetext.hide();
            }
        }
    });
    hasFreetext.trigger('change');
    /* End - Toggle free text element */

    /* Start - Toggle future date validation */
    $('.future-date').change(function () {
        if ($(this).is(':visible') && $(this).val() != '') {
            var errors = {};
            var pattern = /(\d{2})-(\d{2})-(\d{4})/g;
            var matches = pattern.exec($(this).val());
            if (matches != null && matches.length > 3) {
                var enteredDate = new Date(matches[3], matches[2] - 1, matches[1]);
                var reference = new Date();
                var today = new Date(reference.getFullYear(), reference.getMonth(), reference.getDate());

                var futureDateValidationFailureMessage = htmlDataStore.getStringResource("hint_future_date_validation_failure");
                if (enteredDate <= today) {
                    errors[$(this).attr('name')] = futureDateValidationFailureMessage;
                }
            }
            toggleValidationMessages(errors);
        }
    });
    /* End - Toggle future date validation */

    /* Start - Toggle past date validation */
    $('.past-date').change(function () {
        if ($(this).is(':visible') && $(this).val() != '') {
            var errors = {};
            var pattern = /(\d{2})-(\d{2})-(\d{4})/g;
            var matches = pattern.exec($(this).val());
            if (matches != null && matches.length > 3) {
                var enteredDate = new Date(matches[3], matches[2] - 1, matches[1]);
                var reference = new Date();
                var today = new Date(reference.getFullYear(), reference.getMonth(), reference.getDate());
                if (enteredDate > today) {
                    var currentOrPastDateValidationFailureMessage =
                        htmlDataStore.getStringResource("hint_current_or_past_date_validation_failure");
                    errors[$(this).attr('name')] = currentOrPastDateValidationFailureMessage;
                }
            }
            toggleValidationMessages(errors);
        }
    });
    /* End - Toggle past date validation */

    /* Start - Removing error message in the container of checkbox and radio */
    $('input:checkbox, input:radio').change(function () {
        var container = $(this).closest('fieldset');
        $(container).removeClass('error');
        $(container).parent().find('label.error').remove();
    });
    /* End - Removing error message in the container of checkbox and radio */

    /* Start - Function to save the form */
    document.submit = function () {
        var repeatingSections = $('.repeat');
        var showErrorMessageIfInvalid = true;
        var validForm = $("form").valid() && document.multipleSectionRepeatsValidationCheck(repeatingSections,showErrorMessageIfInvalid);
        if (typeof $.fn.customValidationCheck !== 'undefined' && typeof $.fn.customValidationCheck === 'function') {
            validForm = validForm && $.fn.customValidationCheck();
        }
        if (validForm) {
            $(this).find('input:text').each(function(){
                $(this).val($.trim($(this).val()));
            });
            save("complete", false);
        } else {
            addValidationMessage();
        }
    };

    document.saveMiniForm = function () {
        var repeatingSections = $('.repeat');
        var showErrorMessageIfInvalid = true;
        var validForm = $("form").valid() && document.multipleSectionRepeatsValidationCheck(repeatingSections,showErrorMessageIfInvalid);
        if (typeof $.fn.customValidationCheck !== 'undefined' && typeof $.fn.customValidationCheck === 'function') {
            validForm = validForm && $.fn.customValidationCheck();
        }
        if (validForm) {
            $(this).find('input:text').each(function(){
                $(this).val($.trim($(this).val()));
            });
            var jsonData = JSON.stringify($('form').serializeEncounterForm());
            htmlDataStore.createPersonAndDiscardHTML(jsonData);
        } else {
            addValidationMessage();
        }
    };

    document.autoSaveForm = function () {
        save("incomplete", true);
    };

    document.saveDraft = function () {
        save("incomplete", false);
        return false;
    };

    document.preSaveScheduledTasks = [];

    var runPreSaveScheduledTasks = function(){
        console.log("Resolving registered tasks");
        var nextTask = null;
        $.each(document.preSaveScheduledTasks, function(k,v){
            if(nextTask != null){
                new Promise(function(resolve, reject){
                    nextTask();
                    resolve();
                }).then(function(){
                    nextTask = v;
                });
            } else {
                nextTask = v;
            }
        });
        nextTask();
    }

    var save = function (status, keepFormOpen) {
        new Promise(function(resolve, reject){
            if(document.preSaveScheduledTasks.length > 0 && status == "complete"){
                runPreSaveScheduledTasks();
            }
            resolve();
        }).then(function(){
            if(status=="complete"){
                /*Start of populating data entry completion timestamp before serializing the form*/
                if($('.dataEntryCompletionTimeStamp').length){
                    var date = new Date();
                    $('.dataEntryCompletionTimeStamp').val(date.getTime());
                }
                /*Start of populating data entry completion timestamp*/
            }
            var jsonData = JSON.stringify($('form').serializeEncounterForm());
            htmlDataStore.saveHTML(jsonData, status, keepFormOpen);
        });
    };

    var addValidationMessage = function () {
        var validationError = $('#validation-error');
        var formValidationFailureMessage =
            htmlDataStore.getStringResource("hint_form_validation_failure");
        if (validationError.length == 0) {
            $('form').prepend(
                '<div class="error" id="validation-error">' +formValidationFailureMessage + '</div>'
            );
        } else {
            validationError.html(formValidationFailureMessage);
        }
        $('html, body').animate({scrollTop: 0}, 'slow');
    };

    var removeValidationMessage = function () {
        $('#validation-error').remove();
    };
    /* End - Function to save the form */

    /*Start- BarCode Functionality*/

    /* Called by the Activity WebViewActivity*/
    document.populateBarCode = function (jsonString) {
        $.each(jsonString, function (key, value) {
            var $inputField = $("input[name='" + key + "']");
            $inputField.val(value);
            $inputField.trigger('change');  //Need this to trigger the event so AMRS id gets populated.
        });
    };

    $('.barcode_btn').click(function () {
        barCodeComponent.startBarCodeIntent($(this).parent().find("input[type='text']").attr('name'));
    });

    /*End- BarCode Functionality*/

    /*Start- Imaging Functionality*/

    /* Called by the Activity WebViewActivity*/
    document.populateImage = function (sectionName, jsonString) {
        var $parent = $('div[data-name="' + sectionName + '"]');
        $.each(jsonString, function (key, value) {
            var $inputField = $parent.find("input[name='" + key + "']");
            $inputField.val(value);
            $inputField.trigger('change');  //Need this to trigger the event so image gets populated.
        });
    };

    $('.image_btn').click(function () {
        imagingComponent.startImageIntent($(this).parent().parent().attr('data-name'),
            $(this).parent().find("input[type='hidden']").attr('name'),
            $(this).parent().find("input[type='hidden']").val(),
            $(this).parent().find("input[type='text']").attr('name'),
            $(this).parent().find("input[type='text']").val(),
            $("input[name='encounter.form_uuid']").val());
    });

    /*End- Imaging Functionality*/

    /*Start- Audio Capture Functionality*/

    /* Called by the Activity WebViewActivity*/
    document.populateAudio = function (sectionName, jsonString) {
        var $parent = $('div[data-name="' + sectionName + '"]');
        $.each(jsonString, function (key, value) {
            var $inputField = $parent.find("input[name='" + key + "']");
            $inputField.val(value);
            $inputField.trigger('change');  //Need this to trigger the event so audio gets populated.
        });
    };

    $('.audio_record_btn').click(function () {
        audioComponent.startAudioIntent($(this).parent().parent().attr('data-name'),
            $(this).parent().find("input[type='hidden']").attr('name'),
            $(this).parent().find("input[type='hidden']").val(),
            $(this).parent().find("input[type='text']").attr('name'),
            $(this).parent().find("input[type='text']").val(),
            $("input[name='encounter.form_uuid']").val());
    });

    /*End- Audio Capture Functionality*/

    /*Start- Video Capture Functionality*/

    /* Called by the Activity WebViewActivity*/
    document.populateVideo = function (sectionName, jsonString) {
        var $parent = $('div[data-name="' + sectionName + '"]');
        $.each(jsonString, function (key, value) {
            var $inputField = $parent.find("input[name='" + key + "']");
            $inputField.val(value);
            $inputField.trigger('change');  //Need this to trigger the event so video gets populated.
        });
    };

    $('.video_record_btn').click(function () {
        videoComponent.startVideoIntent($(this).parent().parent().attr('data-name'),
            $(this).parent().find("input[type='hidden']").attr('name'),
            $(this).parent().find("input[type='hidden']").val(),
            $(this).parent().find("input[type='text']").attr('name'),
            $(this).parent().find("input[type='text']").val(),
            $("input[name='encounter.form_uuid']").val());
    });

    /*End- Video Capture Functionality*/

    /* Start - Play video in form */
    $('.video-player').click(function () {
        videoComponent.openVideo($(this).attr('data-video'));
    });
    /* End - Play video in form */

    /* Start - view Image in form */
    $('.image-player').click(function () {
        imagingComponent.openImage($(this).attr('data-image'));
    });
    /* End - view Image in form */


    /* Start - Initialize jQuery DatePicker */

    $('.datepicker').datepicker({
        dateFormat: dateFormat,
        changeMonth: true,
        changeYear: true
    });

    /* End - Initialize jQuery DatePicker */

    /*Start - Initialize jQuery DateTimePicker */
    if ($.fn.datetimepicker) {
        $('.datetimepicker').datetimepicker({
            format:'dd-mm-yyyy hh:ii',
            changeMonth: true,
            changeYear: true,
            step : 5,
            autoclose : true
        });
        var dt = new Date();
        var time = dt.getHours() + ":" + dt.getMinutes();
        var dateFormat = "dd-mm-yy";
        var currentDate = $.datepicker.formatDate(dateFormat, new Date());

        var encounterDatetime = $('#encounter\\.encounter_datetime');
        if ($(encounterDatetime).val() == "") {
            $(encounterDatetime).val(currentDate+' '+time);
        }
    }
    /*End - Initialize jQuery DateTimePicker */

    /* Start - CheckDigit algorithm Source: https://wiki.openmrs.org/display/docs/Check+Digit+Algorithm */
    var checkDigitValidationFailureMessage = "";
    $.validator.addMethod("checkDigit", function (value, element) {
            checkDigitValidationFailureMessage = htmlDataStore.getStringResource("hint_check_digit_validation_failure");
            var num = value.split('-');
            if (num.length != 2) {
                return false;
            }
            return $.fn.luhnCheckDigit(num[0]) == num[1];
        }, checkDigitValidationFailureMessage
    );

    // attach 'checkDigit' class to perform validation.
    jQuery.validator.addClassRules({
        checkDigit: {checkDigit: true}
    });

    $.fn.luhnCheckDigit = function (number) {
        var validChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVYWXZ_";
        number = number.toUpperCase().trim();
        var sum = 0;
        for (var i = 0; i < number.length; i++) {
            var ch = number.charAt(number.length - i - 1);
            if (validChars.indexOf(ch) < 0) {
                return false;
            }
            var digit = ch.charCodeAt(0) - 48;
            var weight;
            if (i % 2 == 0) {
                weight = (2 * digit) - parseInt(digit / 5) * 9;
            }
            else {
                weight = digit;
            }
            sum += weight;
        }
        sum = Math.abs(sum) + 10;
        return (10 - (sum % 10)) % 10;
    };

    /* End - CheckDigit Algorithm */

    $.fn.isNotRequiredAndEmpty = function (value, element) {
        if (!$(element).attr('required') && value == '') return true;
    };

    // attach 'checkDigit' class to perform validation.
    jQuery.validator.addClassRules({
        checkDigit: {checkDigit: true}
    });

    /* Start - Checking that the current date is not in the future */
    var currentOrPastDateValidationFailureMessage = "";
    $.validator.addMethod("nonFutureDate", function (value, element) {
            if ($.fn.isNotRequiredAndEmpty(value, element)) return true;
            var pattern = /(\d{2})-(\d{2})-(\d{4})/g;
            var matches = pattern.exec(value);
            if (matches == null || matches.length < 4) {
                return false;
            }
            var enteredDate = new Date(matches[3], matches[2] - 1, matches[1]);
            var reference = new Date();
            var today = new Date(reference.getFullYear(), reference.getMonth(), reference.getDate());
            currentOrPastDateValidationFailureMessage =
                htmlDataStore.getStringResource("hint_current_or_past_date_validation_failure");
            return enteredDate <= today;
        }, currentOrPastDateValidationFailureMessage
    );

    // attach 'nonFutureDate' class to perform validation.
    jQuery.validator.addClassRules({
        nonFutureDate: {nonFutureDate: true}
    });

    /* End - nonFutureDate*/

    /* Start - Checking that the current date is not after encounter date */
    $.validator.addMethod("nonPostEncounterDate", function (value, element) {
            if ($.fn.isNotRequiredAndEmpty(value, element)) return true;
            var pattern = /(\d{2})-(\d{2})-(\d{4})/g;
            var matches = pattern.exec(value);
            var enteredDate = new Date(matches[3], matches[2] - 1, matches[1]);
            var today = new Date();

            pattern = /(\d{2})-(\d{2})-(\d{4})/;
            matches=pattern.exec($('#encounter\\.encounter_datetime').val());
            if(matches == null){
                return true;
            }
            var encounterDate = new Date(matches[3], matches[2] - 1, matches[1]);
            return enteredDate <= encounterDate;
        }, "Please enter a date prior to encounter date."
    );

    // attach 'checkFutureDate' class to perform validation.
    jQuery.validator.addClassRules({
        nonPostEncounterDate: { nonPostEncounterDate: true }
    });

    $('.nonPostEncounterDate').change(function () {
        $(this).valid();
    });
    /* End - nonPostEncounterDate*/

    /* Start - Checking that the current date is in the future */

    var futureDateValidationFailureMessage = "";
    $.validator.addMethod("checkFutureDate", function (value, element) {
            if ($.fn.isNotRequiredAndEmpty(value, element)) return true;
            var pattern = /(\d{2})-(\d{2})-(\d{4})/g;
            var matches = pattern.exec(value);
            if (matches == null || matches.length < 4) {
                return false;
            }
            var enteredDate = new Date(matches[3], matches[2] - 1, matches[1]);
            var reference = new Date();
            var today = new Date(reference.getFullYear(), reference.getMonth(), reference.getDate());
            futureDateValidationFailureMessage = htmlDataStore.getStringResource("hint_future_date_validation_failure");
            return enteredDate > today;
        }, futureDateValidationFailureMessage
    );

    // attach 'checkFutureDate' class to perform validation.
    jQuery.validator.addClassRules({
        checkFutureDate: {checkFutureDate: true}
    });

    /* End - checkFutureDate*/

    /* Start - Checking that the entered value is a valid phone number */
    var phoneNumberValidationFailureMessage = "";
    $.validator.addMethod("phoneNumber", function (value, element) {
            phoneNumberValidationFailureMessage = htmlDataStore.getStringResource("hint_phone_number_validation_failure");
            if ($.fn.isNotRequiredAndEmpty(value, element)){
                return true;
            }
            var validPhoneNumberRegex = /^[\\+]?[(]?[0-9]{3}[)]?[-\\s\\.]?[0-9]{3}[-\\s\\.]?[0-9]{4,6}$/
            return validPhoneNumberRegex.test(value);
        }, phoneNumberValidationFailureMessage
    );

    // attach 'phoneNumber' class to perform validation.
    jQuery.validator.addClassRules({
        phoneNumber: {phoneNumber: true}
    });

    /* End - phoneNumber*/

    /* Start - checkNoneSelectedAlone*/

    $.fn.checkNoneSelectedAlone = function (nameArray) {
        var $validator = $('form').validate();
        var errors = {};
        var result = true;
        var optionNoneExclusiveSelectionValidationFailureMessage =
            htmlDataStore.getStringResource("hint_option_none_exclusive_selection_validation_failure");
        $.each(nameArray, function (i, element) {
            var fieldSetElem = $('fieldset[name="' + element + '"]');
            result = isValidForNoneSelection(fieldSetElem);
            if (!result) {
                errors[element] = optionNoneExclusiveSelectionValidationFailureMessage;
            }
        });
        if (!result) {
            $validator.showErrors(errors);
        }
        return result;
    };

    var isValidForNoneSelection = function (element) {
        var $fieldset = $(element);
        if ($fieldset.prop('tagName') != 'FIELDSET') {
            return true;
        }
        var valid = true;
        var totalSelected = $fieldset.find('input:checkbox:checked').length;
        $.each($fieldset.find('input:checkbox:checked'), function (i, cBoxElement) {
            if (($(cBoxElement).val() == 'none' || $(cBoxElement).val() == '1107^NONE^99DCT') &&
                totalSelected > 1) {
                valid = false;
            }
        });
        return valid;
    };

    /* End - checkNoneSelectedAlone*/

    $.fn.getTempBirthDate = function (years) {
        var currentYear = new Date().getFullYear();
        var estimatedDate = new Date(currentYear - parseInt(years), 0, 1);
        return $.datepicker.formatDate(dateFormat, estimatedDate);
    };

    $.fn.getTempBirthDateFromYearsAndMonths = function (years, months) {
        var estimatedDate = new Date();
        estimatedDate.setDate(1);
        estimatedDate.setMonth( estimatedDate.getMonth() - parseInt(months) );
        estimatedDate.setFullYear( estimatedDate.getFullYear() - parseInt(years));
        return $.datepicker.formatDate(dateFormat, estimatedDate);
    };

    $.fn.getAgeInYears = function (birthDateString) {
        var birthDate = new Date(birthDateString);
        var today = new Date();
        var milliSecondsInAYear = 1000 * 60 * 60 * 24 * 365.26;
        return (today - birthDate) / milliSecondsInAYear;
    };

    $.fn.getAgeInDays = function (birthDateString) {
        var birthDate = new Date(birthDateString);
        var today = new Date();
        var milliSecondsInADay = 1000 * 60 * 60 * 24;
        return (today - birthDate) / milliSecondsInADay;
    };

    /* Start - Used for Sub-Forms */

    $('.repeat')
        .append("<input class='btn btn-primary add_section pull-left' type='button' value='+'/>")
        .append("<input class='btn btn-primary remove_section pull-right' type='button' value='-'/><span class='clear'></span>");

    /* Modified by Sam to create active cloned buttons and separate sections by data-name */
    $(document.body).on('click', '.add_section', function () {
        var $clonedSection = $(this).parent().clone(true).insertAfter($(this).parent());
        var parentName = $clonedSection.attr("id");

        /* Get largest suffix so far */
        var _id = $(this).parent().attr('id');
        var $repeatedSections = $("." + _id);
        var suffixInt = 0;
        $.each($repeatedSections, function (i, repeatedSection) {
            var idnr = $(repeatedSection).attr("data-name").match(/\d+$/);
            if (parseInt(idnr) > parseInt(suffixInt)) {
                suffixInt = idnr;
            }
        });
        if (parseInt(suffixInt) == 0) {
            parentName += "1";
        } else {
            parentName += parseInt(suffixInt) + 1;
        }
        $clonedSection.attr("data-name", parentName);
        $clonedSection.find('input.datepicker').attr("id", "").removeClass('hasDatepicker').unbind().datepicker({
            dateFormat: dateFormat,
            changeMonth: true,
            changeYear: true
        });
        document.clearValuesOnClonedFields($clonedSection);
        if(document.isMaxRepeatsReached($clonedSection)){
            $('.add_section').prop('disabled', true);
        }
        var showErrorMessageIfInvalid = false;
        document.multipleSectionRepeatsValidationCheck($("." + $clonedSection.attr("id")),showErrorMessageIfInvalid);
    });

    $(document.body).on('click', '.remove_section', function () {
        document.removeIfRepeatedSection($(this));
    });

    document.clearValuesOnClonedFields = function ($clonedSection){
        $clonedSection.find(':input:not(:button):not(:radio):not(:checkbox)').val('');
        $clonedSection.find(':input:not(:button)').trigger('change');
        $clonedSection.find(':radio').prop('checked',false);
        $clonedSection.find(':radio').trigger('change');
        $clonedSection.find(':checkbox').prop('checked',false);
        $clonedSection.find(':checkbox').trigger('change');
    }

    document.removeIfRepeatedSection = function($element){
        var $parent = $element.parent();
        var _id = $parent.attr('id');
        if ($parent.parent().find("." + _id).length > 1) {
            $parent.remove();
        }
    }

    /* remove any nested cloned subsections.
        This function is reusable with custom cloning
        @param removalClickElementSelector is optional*/
    document.removeRepeatedSubSections = function($parentSection, removalClickElementSelector){
        if(typeof removalButtonClass === "undefined"){
            removalClickElementSelector = '.remove_section';
        }
        var $repeated_subsection = $parentSection.find(removalClickElementSelector);
        if ($repeated_subsection !== undefined && $repeated_subsection instanceof Array) {
            $.each($repeated_subsection,
                function(key,section){
                    $.each($(section),
                        function(k,v){
                            document.removeIfRepeatedSection($(v));
                        }
                    );
                }
            );
        } else if($repeated_subsection !== undefined) {
            $.each($repeated_subsection,
                function(k,v){
                    document.removeIfRepeatedSection($(v));
                }
            );
        }
    }

    document.isMaxRepeatsReached = function($section){
        var _id = $section.attr('id');
        var sectionCount = $section.parent().find("." + _id).length;
        var repeatParams = $section.attr('data-repeatparams');
        if(repeatParams != null){
            repeatParams = $.parseJSON(repeatParams);
            if(typeof repeatParams.max != 'undefined'){
                var maxRepeats=repeatParams.max;
                return sectionCount >= maxRepeats;
            }
        } else {
            return false;
        }
    }

    document.getCurrentAndMinimumRepeats = function($section){
        var _id = $section.attr('id');
        var sectionCount = $section.parent().find("." + _id).length;
        var repeatParams = $section.attr('data-repeatparams');
        var minRepeatsParams = null;
        if(repeatParams != null){
            repeatParams = $.parseJSON(repeatParams);
            if(typeof repeatParams.min != 'undefined'){
                var minRepeats=repeatParams.min;
                minRepeatsParams = {};
                minRepeatsParams['current_repeats'] = sectionCount;
                minRepeatsParams['minimum_repeats'] = minRepeats;
            }
        }
        return minRepeatsParams;
    }


    document.repeatValidationCheck = function($section,showErrorMessageIfInvalid) {
        var validSection = true;
        var minRepeatsParams = document.getCurrentAndMinimumRepeats($section);
        var minimumRepeatsValidationFailureMessage =
            htmlDataStore.getStringResource("hint_repeating_section_minimum_repeats_validation_failure");
        if(minRepeatsParams != null && minRepeatsParams.current_repeats < minRepeatsParams.minimum_repeats){
            validSection = false;
            if(showErrorMessageIfInvalid){
                var $validationError = $section.find('.repeat-error');
                if($validationError.length>0){
                    $validationError.html(minimumRepeatsValidationFailureMessage + " " + minRepeatsParams.minimum_repeats);
                    $validationError.show();
                } else {
                    $validationError = '<div class="error repeat-error"> ' + minimumRepeatsValidationFailureMessage
                        + " " + minRepeatsParams.minimum_repeats + '. </div>';
                    $section.append($validationError);
                }
            }
        } else {
            var $validationError = $section.find('.repeat-error');
            if($validationError.length>0){
                $validationError.hide();
            }
        }
        return validSection;
    }
    document.multipleSectionRepeatsValidationCheck = function($selector, showErrorMessageIfInvalid) {
        var validForm = true;
        $.each($selector,function(k,v){
            if(validForm){
                validForm = document.repeatValidationCheck($(v),showErrorMessageIfInvalid);
            } else {
                document.repeatValidationCheck($(v),showErrorMessageIfInvalid);
            }
        });
        return validForm;
    }


    /* End - Used for Sub-Forms */

    /* Start - Checks that a field is a decimal */

    $.validator.addClassRules({
        isDecimal: {
            number: true
        }
    });

    /* End - Checks that a field is a decimal */

    /* Start - JS to Prepopulate Data in the Form */
    var populateDataConcepts = function ($div, value) {
        $.each(value, function (k, v) {
            if (v instanceof Array) {
                var $elements = $div.find('[data-concept="' + k + '"]');
                if ($elements.length < v.length) {
                    $.each(v, function (i, valueElement) {
                        if (i == 0) {
                            $.each($elements, function(i, element) {
                                applyValue(element, valueElement);
                            });
                        } else {
                            var $div = $elements.closest('.repeat, .custom-repeat');
                            var $clonedDiv = $div.clone(true);
                            $div.after($clonedDiv);
                            $elements = $clonedDiv.find('[data-concept="' + k + '"]');
                            $.each($elements, function(i, element) {
                                applyValue(element, valueElement);
                            });
                        }
                    });
                } else if ($elements.length == v.length) {
                    $.each(v, function (i, valueElement) {
                        applyValue($elements[i], valueElement);
                    });
                } else {
                    $.each(v, function (i, valueElement) {
                        $.each($elements, function(i, element) {
                            applyValue(element, valueElement);
                        });
                    });
                }

            }else if (v instanceof Object){
                if(v.obs_value !== undefined && v.obs_datetime !== undefined){
                    var obs_elements = $div.find('[data-concept="' + k + '"]');
                    $.each(obs_elements, function(i, element) {
                        applyValue(element, v.obs_value);
                    });

                    var datetime_element = $div.find('[data-obsdatetimefor="' + obs_elements.attr('name') + '"]');
                    applyValue(datetime_element, v.obs_datetime);
                }else if(v.obs_value !==undefined && v.obs_valuetext || v.obs_value !==undefined && v.obs_valuecoded || v.obs_value !==undefined && v.obs_valueuuid){
                    var obs_elements = $div.find('[data-concept="' + k + '"]');
                    $.each(obs_elements, function(i, element) {
                        applyValue(element, v.obs_value);
                    });

                    var value_text_element = $div.find('[data-obsvaluetextfor="' + obs_elements.attr('name') + '"]');
                    applyValue(value_text_element, v.obs_valuetext);

                    var value_coded_element = $div.find('[data-obsvaluecodedfor="' + obs_elements.attr('name') + '"]');
                    applyValue(value_coded_element, v.obs_valuecoded);

                    var value_uuid_element = $div.find('[data-obsvalueuuidfor="' + obs_elements.attr('name') + '"]');
                    applyValue(value_uuid_element, v.obs_valueuuid);
                }else{
                    populateObservations($div, v);
                }
            } else {
                var elements = $div.find('[data-concept="' + k + '"]');
                $.each(elements, function (i, element) {
                    applyValue(element, v);
                });
            }
        });
    };

    var populateNonConceptFields = function ($parentDiv, prePopulateJson) {
        $.each(prePopulateJson, function (key, value) {
            var $elements = $parentDiv.find('[name="' + key + '"]');
            if (value instanceof Array) {
                if ($elements.length < value.length) {
                    $.each(value, function (i, valueElement) {
                        if (i == 0) {
                            $.each($elements, function(i, element) {
                                applyValue(element, valueElement);
                            });
                        } else {
                            var $div = $elements.closest('.repeat, .custom-repeat');
                            var $clonedDiv = $div.clone(true);
                            $div.after($clonedDiv);
                            $elements = $clonedDiv.find('[name="' + key + '"]');
                            $.each($elements, function(i, element) {
                                applyValue(element, valueElement);
                            });
                        }
                    });
                } else if ($elements.length == value.length) {
                    $.each(value, function (i, valueElement) {
                        applyValue($elements[i], valueElement);
                    });
                } else {
                    $.each(value, function (i, valueElement) {
                        $.each($elements, function(i, element) {
                            applyValue(element, valueElement);
                        });
                    });
                }
            } else if (value instanceof Object){
                populateNonObservations($parentDiv, value);
            } else {
                $.each($elements, function (i, element) {
                    applyValue(element, value);
                });
            }
        });
    };

    var applyValue = function (element, value) {
        if ($(element).is(':checkbox') || $(element).is(':radio')) {
            if ($(element).val() == value) {
                $(element).prop('checked', true);
            }
        } else {
            $(element).val(value);
        }
    };

    var populateNonObservations = function ($parentDiv, prePopulateJson) {
        $.each(prePopulateJson, function (key, value) {
            if (value instanceof Object) {
                // check if this is a grouping observation.
                var $div = $parentDiv.find('div[data-group="' + key + '"]');
                if ($div.length > 0) {
                    // we are dealing with grouping
                    if (value instanceof Array) {
                        $.each(value, function (i, element) {
                            if (i == 0) {
                                populateNonConceptFields($div, element);
                            } else {
                                var $clonedDiv = $div.clone(true);
                                document.clearValuesOnClonedFields($clonedDiv);
                                document.removeRepeatedSubSections($clonedDiv);
                                populateNonConceptFields($clonedDiv, element);
                                $div.after($clonedDiv);
                            }
                        });
                    } else {
                        populateNonConceptFields($div, value);
                    }
                } else {
                    // we are not dealing with repeating
                    if (value instanceof Array) {
                        var elements = $parentDiv.find('[data-group="' + key + '"]');
                        if (elements.length < value.length) {
                            $.each(value, function (i, valueElement) {
                                if (i == 0) {
                                    $.each(elements, function (i, element) {
                                        applyValue(element, valueElement);
                                    });
                                } else {
                                    var $div = $(elements).closest('.repeat, .custom-repeat');
                                    var $clonedDiv = $div.clone(true);
                                    $div.after($clonedDiv);
                                    elements = $clonedDiv.find('[data-group="' + key + '"]');
                                    $.each(elements, function (i, element) {
                                        applyValue(element, valueElement);
                                    });
                                }
                            });
                        } else {
                            $.each(value, function (i, valueElement) {
                                $.each(elements, function (i, element) {
                                    applyValue(element, valueElement);
                                });
                            });
                        }
                    } else {
                        populateNonConceptFields($div, value);
                    }
                }
            }
            else {
                var $elements = $parentDiv.find('[name="' + key + '"]');
                $.each($elements, function (i, element) {
                    applyValue(element, value);
                });
            }
        });
    };

    var populateObservations = function ($parentDiv, prePopulateJson) {
        $.each(prePopulateJson, function (key, value) {
            if (value instanceof Object) {
                // check if this is a grouping observation.
                var $div = $parentDiv.find('div[data-concept="' + key + '"]');
                if ($div.length > 0) {
                    // we are dealing with grouping
                    if (value instanceof Array) {
                        $.each(value, function (i, element) {
                            if (i == 0) {
                                populateDataConcepts($div, element);
                            } else {
                                var $clonedDiv = $div.clone(true);
                                document.clearValuesOnClonedFields($clonedDiv);
                                document.removeRepeatedSubSections($clonedDiv);
                                populateDataConcepts($clonedDiv, element);
                                $div.after($clonedDiv);
                            }
                        });
                    } else {
                        populateDataConcepts($div, value);
                    }
                } else {
                    // we are not dealing with repeating
                    if (value instanceof Array) {
                        var elements = $parentDiv.find('[data-concept="' + key + '"]');
                        if (elements.length < value.length) {
                            $.each(value, function (i, valueElement) {
                                if (i == 0) {
                                    $.each(elements, function (i, element) {
                                        applyValue(element, valueElement);
                                    });
                                } else {
                                    var $div = $(elements).closest('.repeat, .custom-repeat');
                                    var $clonedDiv = $div.clone(true);
                                    $div.after($clonedDiv);
                                    elements = $clonedDiv.find('[data-concept="' + key + '"]');
                                    $.each(elements, function (i, element) {
                                        applyValue(element, valueElement);
                                    });
                                }
                            });
                        } else {
                            $.each(value, function (i, valueElement) {
                                $.each(elements, function (i, element) {
                                    applyValue(element, valueElement);
                                });
                            });
                        }
                    } else if (value.obs_value !== undefined && value.obs_datetime !== undefined) {
                        var obs_elements = $parentDiv.find('[data-concept="' + key + '"]');
                        $.each(obs_elements, function (i, element) {
                            applyValue(element, value.obs_value);
                        });


                        var datetime_element = $parentDiv.find('[data-obsdatetimefor="' + obs_elements.attr('name') + '"]');
                        applyValue(datetime_element, value.obs_datetime);
                    }else if(value.obs_value !==undefined && value.obs_valuetext || value.obs_value !==undefined && value.obs_valuecoded || value.obs_value !==undefined && value.obs_valueuuid){
                        var obs_elements = $parentDiv.find('[data-concept="' + key + '"]');
                        $.each(obs_elements, function(i, element) {
                            applyValue(element, value.obs_value);
                        });

                        var value_text_element = $parentDiv.find('[data-obsvaluetextfor="' + obs_elements.attr('name') + '"]');
                        applyValue(value_text_element, value.obs_valuetext);

                        var value_coded_element = $parentDiv.find('[data-obsvaluecodedfor="' + obs_elements.attr('name') + '"]');
                        applyValue(value_coded_element, value.obs_valuecoded);

                        var value_uuid_element = $parentDiv.find('[data-obsvalueuuidfor="' + obs_elements.attr('name') + '"]');
                        applyValue(value_uuid_element, value.obs_valueuuid);
                    } else {
                        populateDataConcepts($div, value);
                    }
                }
            }
            else {
                var $elements = $parentDiv.find('[data-concept="' + key + '"]');
                $.each($elements, function (i, element) {
                    applyValue(element, value);
                });
            }
        });
    };

    var prePopulateData = $.trim($('#pre_populate_data').html());

    if (prePopulateData != '') {
        console.time("Starting population");
        var prePopulateJSON = JSON.parse(prePopulateData);
        $.each(prePopulateJSON, function (key, value) {
            if (key === 'observation' || key === 'index_obs') {
                populateObservations($('form'),value);
            } else {
                populateNonObservations($('form'),value);
            }
        });
        console.timeEnd("Starting population");
    }
    /* End - JS to Prepopulate Data in the Form */

    /* Start - Code to Serialize form along with Data-Concepts */
    $.fn.serializeEncounterForm = function () {
        //construct array of obs_datetime for use while serializing concepts
        setObsDatetimeArray(this);
        setObsValueTextArray(this);
        setObsValueCodedArray(this);
        setObsValueUuidArray(this);
        setObsCommentArray(this);
        setFormUuidToAddObsForArray(this);
        var jsonResult = $.extend({}, serializeNonConceptElements(this), serializeNestedNonConceptElements(this),
            serializeConcepts(this), serializeNestedConcepts(this));
        var completeObject = {};
        var defaultKey = "observation";
        $.each(jsonResult, function (k, v) {
            var key = defaultKey;
            var dotIndex = k.indexOf(".");
            if (dotIndex >= 0) {
                key = k.substr(0, k.indexOf("."));
                if(key == 'index_obs'){
                    k = k.substr(k.indexOf(".")+1, k.length-1);
                }
            }
            if (key !== "obs_datetime") {
                var objects = completeObject[key];
                if (objects === undefined) {
                    objects = {};
                    completeObject[key] = objects;
                }
                objects[k] = v;
            }
        });
        return completeObject;
    };

    var serializeNonConceptElements = function ($form) {
        var object = {};
        var $inputElements = $form.find('[name]').not('[data-concept]');
        $.each($inputElements, function (i, element) {
            var $closestElement = $(element).closest('.section, .group-set', $form);
            if ($form.is($closestElement) || $closestElement.attr('data-group') == undefined ) {
                if ($(element).is(':checkbox') || $(element).is(':radio')) {
                    if ($(element).is(':checked')) {
                        object = pushIntoArray(object, $(element).attr('name'), $(element).val());
                    }
                } else {
                    object = pushIntoArray(object, $(element).attr('name'), $(element).val());
                }
            }
        });
        return object;
    };

    var serializeNestedNonConceptElements = function ($form) {
        var result = {};
        var allParentDivs = $form.find('div[data-group]').filter(':visible');
        var nestedParentDivs = allParentDivs.find('div[data-group]');
        var rootParentDivs = allParentDivs.not(nestedParentDivs);
        $.each(rootParentDivs, function (i, element) {
            var $childDivs = $(element).find('div[data-group]');
            if($childDivs.length > 0){
                var subResult1 = serializeNestedNonConceptElements($(element));
                var subResult2 = serializeNonConceptElements($(element));
                var subResultCombined = $.extend({}, subResult1,subResult2);
                result = pushIntoArray(result, $(element).attr('data-group'), subResultCombined);
            } else {
                var $allNonConcepts = $(element).find('*[name]');
                result = pushIntoArray(result, $(element).attr('data-group'), jsonifyNonConcepts($allNonConcepts));
            }
        });
        return result;
    }

    var serializeNestedConcepts = function ($form) {
        var result = {};
        var allParentDivs = $form.find('div[data-concept]').filter(shouldInclude);
        var nestedParentDivs = allParentDivs.find('div[data-concept]');
        var rootParentDivs = allParentDivs.not(nestedParentDivs);
        $.each(rootParentDivs, function (i, element) {
            var $childDivs = $(element).find('div[data-concept]');
            if($childDivs.length > 0){
                var subResult1 = serializeNestedConcepts($(element));
                var subResult2 = serializeConcepts($(element));
                var subResultCombined = $.extend({}, subResult1,subResult2);
                result = pushIntoArray(result, $(element).attr('data-concept'), subResultCombined);
            } else if ($(element).hasClass('is-index-obs')){
                var $allConcepts = $(element).find('*[data-concept]');
                result = pushIntoArray(result, 'index_obs.'+$(element).attr('data-concept'), jsonifyConcepts($allConcepts, false));
            } else {
                var $allConcepts = $(element).find('*[data-concept]');
                result = pushIntoArray(result, $(element).attr('data-concept'), jsonifyConcepts($allConcepts, true));
            }
        });
        return result;
    };

    function shouldInclude (index, element) {
        var shouldInclude = $(element).is(':visible');
        if (!shouldInclude) {
            var media = $(element).attr("name");
            var mediaValue = media && media.indexOf("consultation");
            if(mediaValue > -1){
                shouldInclude = true;
            }

            var classes = $(element).attr('class');
            if(typeof classes != "undefined"){
                if(classes.indexOf("serializable") > -1){
                    shouldInclude = true;
                }
            }
        }
        return shouldInclude;
    }

    var serializeConcepts = function ($form) {
        var object = {};
        var allConcepts = $form.find('*[data-concept]');
        allConcepts = allConcepts.filter(shouldInclude);
        $.each(allConcepts, function (i, element) {
            var $closestElement = $(element).closest('.section, .concept-set', $form);
            if ($form.is($closestElement) || $closestElement.attr('data-concept') == undefined ) {
                var jsonifiedConcepts = jsonifyConcepts($(element, true));
                if (JSON.stringify(jsonifiedConcepts) != '{}' && jsonifiedConcepts != "") {
                    $.each(jsonifiedConcepts, function (key, value) {
                        if (object[key] !== undefined) {
                            if (!object[key].push) {
                                object[key] = [object[key]];
                            }
                            object[key].push(value || '');
                        } else {
                            object[key] = value || '';
                        }
                    });
                }
            }
        });
        return object;
    };

    var jsonifyConcepts = function ($allConcepts, codeIndexPatientObsIfAvailable) {
        var o = {};
        $.each($allConcepts, function (i, element) {
            var obs_datetime = getObsDatetime(element);
            var obs_valuetext = getObsValueText(element);
            var obs_valuecoded = getObsValueCoded(element);
            var obs_valueuuid = getObsValueUuid(element);
            var obs_comment = getObsComment(element);
            var obs_formuuidtoaddobsfor = getFormUuidToAddObsFor(element);
            if ($(element).is(':checkbox') || $(element).is(':radio')) {
                if ($(element).is(':checked')) {
                    if (obs_datetime != '') {
                        var v = {};
                        var obs_value = $(element).val();
                        if (JSON.stringify(obs_value) != '{}' && obs_value != "") {
                            v = pushIntoArray(v, 'obs_value', obs_value);
                            v = pushIntoArray(v, 'obs_datetime', obs_datetime);
                            o = pushIntoArray(o, $(element).attr('data-concept'), v);
                        }
                    } else if (obs_valuetext != '' || obs_valuecoded != '' || obs_valueuuid != '' || obs_comment != '' || obs_formuuidtoaddobsfor != ''){
                        var v = {};
                        var obs_value = $(element).val();
                        if (JSON.stringify(obs_value) != '{}' && obs_value != "") {
                            v = pushIntoArray(v, 'obs_value', obs_value);
                            v = pushIntoArray(v, 'obs_valuetext', obs_valuetext);
                            v = pushIntoArray(v, 'obs_valuecoded', obs_valuecoded);
                            v = pushIntoArray(v, 'obs_valueuuid', obs_valueuuid);
                            v = pushIntoArray(v, 'obs_comment', obs_comment);
                            v = pushIntoArray(v, 'obs_formuuidtoaddobsfor', obs_formuuidtoaddobsfor);
                            o = pushIntoArray(o, $(element).attr('data-concept'), v);
                        }
                    } else if ($(element).hasClass('is-index-obs')) {
                        o = pushIntoArray(o, 'index_obs.'+$(element).attr('data-concept'), $(element).val());
                    } else {
                        o = pushIntoArray(o, $(element).attr('data-concept'), $(element).val());
                    }
                }
            } else {
                if (obs_datetime != '') {
                    var v = {};
                    var obs_value = $(element).val();
                    if (JSON.stringify(obs_value) != '{}' && obs_value != "") {
                        v = pushIntoArray(v, 'obs_value', obs_value);
                        v = pushIntoArray(v, 'obs_datetime', obs_datetime);
                        o = pushIntoArray(o, $(element).attr('data-concept'), v);
                    }
                } else if (obs_valuetext != '' || obs_valuecoded != '' || obs_valueuuid != '' || obs_comment != '' || obs_formuuidtoaddobsfor != ''){
                    var v = {};
                    var obs_value = $(element).val();
                    if (JSON.stringify(obs_value) != '{}' && obs_value != "") {
                        v = pushIntoArray(v, 'obs_value', obs_value);
                        v = pushIntoArray(v, 'obs_valuetext', obs_valuetext);
                        v = pushIntoArray(v, 'obs_valuecoded', obs_valuecoded);
                        v = pushIntoArray(v, 'obs_valueuuid', obs_valueuuid);
                        v = pushIntoArray(v, 'obs_comment', obs_comment);
                        v = pushIntoArray(v, 'obs_formuuidtoaddobsfor', obs_formuuidtoaddobsfor);
                        o = pushIntoArray(o, $(element).attr('data-concept'), v);
                    }
                } else if ($(element).hasClass('is-index-obs') && codeIndexPatientObsIfAvailable == true){
                    o = pushIntoArray(o, 'index_obs.'+$(element).attr('data-concept'), $(element).val());
                } else {
                    o = pushIntoArray(o, $(element).attr('data-concept'), $(element).val());
                }
            }
        });
        return o;
    };

    var jsonifyNonConcepts = function ($allNonConcepts) {
        var o = {};
        $.each($allNonConcepts, function (i, element) {
            //if element is metadata check whether corresponding value is present
            if(typeof $(element).attr('data-metadata-for') !== 'undefined'){
                var correspondingValueElementName = $(element).attr('data-metadata-for');
                var value = $(element).closest('div[data-group]').find('[name="' + correspondingValueElementName + '"]').val();
                if(value != ''){
                    jsonifyNonConcept(o,element);
                }
            } else {
                jsonifyNonConcept(o,element);
            }
        });
        return o;
    };

    var jsonifyNonConcept = function(object,element){
        if ($(element).is(':checkbox') || $(element).is(':radio')) {
            if ($(element).is(':checked')) {
                object = pushIntoArray(object, $(element).attr('name'), $(element).val());
            }
        } else {
            object = pushIntoArray(object, $(element).attr('name'), $(element).val());
        }
        return object;
    };

    var obsDatetimeArray = null;
    var setObsDatetimeArray = function ($form) {
        obsDatetimeArray = {};
        var obsDatetimeElements = $form.find('*[data-obsdatetimefor]').filter(':visible');
        $.each(obsDatetimeElements, function (i, element) {
            pushIntoArray(obsDatetimeArray, $(element).attr('data-obsdatetimefor'), $(element).val());
        });
    };
    var getObsDatetime = function (element) {
        if (obsDatetimeArray !== null) {
            var elementName = $(element).attr('name');
            if (obsDatetimeArray[elementName] !== undefined) {
                return obsDatetimeArray[elementName];
            }
        }
        return '';
    }

    var obsValueTextArray = null;
    var setObsValueTextArray = function ($form) {
        obsValueTextArray = {};
        var obsValueTextElements = $form.find('*[data-obsvaluetextfor]').filter(':visible');
        $.each(obsValueTextElements, function (i, element) {
            pushIntoArray(obsValueTextArray, $(element).attr('data-obsvaluetextfor'), $(element).val());
        });
    };

    var getObsValueText = function (element) {
        if (obsValueTextArray !== null) {
            var elementName = $(element).attr('name');
            if (obsValueTextArray[elementName] !== undefined) {
                return obsValueTextArray[elementName];
            }
        }
        return '';
    }

    var obsValueCodedArray = null;
    var setObsValueCodedArray = function ($form) {
        obsValueCodedArray = {};
        var obsValueCodedElements = $form.find('*[data-obsvaluecodedfor]').filter(':visible');
        $.each(obsValueCodedElements, function (i, element) {
            if ($(element).is(':checkbox') || $(element).is(':radio')) {
                if ($(element).is(':checked')) {
                    pushIntoArray(obsValueCodedArray, $(element).attr('data-obsvaluecodedfor'), $(element).val());
                }
            }else{
                pushIntoArray(obsValueCodedArray, $(element).attr('data-obsvaluecodedfor'), $(element).val());
            }
        });
    };

    var getObsValueCoded = function (element) {
        if (obsValueCodedArray !== null) {
            var elementName = $(element).attr('name');
            if (obsValueCodedArray[elementName] !== undefined) {
                return obsValueCodedArray[elementName];
            }
        }
        return '';
    }

    var obsValueUuidArray = null;
    var setObsValueUuidArray = function ($form) {
        obsValueUuidArray = {};
        var obsValueUuidElements = $form.find('*[data-obsvalueuuidfor]').filter(':visible');
        $.each(obsValueUuidElements, function (i, element) {
            pushIntoArray(obsValueUuidArray, $(element).attr('data-obsvalueuuidfor'), $(element).val());
        });
    };

    var getObsValueUuid = function (element) {
        if (obsValueUuidArray !== null) {
            var elementName = $(element).attr('name');
            if (obsValueUuidArray[elementName] !== undefined) {
                return obsValueUuidArray[elementName];
            }
        }
        return '';
    }

    var obsCommentArray = null;
    var setObsCommentArray = function ($form) {
        obsCommentArray = {};
        var obsCommentElements = $form.find('*[data-obscommentfor]');
        $.each(obsCommentElements, function (i, element) {
            pushIntoArray(obsCommentArray, $(element).attr('data-obscommentfor'), $(element).val());
        });
    };
    var getObsComment = function (element) {
        if (obsCommentArray !== null) {
            var elementName = $(element).attr('name');
            if (obsCommentArray[elementName] !== undefined) {
                return obsCommentArray[elementName];
            }
        }
        return '';
    }

    var formUuidToAddObsForArray = null;
    var setFormUuidToAddObsForArray = function ($form) {
        formUuidToAddObsForArray = {};
        var formUuidToAddObsForElements = $form.find('*[data-obsformuuidtoaddobsfor]');
        $.each(formUuidToAddObsForElements, function (i, element) {
            pushIntoArray(formUuidToAddObsForArray, $(element).attr('data-obsformuuidtoaddobsfor'), $(element).val());
        });
    };
    var getFormUuidToAddObsFor = function (element) {
        if (formUuidToAddObsForArray !== null) {
            var elementName = $(element).attr('name');
            if (formUuidToAddObsForArray[elementName] !== undefined) {
                return formUuidToAddObsForArray[elementName];
            }
        }
        return '';
    }

    var pushIntoArray = function (object, key, value) {
        if (JSON.stringify(value) == '{}' || value == "") {
            return object;
        }
        if (object[key] !== undefined) {
            if (!object[key].push) {
                object[key] = [object[key]];
            }
            object[key].push(value || '');
        } else {
            object[key] = value || '';
        }
        return object;
    };

    /* End - Code to Serialize form along with Data-Concepts */

    document.setupAutoCompleteData = function (elementName) {
        var dataDictionary = [];
        $.each(locationNamesResults, function (key, locationName) {
            dataDictionary.push({"val": locationName.id, "label": locationName.name});
        });
        document.setupAutoComplete('encounter\\.location_id', dataDictionary);
    };

    //Set up auto complete for an element.(generic, will work with any element that needs auto complete on it)
    document.setupAutoComplete = function (elementName, dataDictionary) {

        $("#" + elementName).autocomplete({
            source: dataDictionary,
            create: function (event, ui) {
                var val = $('input[name=' + elementName + ']').val();
                $.each(dataDictionary, function (i, elem) {
                    if (elem.val == val) {
                        $("#" + elementName).val(elem.label);
                    }
                });
            },
            select: function (event, ui) {
                $('input[name="' + elementName + '"]').val(ui.item.val);
                $("#" + elementName).val(ui.item.label);
                return false;
            }
        });
    };

    document.setupAutoCompleteDataForProvider = function (elementName) {
        var providersDictionary = [];
        $.each(providerNamesResults, function (key, providerName) {
            providersDictionary.push({"val": providerName.identifier, "label": providerName.name});
        });
        document.setupAutoCompleteForProvider('encounter\\.provider_id_select', providersDictionary);
    };

    //Set up auto complete for the provider element.
    document.setupAutoCompleteForProvider = function (elementName, providers) {
        $("#" + elementName).autocomplete({
            source: providers,
            create: function (event, ui) {
                var provider_val = $('input[name="' + elementName + '"]').val();
                $.each(providers, function (i, elem) {
                    if (elem.val == provider_val) {
                        $("#" + elementName).val(elem.label);
                    }
                });
            },
            select: function (event, ui) {
                $('input[name="' + elementName + '"]').val(ui.item.val);
                $("#" + elementName).val(ui.item.label);
                $('#encounter\\.provider_id').val(ui.item.val);
                return false;
            }
        });
    }

    document.setupValidationForProvider = function (value, element) {
        /* Start - Checking that the user entered provider exists in the list of possible providers */
        var listOfProviders = [];
        $.each(providerNamesResults, function (key, providerName) {
            listOfProviders.push({"val": providerName.identifier, "label": providerName.name});
        });
        var encounterProviderValidationFailureMessage =
            htmlDataStore.getStringResource("hint_encounter_provider_validation_failure");
        $.validator.addMethod("validProviderOnly", function (value, element) {

            if ($.fn.isNotRequiredAndEmpty(value, element)) return true;
            var providerEnteredByUser = value;
            for (var i = 0; i < listOfProviders.length; i++) {
                if (providerEnteredByUser == listOfProviders[i].label) {
                    return true;
                }
            }
            return false;
        }, encounterProviderValidationFailureMessage);

        // attach 'validProviderOnly' class to perform validation.
        jQuery.validator.addClassRules({
            "valid-provider-only": {
                validProviderOnly: true
            }
        });
        /* End - validProviderOnly*/
    }

    //Start - Set up auto complete for the person element.
    document.setupAutoCompleteForPerson = function($searchElementSelector, $resultElementSelector, $noResultsElement, $searchLoadingWidget,searchServer) {
        $searchLoadingWidget.hide();

        var yearsRepresentationCharacter = htmlDataStore.getStringResource("general_years_character");
        var monthsRepresentationCharacter = htmlDataStore.getStringResource("general_months_character");
        var daysRepresentationCharacter = htmlDataStore.getStringResource("general_days_character");

        $searchElementSelector.focus(function () {
            $searchElementSelector.autocomplete({
                source: function (request, response) {
                    $searchLoadingWidget.show();
                    var searchTerm = request.term;
                    var searchResults = htmlDataStore.searchPersons(searchTerm, searchServer);
                    searchResults = JSON.parse(searchResults);
                    var listOfPersons = [];
                    $.each(searchResults, function (key, person) {
                        var label = person.name;
                        if(person.sex != undefined && person.sex != ''){
                            label = label + ', ' + person.sex;
                        }
                        if(person.birth_date != undefined && person.birth_date != ''){
                            var parts = person.birth_date.split("-");
                            var birthDate = new Date(parseInt(parts[2]),parseInt(parts[1])-1,parseInt(parts[0]));
                            var years = $.fn.getAgeInYears(birthDate);
                            var age_years = Math.floor(years)
                            var age_months = Math.floor((years-age_years)*12)
                            var month = birthDate.toLocaleString('default', { month: 'short' });
                            var day = birthDate.getDate();
                            var year = birthDate.getFullYear();

                            label = label + ', ' + day + ' ' + month + ' ' + year + ' (';
                            if(person.birthdate_estimated == 'true') {
                                label += '~ ';
                            }

                            if(age_years > 0) {
                                label += age_years+ yearsRepresentationCharacter;
                            }

                            if(age_months > 0) {
                                if(age_years > 0) label +=  ' ';
                                label += age_months + monthsRepresentationCharacter + ')';
                            }

                            if(age_months == 0 && age_years == 0){
                                var age_days = Math.floor($.fn.getAgeInDays(birthDate));
                                label += age_days + daysRepresentationCharacter + ')';
                            }
                        }
                        listOfPersons.push({"val": person.uuid, "label": label});
                    });
                    if(!searchServer || searchTerm.length >=3) {
                        $noResultsElement.val(searchResults.length);
                        $noResultsElement.trigger('change');
                    }

                    $searchLoadingWidget.hide();
                    response(listOfPersons);

                },
                select: function (event, ui) {
                    $searchElementSelector.val(ui.item.label);
                    $resultElementSelector.val(ui.item.val);
                    $resultElementSelector.trigger('change');
                    return false;
                }
            });
        });

        $searchElementSelector.blur(function () {
            $searchLoadingWidget.hide();
            $searchElementSelector.autocomplete("destroy");
        });
    }
    //End - Set up auto complete for the person element.

    document.setupValidationForLocation = function (value, element) {

        var listOfLocations = [];
        $.each(locationNamesResults, function (key, locationName) {
            listOfLocations.push({"val": locationName.id, "label": locationName.name});
        });

        var encounterLocationValidationFailureMessage =
            htmlDataStore.getStringResource("hint_encounter_location_validation_failure");

        /* Start - Checking that the user entered location exists in the list of possible locations */
        $.validator.addMethod("validLocationOnly", function (value, element) {
            if ($.fn.isNotRequiredAndEmpty(value, element)) return true;
            var locationEnteredByUser = $('#encounter\\.location_id').val();
            for (var i = 0; i < listOfLocations.length; i++) {
                if (locationEnteredByUser == listOfLocations[i].label) {
                    return true;
                }
            }
            return false;
        }, encounterLocationValidationFailureMessage);

    }

    // attach 'validLocationOnly' class to perform validation.
    jQuery.validator.addClassRules({
        "valid-location-only": {validLocationOnly: true}
    });
    /* End - validLocationOnly*/

    document.setupValidationForConsultation = function (value, element, listOfConsultants) {
        var consultationConsultantValidationFailureMessage =
            htmlDataStore.getStringResource("hint_consultation_consultant_validation_failure");
        /* Start - Checking that the user entered consultant exists in the list of possible consultant */
        $.validator.addMethod("validConsultantOnly", function (value, element) {
            if ($.fn.isNotRequiredAndEmpty(value, element)) return true;
            var locationEnteredByUser = $('#consultation\\.recipient').val();
            for (var i = 0; i < listOfConsultants.length; i++) {
                if (locationEnteredByUser == listOfConsultants[i].label) {
                    return true;
                }
            }
            return false;
        }, consultationConsultantValidationFailureMessage);

    }

    // attach 'validConsultantOnly' class to perform validation.
    jQuery.validator.addClassRules({
        "valid-consultant-only": {validConsultantOnly: true}
    });
    /* End - validConsultantOnly*/

    /*Start of Checking For Possibility Of Duplicate Form on encounter Date change*/
    $("#encounter\\.encounter_datetime" ).change(function() {
        var formUuid=$('#encounter\\.form_uuid').val();
        var encounterDateTime=$('#encounter\\.encounter_datetime').val();
        var patientUuid=$('#patient\\.uuid').val();
        var formData = $.trim($('#pre_populate_data').html());
        htmlDataStore.checkForPossibleFormDuplicate(formUuid,encounterDateTime,patientUuid,formData);
    });
    /*End of Checking For Possibility Of Duplicate Form on encounter date change*/

    /*Start of Checking For Possibility Of Duplicate Form on Form Load*/
    $(window).ready(function() {
        var formUuid=$('#encounter\\.form_uuid').val();
        var encounterDateTime=$('#encounter\\.encounter_datetime').val();
        var patientUuid=$('#patient\\.uuid').val();
        var formData = $.trim($('#pre_populate_data').html());
        htmlDataStore.checkForPossibleFormDuplicate(formUuid,encounterDateTime,patientUuid,formData);
    });
    /*End of Checking For Possibility Of Duplicate Form on Form Load*/

    /*setting default encounter location*/
    var defaultEncounterLocationSetting = htmlDataStore.getDefaultEncounterLocationSetting();
    if(defaultEncounterLocationSetting){
        var defaultEncounterLocation = htmlDataStore.getDefaultEncounterLocationPreference();
        defaultEncounterLocation = JSON.parse(defaultEncounterLocation);
        $.each(defaultEncounterLocation, function () {
            $("#encounter\\.location_id").val(this.name);
            $('[name="encounter\\.location_id"]').val(this.id);
        });
    }

    /*end of Setting Default encounter Location*/

    /* Start populating gps location data */

    if($('.cummulativeFormOpeningGPSData').length){
        if($('.cummulativeFormOpeningGPSData').val().length){
            var lastKnowGPSLocationJsonObj = htmlDataStore.getLastKnowGPSLocation("json-object");
            var gpsLocationDataOnForm = $('.cummulativeFormOpeningGPSData').val();
            gpsLocationDataOnForm = gpsLocationDataOnForm.trim();
            lastKnowGPSLocationJsonObj = lastKnowGPSLocationJsonObj.trim();
            if(gpsLocationDataOnForm.substring(0, 2) === "[{" && lastKnowGPSLocationJsonObj.substring(0, 1) === "{"){
                var locationObj = JSON.parse(gpsLocationDataOnForm);
                locationObj.push(JSON.parse(lastKnowGPSLocationJsonObj));
                var newLocationData = JSON.stringify(locationObj);
                $(".cummulativeFormOpeningGPSData").val(newLocationData);
            }else{
                var newLocationData = htmlDataStore.getLastKnowGPSLocation("json-array");
                $(".cummulativeFormOpeningGPSData").val(newLocationData);
            }
        } else {
            var lastKnowGPSLocationJsonArray = htmlDataStore.getLastKnowGPSLocation("json-array");
            $(".cummulativeFormOpeningGPSData").val(lastKnowGPSLocationJsonArray);
        }
    }

    /*Start of populating initial form opening timestamp*/
    if($('.initialFormOpeningTimestamp').length && !$('.initialFormOpeningTimestamp').val().length){
        var date = new Date();
        $('.initialFormOpeningTimestamp').val(date.getTime());
    }
    /*end of populating initial form opening timestamp*/

    /*Start- GPS Location picker functionality*/

    /* Called by the Activity WebViewActivity*/
    document.populatePickedGpsLocation = function (sectionName, jsonString) {
        var $parent = $('div[data-name="' + sectionName + '"]');
        $.each(jsonString, function (key, value) {
            var $inputField = $parent.find("input[name='" + key + "']");
            $inputField.val(value);
            $inputField.trigger('change');  //Need this to trigger the event so GPS location value gets populated.
        });
    };

    $('.gps-location-picker').click(function () {
        var $parent = $(this).closest('div[data-name]');
        var zoomLevel =  $(this).attr('data-zoomlevel');
        var createDemographicsPreferredUpdate = $(this).attr('data-create-demographics-update');

        if(createDemographicsPreferredUpdate == 'true' || createDemographicsPreferredUpdate == true || createDemographicsPreferredUpdate == 1){
            createDemographicsPreferredUpdate = true;
        } else {
            createDemographicsPreferredUpdate = false;
        }
        if (typeof zoomLevel === typeof undefined || zoomLevel === false) {
            zoomLevel = 12;
        }
        gpsLocationPickerComponent.getGPSLocationPicker(
            $parent.attr('data-name'),
            $parent.find("input.latitude").attr('name'),
            $parent.find("input.longitude").attr('name'),
            zoomLevel,
            createDemographicsPreferredUpdate
        );
    });

    /*End- Location picker functionality*/

    /*Start- Create and populate relationship person functionality*/
    $('.create-relationship-person').click(function () {
        var $parent = $(this).closest('div[data-name]');
        var resultField = $(this).attr('data-result-field');
        relationshipCreatorComponent.createRelationshipPerson(
            $parent.attr('data-name'),
            resultField
        );
    });

    document.updatePersonDemographics = function(sectionName, resultField, personUuid){
        relationshipCreatorComponent.updateRelationshipPerson(
            sectionName,
            resultField,
            personUuid
        );
    }

    document.populateRelationshipPerson = function (sectionName, jsonString) {
        var $parent = $('div[data-name="' + sectionName + '"]');
        $.each(jsonString, function (key, value) {
            var $inputField = $parent.find("input[name='" + key + "']");
            $inputField.val(value);
            $inputField.trigger('change');
        });
    };
    /*End- Create and populate relationship person functionality*/

    document.launchEncounterMiniForm = function(patientUuid,formUuid) {
        encounterMiniFormCreatorComponent.launchEncounterMiniForm(patientUuid,formUuid);
    };


    document.setupAutoCompleteForCountry = function(elementName) {
        var dataDictionary = [
            {"val": "Afghanistan", "label": "Afghanistan"},
            {"val": "Albania", "label": "Albania"},
            {"val": "Algeria", "label": "Algeria"},
            {"val": "Andorra", "label": "Andorra"},
            {"val": "Angola", "label": "Angola"},
            {"val": "Antigua and Barbuda", "label": "Antigua and Barbuda"},
            {"val": "Argentina", "label": "Argentina"},
            {"val": "Armenia", "label": "Armenia"},
            {"val": "Australia", "label": "Australia"},
            {"val": "Austria", "label": "Austria"},
            {"val": "Azerbaijan", "label": "Azerbaijan"},
            {"val": "Bahamas", "label": "Bahamas"},
            {"val": "Bahrain", "label": "Bahrain"},
            {"val": "Bangladesh", "label": "Bangladesh"},
            {"val": "Barbados", "label": "Barbados"},
            {"val": "Belarus", "label": "Belarus"},
            {"val": "Belgium", "label": "Belgium"},
            {"val": "Belize", "label": "Belize"},
            {"val": "Benin", "label": "Benin"},
            {"val": "Bhutan", "label": "Bhutan"},
            {"val": "Bolivia", "label": "Bolivia"},
            {"val": "Bosnia and Herzegovina", "label": "Bosnia and Herzegovina"},
            {"val": "Botswana", "label": "Botswana"},
            {"val": "Brazil", "label": "Brazil"},
            {"val": "Brunei", "label": "Brunei"},
            {"val": "Bulgaria", "label": "Bulgaria"},
            {"val": "Burkina Faso", "label": "Burkina Faso"},
            {"val": "Burundi", "label": "Burundi"},
            {"val": "Côte d'Ivoire", "label": "Côte d'Ivoire"},
            {"val": "Cabo Verde", "label": "Cabo Verde"},
            {"val": "Cambodia", "label": "Cambodia"},
            {"val": "Cameroon", "label": "Cameroon"},
            {"val": "Canada", "label": "Canada"},
            {"val": "Central African Republic", "label": "Central African Republic"},
            {"val": "Chad", "label": "Chad"},
            {"val": "Chile", "label": "Chile"},
            {"val": "China", "label": "China"},
            {"val": "Colombia", "label": "Colombia"},
            {"val": "Comoros", "label": "Comoros"},
            {"val": "Congo (Congo-Brazzaville)", "label": "Congo (Congo-Brazzaville)"},
            {"val": "Costa Rica", "label": "Costa Rica"},
            {"val": "Croatia", "label": "Croatia"},
            {"val": "Cuba", "label": "Cuba"},
            {"val": "Cyprus", "label": "Cyprus"},
            {"val": "Czechia (Czech Republic)", "label": "Czechia (Czech Republic)"},
            {"val": "Democratic Republic of the Congo", "label": "Democratic Republic of the Congo"},
            {"val": "Denmark", "label": "Denmark"},
            {"val": "Djibouti", "label": "Djibouti"},
            {"val": "Dominica", "label": "Dominica"},
            {"val": "Dominican Republic", "label": "Dominican Republic"},
            {"val": "Ecuador", "label": "Ecuador"},
            {"val": "Egypt", "label": "Egypt"},
            {"val": "El Salvador", "label": "El Salvador"},
            {"val": "Equatorial Guinea", "label": "Equatorial Guinea"},
            {"val": "Eritrea", "label": "Eritrea"},
            {"val": "Estonia", "label": "Estonia"},
            {"val": "Eswatini (fmr. Swaziland)", "label": "Eswatini (fmr. Swaziland)"},
            {"val": "Ethiopia", "label": "Ethiopia"},
            {"val": "Fiji", "label": "Fiji"},
            {"val": "Finland", "label": "Finland"},
            {"val": "France", "label": "France"},
            {"val": "Gabon", "label": "Gabon"},
            {"val": "Gambia", "label": "Gambia"},
            {"val": "Georgia", "label": "Georgia"},
            {"val": "Germany", "label": "Germany"},
            {"val": "Ghana", "label": "Ghana"},
            {"val": "Greece", "label": "Greece"},
            {"val": "Grenada", "label": "Grenada"},
            {"val": "Guatemala", "label": "Guatemala"},
            {"val": "Guinea", "label": "Guinea"},
            {"val": "Guinea-Bissau", "label": "Guinea-Bissau"},
            {"val": "Guyana", "label": "Guyana"},
            {"val": "Haiti", "label": "Haiti"},
            {"val": "Holy See", "label": "Holy See"},
            {"val": "Honduras", "label": "Honduras"},
            {"val": "Hungary", "label": "Hungary"},
            {"val": "Iceland", "label": "Iceland"},
            {"val": "India", "label": "India"},
            {"val": "Indonesia", "label": "Indonesia"},
            {"val": "Iran", "label": "Iran"},
            {"val": "Iraq", "label": "Iraq"},
            {"val": "Ireland", "label": "Ireland"},
            {"val": "Israel", "label": "Israel"},
            {"val": "Italy", "label": "Italy"},
            {"val": "Jamaica", "label": "Jamaica"},
            {"val": "Japan", "label": "Japan"},
            {"val": "Jordan", "label": "Jordan"},
            {"val": "Kazakhstan", "label": "Kazakhstan"},
            {"val": "Kenya", "label": "Kenya"},
            {"val": "Kiribati", "label": "Kiribati"},
            {"val": "Kuwait", "label": "Kuwait"},
            {"val": "Kyrgyzstan", "label": "Kyrgyzstan"},
            {"val": "Laos", "label": "Laos"},
            {"val": "Latvia", "label": "Latvia"},
            {"val": "Lebanon", "label": "Lebanon"},
            {"val": "Lesotho", "label": "Lesotho"},
            {"val": "Liberia", "label": "Liberia"},
            {"val": "Libya", "label": "Libya"},
            {"val": "Liechtenstein", "label": "Liechtenstein"},
            {"val": "Lithuania", "label": "Lithuania"},
            {"val": "Luxembourg", "label": "Luxembourg"},
            {"val": "Madagascar", "label": "Madagascar"},
            {"val": "Malawi", "label": "Malawi"},
            {"val": "Malaysia", "label": "Malaysia"},
            {"val": "Maldives", "label": "Maldives"},
            {"val": "Mali", "label": "Mali"},
            {"val": "Malta", "label": "Malta"},
            {"val": "Marshall Islands", "label": "Marshall Islands"},
            {"val": "Mauritania", "label": "Mauritania"},
            {"val": "Mauritius", "label": "Mauritius"},
            {"val": "Mexico", "label": "Mexico"},
            {"val": "Micronesia", "label": "Micronesia"},
            {"val": "Moldova", "label": "Moldova"},
            {"val": "Monaco", "label": "Monaco"},
            {"val": "Mongolia", "label": "Mongolia"},
            {"val": "Montenegro", "label": "Montenegro"},
            {"val": "Morocco", "label": "Morocco"},
            {"val": "Mozambique", "label": "Mozambique"},
            {"val": "Myanmar (formerly Burma)", "label": "Myanmar (formerly Burma)"},
            {"val": "Namibia", "label": "Namibia"},
            {"val": "Nauru", "label": "Nauru"},
            {"val": "Nepal", "label": "Nepal"},
            {"val": "Netherlands", "label": "Netherlands"},
            {"val": "New Zealand", "label": "New Zealand"},
            {"val": "Nicaragua", "label": "Nicaragua"},
            {"val": "Niger", "label": "Niger"},
            {"val": "Nigeria", "label": "Nigeria"},
            {"val": "North Korea", "label": "North Korea"},
            {"val": "North Macedonia", "label": "North Macedonia"},
            {"val": "Norway", "label": "Norway"},
            {"val": "Oman", "label": "Oman"},
            {"val": "Pakistan", "label": "Pakistan"},
            {"val": "Palau", "label": "Palau"},
            {"val": "Palestine State", "label": "Palestine State"},
            {"val": "Panama", "label": "Panama"},
            {"val": "Papua New Guinea", "label": "Papua New Guinea"},
            {"val": "Paraguay", "label": "Paraguay"},
            {"val": "Peru", "label": "Peru"},
            {"val": "Philippines", "label": "Philippines"},
            {"val": "Poland", "label": "Poland"},
            {"val": "Portugal", "label": "Portugal"},
            {"val": "Qatar", "label": "Qatar"},
            {"val": "Romania", "label": "Romania"},
            {"val": "Russia", "label": "Russia"},
            {"val": "Rwanda", "label": "Rwanda"},
            {"val": "Saint Kitts and Nevis", "label": "Saint Kitts and Nevis"},
            {"val": "Saint Lucia", "label": "Saint Lucia"},
            {"val": "Saint Vincent and the Grenadines", "label": "Saint Vincent and the Grenadines"},
            {"val": "Samoa", "label": "Samoa"},
            {"val": "San Marino", "label": "San Marino"},
            {"val": "Sao Tome and Principe", "label": "Sao Tome and Principe"},
            {"val": "Saudi Arabia", "label": "Saudi Arabia"},
            {"val": "Senegal", "label": "Senegal"},
            {"val": "Serbia", "label": "Serbia"},
            {"val": "Seychelles", "label": "Seychelles"},
            {"val": "Sierra Leone", "label": "Sierra Leone"},
            {"val": "Singapore", "label": "Singapore"},
            {"val": "Slovakia", "label": "Slovakia"},
            {"val": "Slovenia", "label": "Slovenia"},
            {"val": "Solomon Islands", "label": "Solomon Islands"},
            {"val": "Somalia", "label": "Somalia"},
            {"val": "South Africa", "label": "South Africa"},
            {"val": "South Korea", "label": "South Korea"},
            {"val": "South Sudan", "label": "South Sudan"},
            {"val": "Spain", "label": "Spain"},
            {"val": "Sri Lanka", "label": "Sri Lanka"},
            {"val": "Sudan", "label": "Sudan"},
            {"val": "Suriname", "label": "Suriname"},
            {"val": "Sweden", "label": "Sweden"},
            {"val": "Switzerland", "label": "Switzerland"},
            {"val": "Syria", "label": "Syria"},
            {"val": "Tajikistan", "label": "Tajikistan"},
            {"val": "Tanzania", "label": "Tanzania"},
            {"val": "Thailand", "label": "Thailand"},
            {"val": "Timor-Leste", "label": "Timor-Leste"},
            {"val": "Togo", "label": "Togo"},
            {"val": "Tonga", "label": "Tonga"},
            {"val": "Trinidad and Tobago", "label": "Trinidad and Tobago"},
            {"val": "Tunisia", "label": "Tunisia"},
            {"val": "Turkey", "label": "Turkey"},
            {"val": "Turkmenistan", "label": "Turkmenistan"},
            {"val": "Tuvalu", "label": "Tuvalu"},
            {"val": "Uganda", "label": "Uganda"},
            {"val": "Ukraine", "label": "Ukraine"},
            {"val": "United Arab Emirates", "label": "United Arab Emirates"},
            {"val": "United Kingdom", "label": "United Kingdom"},
            {"val": "United States of America", "label": "United States of America"},
            {"val": "Uruguay", "label": "Uruguay"},
            {"val": "Uzbekistan", "label": "Uzbekistan"},
            {"val": "Vanuatu", "label": "Vanuatu"},
            {"val": "Venezuela", "label": "Venezuela"},
            {"val": "Vietnam", "label": "Vietnam"},
            {"val": "Yemen", "label": "Yemen"},
            {"val": "Zambia", "label": "Zambia"},
            {"val": "Zimbabwe", "label": "Zimbabwe"}
        ];
        document.setupAutoComplete(elementName, dataDictionary);
    };

});
