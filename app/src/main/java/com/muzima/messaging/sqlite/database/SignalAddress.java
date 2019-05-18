package com.muzima.messaging.sqlite.database;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.ShortNumberInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.utils.GroupUtil;
import com.muzima.messaging.utils.Util;
import com.muzima.util.DelimiterUtil;
import com.muzima.utils.NumberUtil;

public class SignalAddress implements Parcelable, Comparable<SignalAddress> {

    public static final Parcelable.Creator<SignalAddress> CREATOR = new Parcelable.Creator<SignalAddress>() {
        public SignalAddress createFromParcel(Parcel in) {
            return new SignalAddress(in);
        }

        public SignalAddress[] newArray(int size) {
            return new SignalAddress[size];
        }
    };

    public static final SignalAddress UNKNOWN = new SignalAddress("Unknown");

    private static final String TAG = SignalAddress.class.getSimpleName();

    private static final AtomicReference<Pair<String, ExternalAddressFormatter>> cachedFormatter = new AtomicReference<>();

    private final String address;

    private SignalAddress(@NonNull String address) {
        if (address == null) throw new AssertionError(address);
        this.address = address;
    }

    public SignalAddress(Parcel in) {
        this(in.readString());
    }

    public static @NonNull SignalAddress fromSerialized(@NonNull String serialized) {
        return new SignalAddress(serialized);
    }

    public static SignalAddress fromExternal(@NonNull Context context, @Nullable String external) {
        return new SignalAddress(getExternalAddressFormatter(context).format(external));
    }

    public static @NonNull List<SignalAddress> fromSerializedList(@NonNull String serialized, char delimiter) {
        String[]      escapedAddresses = DelimiterUtil.split(serialized, delimiter);
        List<SignalAddress> addresses        = new LinkedList<>();

        for (String escapedAddress : escapedAddresses) {
            addresses.add(SignalAddress.fromSerialized(DelimiterUtil.unescape(escapedAddress, delimiter)));
        }

        return addresses;
    }

    public static @NonNull String toSerializedList(@NonNull List<SignalAddress> addresses, char delimiter) {
        Collections.sort(addresses);

        List<String> escapedAddresses = new LinkedList<>();

        for (SignalAddress address : addresses) {
            escapedAddresses.add(DelimiterUtil.escape(address.serialize(), delimiter));
        }

        return Util.join(escapedAddresses, delimiter + "");
    }

    private static @NonNull ExternalAddressFormatter getExternalAddressFormatter(Context context) {
        String localNumber = TextSecurePreferences.getLocalNumber(context);

        if (!TextUtils.isEmpty(localNumber)) {
            Pair<String, ExternalAddressFormatter> cached = cachedFormatter.get();

            if (cached != null && cached.first.equals(localNumber)) return cached.second;

            ExternalAddressFormatter formatter = new ExternalAddressFormatter(localNumber);
            cachedFormatter.set(new Pair<>(localNumber, formatter));

            return formatter;
        } else {
            return new ExternalAddressFormatter(Util.getSimCountryIso(context).or("US"), true);
        }
    }

    @Override
    public String toString() {
        return address;
    }

    public String serialize() {
        return address;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof SignalAddress)) return false;
        return address.equals(((SignalAddress) other).address);
    }

    public boolean isGroup() {
        return GroupUtil.isEncodedGroup(address);
    }

    public boolean isMmsGroup() {
        return GroupUtil.isMmsGroup(address);
    }

    public boolean isEmail() {
        return NumberUtil.isValidEmail(address);
    }

    public boolean isPhone() {
        return !isGroup() && !isEmail();
    }

    public @NonNull String toGroupString() {
        if (!isGroup()) throw new AssertionError("Not group: " + address);
        return address;
    }

    public @NonNull String toPhoneString() {
        if (!isPhone()) throw new AssertionError("Not e164: " + address);
        return address;
    }

    public @NonNull String toEmailString() {
        if (!isEmail()) throw new AssertionError("Not email: " + address);
        return address;
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
    }

    @Override
    public int compareTo(@NonNull SignalAddress other) {
        return address.compareTo(other.address);
    }

    @VisibleForTesting
    public static class ExternalAddressFormatter {

        private static final String TAG = ExternalAddressFormatter.class.getSimpleName();

        private static final Set<String> SHORT_COUNTRIES = new HashSet<String>() {{
            add("NU");
            add("TK");
            add("NC");
            add("AC");
        }};

        private final String localNumberString;
        private final String localCountryCode;

        private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        private final Pattern         ALPHA_PATTERN   = Pattern.compile("[a-zA-Z]");

        ExternalAddressFormatter(@NonNull String localNumberString) {
            try {
                Phonenumber.PhoneNumber localNumber = phoneNumberUtil.parse(localNumberString, null);

                this.localNumberString = localNumberString;
                this.localCountryCode  = phoneNumberUtil.getRegionCodeForNumber(localNumber);
            } catch (NumberParseException e) {
                throw new AssertionError(e);
            }
        }

        ExternalAddressFormatter(@NonNull String localCountryCode, boolean countryCode) {
            this.localNumberString = "";
            this.localCountryCode  = localCountryCode;
        }

        public String format(@Nullable String number) {
            if (number == null)                       return "Unknown";
            if (ALPHA_PATTERN.matcher(number).find()) return number.trim();

            String bareNumber = number.replaceAll("[^0-9+]", "");

            if (bareNumber.length() == 0) {
                if (number.trim().length() == 0) return "Unknown";
                else                             return number.trim();
            }

            // libphonenumber doesn't seem to be correct for Germany and Finland
            if (bareNumber.length() <= 6 && ("DE".equals(localCountryCode) || "FI".equals(localCountryCode) || "SK".equals(localCountryCode))) {
                return bareNumber;
            }

            // libphonenumber seems incorrect for Russia and a few other countries with 4 digit short codes.
            if (bareNumber.length() <= 4 && !SHORT_COUNTRIES.contains(localCountryCode)) {
                return bareNumber;
            }

            try {
                Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(bareNumber, localCountryCode);

                if (ShortNumberInfo.getInstance().isPossibleShortNumberForRegion(parsedNumber, localCountryCode)) {
                    return bareNumber;
                }

                return phoneNumberUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
            } catch (NumberParseException e) {
                Log.w(TAG, e);
                if (bareNumber.charAt(0) == '+')
                    return bareNumber;

                String localNumberImprecise = localNumberString;

                if (localNumberImprecise.charAt(0) == '+')
                    localNumberImprecise = localNumberImprecise.substring(1);

                if (localNumberImprecise.length() == bareNumber.length() || bareNumber.length() > localNumberImprecise.length())
                    return "+" + number;

                int difference = localNumberImprecise.length() - bareNumber.length();

                return "+" + localNumberImprecise.substring(0, difference) + bareNumber;
            }
        }
    }

}
