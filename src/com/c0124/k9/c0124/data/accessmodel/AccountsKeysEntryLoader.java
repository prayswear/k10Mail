package com.c0124.k9.c0124.data.accessmodel;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.c0124.k9.BaseAccount;
import com.c0124.k9.Preferences;
import com.c0124.k9.c0124.data.AccountsKeyEntry;
import com.c0124.k9.c0124.data.KeyStoreOpenHelper;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
* 1. It will load all the email accounts that have keys or have email accounts setupped in this computer.
* 2. For the
*
*  */
public class AccountsKeysEntryLoader extends AsyncTaskLoader<AbstractList<AccountsKeyEntry>> {
    public AccountsKeysEntryLoader(Context context) {
        super(context);
        onContentChanged();
    }

    @Override
    public synchronized AbstractList<AccountsKeyEntry> loadInBackground() {
        KeyStoreOpenHelper helper = new KeyStoreOpenHelper(getContext().getApplicationContext());
        return helper.getAllAccountsKeys(getEmailNames());
    }

    @Override
    protected void onStartLoading() {
        if (takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        onStopLoading();
    }

    protected AbstractList<String> getEmailNames() {
        ArrayList<String> emailNames = new ArrayList<String>();
        BaseAccount[] accounts = Preferences.getPreferences(this.getContext()).getAccounts();
        for (BaseAccount account : accounts) {
            emailNames.add(account.getEmail().toLowerCase(Locale.getDefault()));
        }
        return emailNames;
    }
}
