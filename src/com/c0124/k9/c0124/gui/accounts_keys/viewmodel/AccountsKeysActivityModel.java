package com.c0124.k9.c0124.gui.accounts_keys.viewmodel;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.c0124.k9.K9;
import com.c0124.k9.R;
import com.c0124.k9.c0124.ClientHelper;
import com.c0124.k9.c0124.data.AccountsKeyEntry;
import com.c0124.k9.c0124.data.DataUtility;
import com.c0124.k9.c0124.gui.accounts_keys.activity.AccountsKeys;

import java.util.AbstractList;
import java.util.ArrayList;


/**
 * Created by xinqian on 7/18/15.
 */
public class AccountsKeysActivityModel {
    private boolean m_isAccountsKeyListExpanding;
    private AbstractList<AccountsKeyEntry> m_accountsKeysData;
    private AccountsKeysArrayAdaptor m_accountsKeysListAdaptor;
    private ImageView m_accountsKeyListExpandingIdicator;
    private View m_listTopSeparator;
    private TextView m_overAllStatusView;
    private AccountsKeys m_context;

    static final int s_accountsKeysLoaderIndex = 0;
    static final int s_separatorColor_expanded = android.R.color.holo_blue_light;
    static final int s_separatorColor_folded = android.R.color.holo_blue_dark;

    public AccountsKeysActivityModel(AccountsKeys p_context,
                                     AccountsKeysArrayAdaptor p_accountsKeysListAdaptor,
                                     ImageView p_accountsKeyListExpandingIndicator,
                                     View p_listTopSeparator,
                                     TextView p_overAllStatusView) {
        m_context = p_context;
        m_accountsKeyListExpandingIdicator = p_accountsKeyListExpandingIndicator;
        m_accountsKeysListAdaptor = p_accountsKeysListAdaptor;
        m_listTopSeparator = p_listTopSeparator;
        m_accountsKeysData = new ArrayList<AccountsKeyEntry>();
        m_overAllStatusView = p_overAllStatusView;

        // by default, folding the expand list.
        m_isAccountsKeyListExpanding = false;
        m_listTopSeparator.setBackgroundColor(m_context.getResources().getColor(s_separatorColor_folded));
        m_accountsKeyListExpandingIdicator.setImageResource(android.R.drawable.arrow_up_float);
    }

    public DataUtility.OverAllStatus getOverAllStatus() {
        return  DataUtility.getOverAllStatus(m_accountsKeysData);
    }

    protected String getOverAllStatusStatement(DataUtility.OverAllStatus overAllStatus) {
        switch (overAllStatus) {
            case AllEmailAccountsAreReadyForSendEncryptingEmails:
                return m_context.getString(R.string.c0124_string_overall_status_allEmailsAccountsAreReadyForEmail);
            case AllEmailAccountsAreReadyForSendEncryptingEmailsWhileSomeEmailAccountsInWaitingForKeyVerifying:
                return m_context.getString(R.string.c0124_string_overall_status_allEmailAccountsAreReadyForSendEncryptingEmailsWhileSomeEmailAccountsInWaitingForKeyVerifying);
            case SomeEmailAccountsNoKeyAndAreWaitingForKeyVerifying:
                return m_context.getString(R.string.c0124_string_overall_status_waitingForVerifyEmail);
            case NoKeysAtAll:
                break;
            case SomeEmailsAreWithoutValidKeyAndNoRegistrationGoingon:
                break;
        }
        return m_context.getString(R.string.c0124_string_overall_status_noKeysAtAll);
    }

    protected void showAccountKeysData() {
        ClientHelper.v(K9.LOG_TAG, "overall:" + getOverAllStatus());
        m_overAllStatusView.setText(getOverAllStatusStatement(getOverAllStatus()));
        if (m_isAccountsKeyListExpanding) {
            m_accountsKeysListAdaptor.clear();
            m_accountsKeysListAdaptor.addAll(m_accountsKeysData);
        }
    }

    public void setAccountsKeysData(AbstractList<AccountsKeyEntry> p_accountsKeysData) {
        if (p_accountsKeysData == null) {
            m_accountsKeysData = new ArrayList<AccountsKeyEntry>();
        } else {
            m_accountsKeysData = p_accountsKeysData;
        }
        showAccountKeysData();
    }

    protected void foldAccountsKeysList() {
        ClientHelper.v(K9.LOG_TAG, "fold:" + m_isAccountsKeyListExpanding);
        m_isAccountsKeyListExpanding = false;
        m_accountsKeyListExpandingIdicator.setImageResource(android.R.drawable.arrow_up_float);
        m_accountsKeysListAdaptor.clear();
        m_listTopSeparator.setBackgroundColor(m_context.getResources().getColor(s_separatorColor_folded));
    }

    protected void expandAccountsKeysList() {
        ClientHelper.v(K9.LOG_TAG, "expanding:" + m_isAccountsKeyListExpanding);
        m_isAccountsKeyListExpanding = true;
        m_accountsKeysListAdaptor.clear();
        m_accountsKeyListExpandingIdicator.setImageResource(android.R.drawable.arrow_down_float);
        m_accountsKeysListAdaptor.addAll(m_accountsKeysData);
        m_listTopSeparator.setBackgroundColor(m_context.getResources().getColor(s_separatorColor_expanded));
    }

    public void changeAccountsKeysListExpanding() {
        if (m_isAccountsKeyListExpanding)
            foldAccountsKeysList();
        else
            expandAccountsKeysList();
    }

    static final String s_accountsKeysListIsExpanding = "isAccountsKeysListExpanding";
    public void saveState() {
        ClientHelper.v(K9.LOG_TAG, "save instance:" + m_isAccountsKeyListExpanding);

        SharedPreferences sharedPref = m_context.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(s_accountsKeysListIsExpanding, m_isAccountsKeyListExpanding);
        editor.commit();
    }

    public void restoreState() {
        SharedPreferences sharedPref = m_context.getPreferences(Context.MODE_PRIVATE);
        m_isAccountsKeyListExpanding = sharedPref.getBoolean(s_accountsKeysListIsExpanding, false);
        ClientHelper.v(K9.LOG_TAG, "got instance:" + m_isAccountsKeyListExpanding);

        if (m_isAccountsKeyListExpanding)
            expandAccountsKeysList();
        else
            foldAccountsKeysList();
    }

    public void refreshAccountsKeys() {
        m_context.getLoaderManager().restartLoader(s_accountsKeysLoaderIndex, null, m_context);
    }

    public static int getAccountsKeysLoaderIndex() {
        return s_accountsKeysLoaderIndex;
    }
}
