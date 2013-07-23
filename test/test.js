var /**@type {Form}*/form;
var /**@type {Connection}*/connection;
var /**@type {*}*/fileManager;

$(document).ready(function() {
  // 'use strict';
  var formParts, existingInstanceJ, instanceToEdit, loadErrors, jsonErrors, jDataO,
    queryParams = helper.getAllQueryParams(),
    formDataController = new FormDataController(queryParams);

  connection = new Connection();
  existingInstanceJ = formDataController.get();

  if (!existingInstanceJ){
    $('form.jr').remove();
    return gui.alert('Instance with id "'+settings.instanceId+'" could not be found.');
  }

  jDataO = new JData(existingInstanceJ);
  instanceToEdit = jDataO.toXML();
  console.debug('instance to edit: ', instanceToEdit);
  form = new Form('form.jr:eq(0)', modelStr, instanceToEdit);

  loadErrors = form.init();
  //check if JSON format is complete and if not, prepend the errors
  jsonErrors = jDataO.get().errors;

  //controller for submission of data to drishti
  $('#submit-form').click(function(){
      console.log("submit button clicked");
    var jData, saveResult;
    if (typeof form !== 'undefined'){
      form.validateForm();
      if (!form.isValid()){
        gui.alert('Form contains errors <br/>(please see fields marked in red)');
        return;
      }
      else{
        jData = jDataO.get();
        saveResult = formDataController.save(form.getInstanceID(), jData);
      }
    }
  });
});
