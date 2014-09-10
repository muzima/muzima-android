/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

$(document).ready(function () {
    'use strict';
    var dateFormat = "dd-mm-yy";

    /* Start - Function to save the form */
    document.submit = function () {
        var validForm = $("form").valid();
        if (typeof $.fn.customValidationCheck !== 'undefined' && typeof $.fn.customValidationCheck === 'function') {
            validForm = validForm && $.fn.customValidationCheck();
        }
        if (validForm) {
            save("complete");
        }
    };

    document.saveDraft = function () {
        save("incomplete");
        return false;
    };

    var save = function (status) {
        var jsonData = JSON.stringify($('form').serializeEncounterForm());
        htmlDataStore.saveHTML(jsonData, status);
    };
    /* End - Function to save the form */

    /*Start- BarCode Functionality*/

    /* Called by the Activity WebViewActivity*/
    document.populateBarCode = function (jsonString) {
        $.each(jsonString, function (key, value) {
            var $inputField = $("input[name='" + key + "']");
            $inputField.val(value);
            $inputField.trigger('change');  //Need this to trigger the event so AMRS id gets populated.
        })
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
        })
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
        })
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
        })
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

    /* Start - Initialize jQuery DatePicker */

    $('.datepicker').datepicker({
        dateFormat: dateFormat,
        changeMonth: true,
        changeYear: true
    });

    /* Start - Initialize jQuery DatePicker */

    /* Start - CheckDigit algorithm Source: https://wiki.openmrs.org/display/docs/Check+Digit+Algorithm */

    $.validator.addMethod("checkDigit", function (value, element) {
            var num = value.split('-');
            if (num.length != 2) {
                return false;
            }
            return $.fn.luhnCheckDigit(num[0]) == num[1];
        }, "Please enter digits that matches CheckDigit algorithm."
    );

    // attach 'checkDigit' class to perform validation.
    jQuery.validator.addClassRules({
        checkDigit: { checkDigit: true }
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
        checkDigit: { checkDigit: true }
    });

    /* Start - Checking that the current date is not in the future */

    $.validator.addMethod("nonFutureDate", function (value, element) {
            if ($.fn.isNotRequiredAndEmpty(value, element)) return true;
            var pattern = /(\d{2})-(\d{2})-(\d{4})/g;
            var matches = pattern.exec(value);
            var enteredDate = new Date(matches[3], matches[2] - 1, matches[1]);
            var today = new Date();
            return enteredDate <= today;
        }, "Please enter a date prior or equal to today."
    );

    // attach 'nonFutureDate' class to perform validation.
    jQuery.validator.addClassRules({
        nonFutureDate: { nonFutureDate: true }
    });

    /* End - nonFutureDate*/

    /* Start - Checking that the current date is in the future */

    $.validator.addMethod("checkFutureDate", function (value, element) {
            if ($.fn.isNotRequiredAndEmpty(value, element)) return true;
            var pattern = /(\d{2})-(\d{2})-(\d{4})/g;
            var matches = pattern.exec(value);
            var enteredDate = new Date(matches[3], matches[2] - 1, matches[1]);
            var today = new Date();
            return enteredDate > today;
        }, "Please enter a date in the future."
    );

    // attach 'checkFutureDate' class to perform validation.
    jQuery.validator.addClassRules({
        checkFutureDate: { checkFutureDate: true }
    });

    /* End - checkFutureDate*/

    /* Start - Checking that the entered value is a valid phone number */

    $.validator.addMethod("phoneNumber", function (value, element) {
            if ($.fn.isNotRequiredAndEmpty(value, element)) return true;
            var inputLength = value.length;
            return inputLength >= 8 && inputLength <= 12;
        }, "Invalid Phone Number. Please check and re-enter."
    );

    // attach 'phoneNumber' class to perform validation.
    jQuery.validator.addClassRules({
        phoneNumber: { phoneNumber: true }
    });

    /* End - phoneNumber*/

    /* Start - checkNoneSelectedAlone*/

    $.fn.checkNoneSelectedAlone = function (nameArray) {
        var $validator = $('form').validate();
        var errors = {};
        var result = true;
        $.each(nameArray, function (i, element) {
            var fieldSetElem = $('fieldset[name="' + element + '"]');
            result = isValidForNoneSelection(fieldSetElem);
            if (!result) {
                errors[element] = "If 'None' is selected, no other options can be selected.";
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
            if (($(cBoxElement).val() == 'none' || $(cBoxElement).val() == '1107^NONE^99DCT')
                && totalSelected > 1) {
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

    $.fn.getAgeInYears = function (birthDateString) {
        var birthDate = new Date(birthDateString);
        var today = new Date();
        var milliSecondsInAYear = 1000 * 60 * 60 * 24 * 365.26;
        return (today - birthDate) / milliSecondsInAYear;
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

        /* clear values on cloned fields */
        $clonedSection.find(':input:not(:button)').val('');
        $clonedSection.find(':input:not(:button)').trigger('change');
    });

    $(document.body).on('click', '.remove_section', function () {
        var $parent = $(this).parent();
        var _id = $parent.attr('id');
        if ($("." + _id).length > 1) {
            $parent.remove();
        }
    });

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
                $div.find('[data-concept="' + k + '"]').val(v);
            } else {
                var elements = $div.find('[data-concept="' + k + '"]');
                $.each(elements, function(i, element) {
                    applyValue(element, v);
                });
            }
        });
    };

    var populateNonConceptFields = function (prePopulateJson) {
        $.each(prePopulateJson, function (key, value) {
            var $elements = $('[name="' + key + '"]');
            $.each($elements, function (i, element) {
                if ($(element).is(':checkbox') || $(element).is(':radio')) {
                    if ($(element).val() == value) {
                        $(element).prop('checked', true);
                    }
                } else {
                    $(element).val(value);
                }
            });
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

    var populateObservations = function (prePopulateJson) {
        $.each(prePopulateJson, function (key, value) {
            if (value instanceof Object) {
                // check if this is a grouping observation.
                var $div = $('div[data-concept="' + key + '"]');
                if ($div.length > 0) {
                    // we are dealing with grouping
                    if (value instanceof Array) {
                        $.each(value, function (i, element) {
                            if (i == 0) {
                                populateDataConcepts($div, element);
                            } else {
                                var $clonedDiv = $div.clone(true);
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
                        var elements = $('[data-concept="' + key + '"]');
                        if (elements.length < value.length) {
                            $.each(value, function (i, valueElement) {
                                if (i == 0) {
                                    $.each(elements, function(i, element) {
                                        applyValue(element, valueElement);
                                    });
                                } else {
                                    var $div = $(elements).closest('.repeat, .custom-repeat');
                                    var $clonedDiv = $div.clone(true);
                                    $div.after($clonedDiv);
                                    elements = $clonedDiv.find('[data-concept="' + key + '"]');
                                    $.each(elements, function(i, element) {
                                        applyValue(element, valueElement);
                                    });
                                }
                            });
                        } else {
                            $.each(value, function (i, valueElement) {
                                $.each(elements, function(i, element) {
                                    applyValue(element, valueElement);
                                });
                            });
                        }
                    } else {
                        populateDataConcepts($div, value);
                    }
                }
            }
            else {
                var $elements = $('[data-concept="' + key + '"]');
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
        $.each(prePopulateJSON, function(key, value) {
            if (key === 'observation') {
                populateObservations(value);
            } else {
                populateNonConceptFields(value);
            }
        });
        console.timeEnd("Starting population");
    }
    /* End - JS to Prepopulate Data in the Form */

    /* Start - Code to Serialize form along with Data-Concepts */
    $.fn.serializeEncounterForm = function () {
        var jsonResult = $.extend({}, serializeNonConceptElements(this),
            serializeConcepts(this), serializeNestedConcepts(this));
        var completeObject = {};
        var defaultKey = "observation";
        $.each(jsonResult, function (k, v) {
            var key = defaultKey;
            var dotIndex = k.indexOf(".");
            if (dotIndex >= 0) {
                key = k.substr(0, k.indexOf("."));
            }
            var objects = completeObject[key];
            if (objects === undefined) {
                objects = {};
                completeObject[key] = objects;
            }
            objects[k] = v;
        });
        return completeObject;
    };

    var serializeNonConceptElements = function ($form) {
        var object = {};
        var $inputElements = $form.find('[name]').not('[data-concept]');
        $.each($inputElements, function (i, element) {
            if ($(element).is(':checkbox') || $(element).is(':radio')) {
                if ($(element).is(':checked')) {
                    object = pushIntoArray(object, $(element).attr('name'), $(element).val());
                }
            } else {
                object = pushIntoArray(object, $(element).attr('name'), $(element).val());
            }
        });
        return object;
    };

    var serializeNestedConcepts = function ($form) {
        var result = {};
        var parentDivs = $form.find('div[data-concept]').filter(':visible');
        $.each(parentDivs, function (i, element) {
            var $allConcepts = $(element).find('*[data-concept]');
            result = pushIntoArray(result, $(element).attr('data-concept'), jsonifyConcepts($allConcepts));
        });
        return result;
    };

    var serializeConcepts = function ($form) {
        var object = {};
        var allConcepts = $form.find('*[data-concept]').filter(':visible');
        $.each(allConcepts, function (i, element) {
            if ($(element).closest('.section, .concept-set').attr('data-concept') == undefined) {
                var jsonifiedConcepts = jsonifyConcepts($(element));
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

    var jsonifyConcepts = function ($allConcepts) {
        var o = {};
        $.each($allConcepts, function (i, element) {
            if ($(element).is(':checkbox') || $(element).is(':radio')) {
                if ($(element).is(':checked')) {
                    o = pushIntoArray(o, $(element).attr('data-concept'), $(element).val());
                }
            } else {
                o = pushIntoArray(o, $(element).attr('data-concept'), $(element).val());
            }
        });
        return o;
    };

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

    /* Called by the Activity WebViewActivity*/
    document.populateFingeprint = function (jsonString) {
        $.each(jsonString, function (key, value) {
            var $inputField = $("input[name='" + key + "']");
            $inputField.val(value);
            $inputField.trigger('change');  //Need this to trigger the event so AMRS id gets populated.
        })
    };
    $('.fingerprint_btn').click(function () {
        fingerprintComponent.startFingerprintIntent($(this).parent().find("input[type='text']").attr('name'));
    });

    /* End - Code to Serialize form along with Data-Concepts */

});
