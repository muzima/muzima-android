package com.muzima.model.shr.kenyaemr.Addendum;

/**
 * Created by patrick on 11/04/2018.
 */
import com.fasterxml.jackson.annotation.*;
import com.muzima.model.shr.kenyaemr.CardDetails;

import java.util.List;

/**
 * Created by Muhoro on 2/27/2018.
 */

public class Addendum {

    @JsonProperty("CARD_DETAILS")
    private CardDetails CardDetail;

    @JsonProperty("IDENTIFIERS")
    private List<Identifier> Identifiers;

    public CardDetails getCardDetail() {
        return CardDetail;
    }

    public void setCardDetail(CardDetails cardDetail) {
        CardDetail = cardDetail;
    }

    public List<Identifier> getIdentifiers() {
        return Identifiers;
    }

    public void setIdentifiers(List<Identifier> identifiers) {
        Identifiers = identifiers;
    }
}
