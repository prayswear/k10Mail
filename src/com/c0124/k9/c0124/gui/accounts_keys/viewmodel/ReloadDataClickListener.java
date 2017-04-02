package com.c0124.k9.c0124.gui.accounts_keys.viewmodel;

import android.view.View;

import com.c0124.k9.c0124.gui.accounts_keys.activity.AccountsKeys;

/**
 * Created by xinqian on 7/20/15.
 */
public class ReloadDataClickListener implements View.OnClickListener {
    AccountsKeysActivityModel m_accountsKeysModel;

    public ReloadDataClickListener(AccountsKeysActivityModel p_model) {
        m_accountsKeysModel = p_model;
    }

    @Override
    public void onClick(View view) {
        m_accountsKeysModel.refreshAccountsKeys();
    }
}
