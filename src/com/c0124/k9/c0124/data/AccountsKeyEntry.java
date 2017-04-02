package com.c0124.k9.c0124.data;

/**
 * Created by xinqian on 7/7/15.
 */
public class AccountsKeyEntry {
    public final String token;
    public final long timeStamp;
    public final String email;
    public boolean isRegistrationPending;
    // If the email have key but does not have an email setup in K9 - false.
    // If the email have key and also setup in K9 - true.
    public boolean isEmailHaveAccountSetup;
    final public boolean isHavingValidKey;

    public AccountsKeyEntry(String token, long timeStamp, String email, boolean isRegistrationPending, boolean isEmailHaveAccountSetup, boolean isHavingValidKey) {
        this.token = token;
        this.timeStamp = timeStamp;
        this.email = email;
        this.isRegistrationPending = isRegistrationPending;
        this.isEmailHaveAccountSetup = isEmailHaveAccountSetup;
        this.isHavingValidKey = isHavingValidKey;
    }
}
