package com.c0124.k9.c0124.data;

import java.nio.ByteBuffer;

/**
 * Created by xinqian on 7/7/15.
 */
public class PublicKeyEntry {
    public final ByteBuffer pubKey;
    public final String token;
    public final long timeStamp;

    public PublicKeyEntry(ByteBuffer pubKey, String token, long timeStamp) {
        this.pubKey = pubKey;
        this.token = token;
        this.timeStamp = timeStamp;
    }
}
