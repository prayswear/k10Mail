package com.c0124.k9.c0124.gui.accounts_keys.viewmodel;

import android.view.View;

/**
 * Created by xinqian on 7/19/15.
 */
public class AccountsKeysListExpandingClickListener implements View.OnClickListener {
    AccountsKeysActivityModel m_model;

    public AccountsKeysListExpandingClickListener(AccountsKeysActivityModel p_model) {
        m_model = p_model;
    }

    @Override
    public void onClick(View view) {
        m_model.changeAccountsKeysListExpanding();
    }
}
