$(document).ready(function () {
    'use strict';
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
        console.log("Prasanna");
        //barCodeComponent is defined in FormWebViewActivity.java

        //barCodeComponent.startBarCodeIntent($barcodeInput.attr('name'));
    });
    /*End- BarCode Functionality*/

});
