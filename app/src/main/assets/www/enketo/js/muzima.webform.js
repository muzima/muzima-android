/**
 * @preserve Copyright 2012 Martijn van de Rijdt & Modi Research Group at Columbia University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*jslint browser:true, devel:true, jquery:true, smarttabs:true, sub:true *//*global vkbeautify, gui, FormDataController, modelStr, StorageLocal, FileManager, Form*/

var /**@type {Form}*/form;
var /**@type {*}*/fileManager;

$(document).ready(function () {
    'use strict';
    var existingInstanceJ, instanceToEdit, loadErrors, jDataO,
        queryParams = {id: "", formName: ""},
        formDataController = new FormDataController(queryParams);

    existingInstanceJ = formDataController.get();

    if (!existingInstanceJ) {
        $('form.jr').remove();
        return gui.alert('Instance with id "' + settings.instanceId + '" could not be found.');
    }

    jDataO = new JData(existingInstanceJ);
    instanceToEdit = jDataO.toXML();
    form = new Form('form.jr:eq(0)', modelStr, instanceToEdit);

    loadErrors = form.init();

    function save(status) {
        formDataRepositoryContext.showSaveProgressBar();
        var jsonData = jDataO.get();
        var xmlData = jDataO.toXML();
        delete jsonData.errors;
        formDataController.save(jsonData, xmlData, status);
    }

    document.saveDraft = function () {
        if (typeof form !== 'undefined') {
            save("incomplete");
        }
        return false;
    };

    document.submit = function () {
        if (typeof form !== 'undefined') {
            form.validateForm();
            if (!form.isValid()) {
                gui.alert('Form contains errors <br/>(please see fields marked in red)');
                return;
            }
            else {
                save("complete");
            }
        }
    };

    /*Start- BarCode Functionality*/

    document.populateBarCode = function (jsonString) {
        $.each(jsonString, function (key, value) {
            var $inputField = $("input[name='" + key + "']");
            $inputField.val(value);
            $inputField.trigger('change');  //Need this to trigger the event so AMRS id gets populated.
        })
    };

    var $barcodeInput = $('input[type="barcode"]');

    // !!Please keep the isFromInput flag. In WebView, if the .barcode_img is before the input, the click on the input
    // would trigger the event of click on the .barcode_img.
    var isFromInput = false;
    $barcodeInput.before("<input type='button' class='barcode_img'>");
    $('.barcode_img').click(function () {
        //barCodeComponent is defined in FormWebViewActivity.java
        if (!isFromInput) {
            barCodeComponent.startBarCodeIntent($barcodeInput.attr('name'));
        }
        isFromInput = false;
    });
    $barcodeInput.click(function (e) {
        isFromInput = true;
    });
    /*End- BarCode Functionality*/

    /*Start- Imaging Functionality*/

    document.populateImage = function (jsonString) {
        $.each(jsonString, function (key, value) {
            var $inputField = $("input[name='" + key + "']");
            $inputField.val(value);
            $inputField.trigger('change');  //Need this to trigger the event so image id gets populated.
        })
    };

    var $imageInput = $('input[type="image"]');

    // !!Please keep the isFromInput flag. In WebView, if the .image_img is before the input, the click on the input
    // would trigger the event of click on the .image_img.
    var isFromInput = false;
    $imageInput.before("<input type='button' class='image_img'>");
    $('.image_img').click(function () {
        //imagingComponent is defined in FormWebViewActivity.java
        if (!isFromInput) {
            imagingComponent.startImageIntent($imageInput.attr('name'));
        }
        isFromInput = false;
    });
    $imageInput.click(function (e) {
        isFromInput = true;
    });
    /*End- Image Functionality*/

    /*Start- Video Functionality*/

    document.populateVideo = function (jsonString) {
        $.each(jsonString, function (key, value) {
            var $inputField = $("input[name='" + key + "']");
            $inputField.val(value);
            $inputField.trigger('change');  //Need this to trigger the event so video file gets populated.
        })
    };

    var $videoInput = $('input[type="video"]');

    // !!Please keep the isFromInput flag. In WebView, if the .video_record is before the input, the click on the input
    // would trigger the event of click on the .video_record.
    var isFromInput = false;
    $videoInput.before("<input type='button' class='video_record'>");
    $('.video_record').click(function () {
        //videoComponent is defined in FormWebViewActivity.java
        if (!isFromInput) {
            videoComponent.startVideoIntent($videoInput.attr('name'));
        }
        isFromInput = false;
    });
    $videoInput.click(function (e) {
        isFromInput = true;
    });
    /*End- Video Functionality*/

    /*Start- Audio Functionality*/
    document.populateAudio = function (jsonString) {
        $.each(jsonString, function (key, value) {
            var $inputField = $("input[name='" + key + "']");
            $inputField.val(value);
            $inputField.trigger('change');  //Need this to trigger the event so audio file path gets populated.
        })
    };

    var $audioInput = $('input[type="audio"]');

    // !!Please keep the isFromInput flag. In WebView, if the .audio_record is before the input, the click on the input
    // would trigger the event of click on the .audio_record.
    var isFromInput = false;
    $audioInput.before("<input type='button' class='video_record'>");
    $('.video_record').click(function () {
        //audioComponent is defined in FormWebViewActivity.java
        if (!isFromInput) {
            audioComponent.startAudioIntent($audioInput.attr('name'));
        }
        isFromInput = false;
    });
    $audioInput.click(function (e) {
        isFromInput = true;
    });
    /*End- Audio Functionality*/


    /* Multi select Hack - Start */

    var checkboxParent = $("form input:checkbox").parent();

    //Set all the checkboxes to be hidden at init.
    checkboxParent.hide();

    // Gets the appropriate button text.
    var getButtonText = function (allOptions) {
        var selectedText = selectedOptions(allOptions);
        return selectedText.length == 0 ? 'Show Options' : selectedText.join();
    };

    // Returns an array of selected checkbox values.
    var selectedOptions = function (allOptions) {
        var selected = new Array();
        allOptions.each(function (i, element) {
            if ($(element).attr('data-checked') == 'true') {
                selected.push($(element).find('span').text());
            }
        });
        return selected;
    };

    // Iterates all the checkbox questions and adds a button to toggle its options.
    checkboxParent.parent().each(function (i, element) {
        var $btn = $($.parseHTML("<input class='toggle_chk_btn' type ='button'>"));
        $btn.val(getButtonText($(element).find('label')));
        $(element).append($btn)
    });

    // Perform option toggling.
     $(document).on('click','.toggle_chk_btn',function (e) {
        var $showHideBtn = $(e.target);
        var $siblingLabels = $showHideBtn.siblings("label");
        $siblingLabels.toggle();
        // Get back the focus on the selected question. A necessary step in mobile devices.
        $('html, body').animate({
            scrollTop: $showHideBtn.parent().offset().top
        }, 100);

        if ($siblingLabels.first().is(':visible')) {
            $showHideBtn.val("Hide Options");
        } else {
            $showHideBtn.val(getButtonText($siblingLabels));
        }
    });

    /* Multi select Hack - End */
});
