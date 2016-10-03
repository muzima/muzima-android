var enketo = enketo || {};
enketo.FormDataRepository = function () {
    "use strict";

    var repository;
    if (typeof formDataRepositoryContext !== "undefined") {
        repository = formDataRepositoryContext;
    }

    return {
        getFormInstanceByFormTypeAndId: function (formID, formName) {
            return null;
        },
        queryUniqueResult: function (sql) {
            return repository.queryUniqueResult(sql);
        },
        queryList: function (sql) {
            return repository.queryList(sql);
        },
        saveFormSubmission: function (params, xmlData, data) {
        return repository.saveFormSubmission(JSON.stringify(params), xmlData, JSON.stringify(data));
        },
        saveEntity: function (entityType, entity) {
            return repository.saveEntity(entityType, JSON.stringify(entity));
        }
    };
};

enketo.EntityRelationshipLoader = function(){
    var load = function(){ 
        console.log("EntityRelationshipLoader.load called");

        return {};
    };
    return {
        load:  load
    };
};

enketo.FormDefinitionLoader = function(){
    var load = function(){ 
        console.log("FormDefinitionLoader.load called");

        return {};
    };
    return {
        load:  load
    };
};

enketo.FormModelMapper = function(formDataRepository, queryBuilder, idFactory){
    var mapToFormModel = function(){ 
        console.log("mapToFormModel called");
        var model = JSON.parse(getFormInterface().getModelXml());
        for(var i = 0; i < model.form.fields.length; i++){
            model.form.fields[i].value = "";
        }
        model.errors = [];
        return model;
    };

    var mapToEntityAndSave = function(){ 
        console.log("mapToEntityAndSave called");
    };
    return {
        mapToFormModel:  mapToFormModel,
        mapToEntityAndSave: mapToEntityAndSave
    };
};


enketo.SQLQueryBuilder = function(formDataRepository){
    var loadEntityHierarchy = function(){ 
        console.log("loadEntityHierarchy called");
    };

    return {
        loadEntityHierarchy:  loadEntityHierarchy,
    };
};


enketo.IdFactory = function (idFactoryBridge) {
    "use strict";
    return{
        generateIdFor: function (entityType) {
            return idFactoryBridge.generateIdFor(entityType);
        }
    };
};

enketo.IdFactoryBridge = function () {
    "use strict";
    var idFactoryContext;
    if (typeof formDataRepositoryContext !== "undefined") {
        idFactoryContext = formDataRepositoryContext;
    }

    return {
        generateIdFor: function (entityType) {
            return idFactoryContext.generateIdFor(entityType);
        }
    };
};

enketo.FormSubmissionRouter = function () {
    "use strict";
    var submissionRouter;
    if (typeof formSubmissionRouter !== "undefined") {
        submissionRouter = formSubmissionRouter;
    }

    return {
        route: function (instanceId) {
            return submissionRouter.route(instanceId);
        }
    };
};

enketo.hasValue = function (object) {
    "use strict";
    return !(typeof object === "undefined" || !object);
};

enketo.EntityRelationships = function(){
    var determineEntitiesAndRelations = function(){
        console.log("EntityRelationships determineEntitiesAndRelations called");
    }
    return {
        determineEntitiesAndRelations : determineEntitiesAndRelations
    };
}
