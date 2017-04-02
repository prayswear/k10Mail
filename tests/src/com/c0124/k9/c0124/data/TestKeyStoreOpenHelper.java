package com.c0124.k9.c0124.data;

import java.util.HashMap;
import java.util.Set;

import com.c0124.k9.c0124.data.KeyStoreOpenHelper;

import android.content.Context;

public class TestKeyStoreOpenHelper extends KeyStoreOpenHelper {

    public TestKeyStoreOpenHelper(Context context) {
        super(context);
    }

    public Set<String> getAllEmailsInRegistration()
    {
        return super.getAllEmailsInRegistration();
    }
    
    public HashMap<String, AccountsKeyEntry> getAllEmailsCurrentKeys()
    {
        return super.getAllEmailsCurrentKeys();
    }
}
