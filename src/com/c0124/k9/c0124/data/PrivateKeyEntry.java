package com.c0124.k9.c0124.data;

import java.nio.ByteBuffer;

/**
 * Created by xinqian on 7/7/15.
 */
public class PrivateKeyEntry {
    public final ByteBuffer priKey;
    public final long timeStamp;

    public PrivateKeyEntry(ByteBuffer priKey, long timeStamp) {
        this.priKey = priKey;
        this.timeStamp = timeStamp;
    }
}
