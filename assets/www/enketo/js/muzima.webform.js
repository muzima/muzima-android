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
        var jData = jDataO.get();
        delete jData.errors;
        formDataController.save(jData, status);
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
        if(!isFromInput){
            barCodeComponent.startBarCodeIntent($barcodeInput.attr('name'));
        }
        isFromInput = false;
    });
    $barcodeInput.click(function (e){
        isFromInput = true;
    });
    /*End- BarCode Functionality*/

});
