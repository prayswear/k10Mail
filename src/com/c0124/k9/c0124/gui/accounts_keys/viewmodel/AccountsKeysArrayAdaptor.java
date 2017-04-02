package com.c0124.k9.c0124.gui.accounts_keys.viewmodel;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.c0124.k9.R;
import com.c0124.k9.c0124.ClientHelper;
import com.c0124.k9.c0124.data.AccountsKeyEntry;
import com.c0124.k9.c0124.gui.accounts_keys.activity.AccountsKeys;

import java.util.ArrayList;


public class AccountsKeysArrayAdaptor extends ArrayAdapter<AccountsKeyEntry> {
    Integer m_resource;

    public AccountsKeysArrayAdaptor(Context p_context, ArrayList<AccountsKeyEntry> p_items) {
        super(p_context, 0, p_items);
    }

    @Override
    public View getView(int position, View itemView, ViewGroup parent) {
        final AccountsKeyEntry accountsKeysEntry = getItem(position);

        if (itemView == null) {
            itemView = LayoutInflater.from(getContext()).inflate(R.layout.c0124_list_item_accounts_keys, parent, false);
        }

        TextView email = (TextView) itemView.findViewById(R.id.c0124_text_view_accounts_keys_item_email_account);
        TextView createTime = (TextView) itemView.findViewById(R.id.c0124_text_view_accounts_keys_item_create_time);
        TextView token = (TextView) itemView.findViewById(R.id.c0124_text_view_accounts_keys_item_token);
        ProgressBar progress = (ProgressBar) itemView.findViewById(R.id.c0124_progress_bar_accounts_keys_key_refresh);

        // Populate the data into the template view using the data object
        email.setText(accountsKeysEntry.email);
        createTime.setText(getKeyDateTimeString(accountsKeysEntry));
        token.setText(getTokenString(accountsKeysEntry));
        progress.setVisibility(getProgressVisibility(accountsKeysEntry));
        Button refreshButton = (Button) itemView.findViewById(R.id.c0124_button_accounts_keys_refresh);

        final String accountEmail = accountsKeysEntry.email;
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClientHelper.v("ViewItem", "going to refresh:" + accountEmail);
                AccountsKeys activity = (AccountsKeys)getContext();
                activity.createKeyForAccount(accountEmail);
            }
        });

        if (!accountsKeysEntry.isEmailHaveAccountSetup) {
            refreshButton.setVisibility(View.INVISIBLE);
        }
        // Return the completed view to render on screen
        return itemView;
    }

    String getKeyDateTimeString(AccountsKeyEntry p_accountsKeysEntry) {
        final String KEY_CREATED = getContext().getString(R.string.c0124_string_accounts_keys_keys_created_at);

        if (p_accountsKeysEntry.timeStamp > 0 && !p_accountsKeysEntry.token.isEmpty()) {
            return KEY_CREATED + ClientHelper.getSimpleDateTimeStringFromTimeStamp(p_accountsKeysEntry.timeStamp, getContext());
        } else {
            return getContext().getString(R.string.c0124_string_account_keys_textMessage_noKey);
        }
    }

    String getTokenString(AccountsKeyEntry p_accountsKeysEntry) {
        final String TOKEN = getContext().getString(R.string.c0124_string_accounts_keys_token);
        final String WAITING_FOR_VERIFY_EMAIL = getContext().getString(R.string.c0124_string_accounts_keys_waiting_for_verifying_email);
        if (!p_accountsKeysEntry.isRegistrationPending) {
            if (!p_accountsKeysEntry.token.isEmpty()) {
                return TOKEN + p_accountsKeysEntry.token;
            } else {
                return "";
            }
        } else if (!p_accountsKeysEntry.token.isEmpty()) {
            return TOKEN + p_accountsKeysEntry.token + ", " + WAITING_FOR_VERIFY_EMAIL;
        } else {
            return WAITING_FOR_VERIFY_EMAIL;
        }
    }

    int getProgressVisibility(AccountsKeyEntry p_accountsKeysEntry)
    {
        if (p_accountsKeysEntry.isRegistrationPending)
            return View.VISIBLE;
        else
            return View.GONE;
    }
}