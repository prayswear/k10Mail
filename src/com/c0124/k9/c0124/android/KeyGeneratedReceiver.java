package com.c0124.k9.c0124.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by xinqian on 8/8/15.
 */
public class KeyGeneratedReceiver extends BroadcastReceiver {

    static public final String intentMessageDataTag;
    static public final String actionTag;
    @Override
    public void onReceive(Context context, Intent intent) {
        CharSequence intentData;
        intentData = intent.getCharSequenceExtra(intentMessageDataTag);
        Toast.makeText(context, intentData, Toast.LENGTH_LONG).show();
    }

    static
    {
        intentMessageDataTag = "message";
        actionTag = "com.c0124.k9.c0124.android.KeyGenerationActionIntent";
    }
}

