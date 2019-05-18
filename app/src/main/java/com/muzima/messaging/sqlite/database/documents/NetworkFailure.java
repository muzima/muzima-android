package com.muzima.messaging.sqlite.database.documents;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.muzima.messaging.sqlite.database.SignalAddress;

public class NetworkFailure {
    @JsonProperty(value = "a")
    private String address;

    public NetworkFailure(SignalAddress address) {
        this.address = address.serialize();
    }

    public NetworkFailure() {}

    @JsonIgnore
    public SignalAddress getAddress() {
        return SignalAddress.fromSerialized(address);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof NetworkFailure)) return false;

        NetworkFailure that = (NetworkFailure)other;
        return this.address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}
