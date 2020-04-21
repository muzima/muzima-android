package com.muzima.view.forms;

public class CreateRelationshipPersonResult {
    private String personUuid;

    public CreateRelationshipPersonResult(String personUuid){
        this.personUuid = personUuid;
    }

    public String getPersonUuid() {
        return personUuid;
    }
}
