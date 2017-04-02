package com.c0124.k9.c0124.data;

/**
 * Created by xinqian on 7/7/15.
 */
public class RegistrationEntry {
    public final String myEmail;
    public final String signature;
    public final long timeStamp;
    public final long state; // 10 - created,

    public final static long CreatedState = 10;
    // public final static long RegFinished = 20; // 21, 22, 23 checked email 1, 2, 3 times.

    public RegistrationEntry(String myEmail, String signature, long timeStamp, long state)
    {
        this.myEmail = myEmail;
        this.signature = signature;
        this.timeStamp = timeStamp;
        this.state = state;
    }
}
