package com.muzima.model.shr.kenyaemr.Addendum;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.muzima.model.shr.kenyaemr.CardDetails;

import java.util.List;

public class Addendum {

    @JsonProperty("CARD_DETAILS")
    private CardDetails cardDetail;

    @JsonProperty("IDENTIFIERS")
    private List<Identifier> identifiers;

    public CardDetails getCardDetail() {
        return cardDetail;
    }

    public void setCardDetail(CardDetails cardDetail) {
        this.cardDetail = cardDetail;
    }

    public List<Identifier> getIdentifiers() {
        return this.identifiers;
    }

    public void setIdentifiers(List<Identifier> identifiers) {
        this.identifiers = identifiers;
    }
}
