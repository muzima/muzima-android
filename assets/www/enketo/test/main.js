var getDataFromFile = function (url) {
    var result = "";
    $.ajax('./test/' + url, {
        async: false,
        dataType: 'text',
        mimeType: 'application/json',
        success: function (json) {
            result = json;
        }
    });
    return result;
};

var formInstance = formInstance || {
    getModel: function () {
        return getDataFromFile('model.xml');
    }, getHTML: function () {
        return getDataFromFile('form.html');
    }};

var ziggyFileLoader = ziggyFileLoader || {
    loadAppData: function () {
        return getDataFromFile('model.json');
    }};

var formDataRepositoryContext = formDataRepositoryContext || {
    getFormPayload: function () {
        return getDataFromFile('payload.json');
    },
    saveFormSubmission: function (data, xmlData, status) {
        console.log(JSON.stringify(data));
    }

}