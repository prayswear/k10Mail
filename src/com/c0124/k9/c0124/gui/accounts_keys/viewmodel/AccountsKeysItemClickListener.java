package com.c0124.k9.c0124.gui.accounts_keys.viewmodel;

import android.view.View;
import android.widget.AdapterView;

import com.c0124.k9.c0124.ClientHelper;

/**
 * Created by xinqian on 7/8/15.
 */
public class AccountsKeysItemClickListener implements AdapterView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        ClientHelper.v("item clicked", "clicked:" + position);
    }
}
