package com.c0124.k9.c0124.gui.accounts_keys.activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.c0124.k9.BaseAccount;
import com.c0124.k9.K9;
import com.c0124.k9.Preferences;
import com.c0124.k9.R;
import com.c0124.k9.activity.K9ListActivity;
import com.c0124.k9.c0124.ClientHelper;
import com.c0124.k9.c0124.data.AccountsKeyEntry;
import com.c0124.k9.c0124.data.KeyStoreOpenHelper;
import com.c0124.k9.c0124.data.accessmodel.AccountsKeysEntryLoader;

import com.c0124.k9.c0124.gui.accounts_keys.ExportKeysTask;
import com.c0124.k9.c0124.gui.accounts_keys.ImportKeysTask;
import com.c0124.k9.c0124.gui.accounts_keys.viewmodel.AccountsKeysActivityModel;
import com.c0124.k9.c0124.gui.accounts_keys.viewmodel.AccountsKeysArrayAdaptor;
import com.c0124.k9.c0124.gui.accounts_keys.viewmodel.AccountsKeysItemClickListener;
import com.c0124.k9.c0124.gui.accounts_keys.viewmodel.AccountsKeysListExpandingClickListener;
import com.c0124.k9.c0124.gui.accounts_keys.viewmodel.ReloadDataClickListener;
import com.c0124.k9.c0124.gui.accounts_keys.CreateKeyTask;
import com.c0124.utility.functional.Func1;
import com.c0124.utility.functional.FunctionExecuter;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class AccountsKeys extends K9ListActivity
        implements LoaderManager.LoaderCallbacks<AbstractList<AccountsKeyEntry>> {

    AccountsKeysArrayAdaptor m_adaptor;
    AccountsKeysActivityModel m_activityModel;

    public static final String keyUpdatedIntentActionTag;
    private static final int ACTIVITY_REQUEST_PICK_KEYS_FILE = 10;
    private BroadcastReceiver m_BroadcastReceiver;

    static {
        keyUpdatedIntentActionTag = "com.c0124.k9.c0124.gui.accounts_keys.activity.KeysUpdatedAction";
    }

    {
        m_BroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (keyUpdatedIntentActionTag.equals(intent.getAction().toString())) {
                    ClientHelper.v(K9.LOG_TAG, "Receive key updated Broadcast.");
                    refreshAccountsKeys();
                }
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ClientHelper.v(K9.LOG_TAG, "create AccountsKeys activity");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.c0124_activity_accounts_keys);

        try {
            m_adaptor = new AccountsKeysArrayAdaptor(this, new ArrayList<AccountsKeyEntry>());
            ListView listView = getListView();
            listView.setAdapter(m_adaptor);
            listView.setOnItemClickListener(new AccountsKeysItemClickListener());

            ImageView indicator = (ImageView) findViewById(R.id.c0124_accounts_keys_list_is_expanding_indicator);
            TextView overallStatus = (TextView) findViewById(R.id.c0124_accounts_keys_overall_text);
            m_activityModel = new AccountsKeysActivityModel(this, m_adaptor, indicator,
                    findViewById(R.id.c0124_accounts_keys_list_separator),
                    overallStatus);
            AccountsKeysListExpandingClickListener listener = new AccountsKeysListExpandingClickListener(m_activityModel);
            indicator.setOnClickListener(listener);
            findViewById(R.id.c0124_accounts_keys_list_header).setOnClickListener(listener);
            findViewById(R.id.c0124_accounts_keys_list_title).setOnClickListener(listener);

            // Begin loading data via loader.
            getLoaderManager().initLoader(m_activityModel.getAccountsKeysLoaderIndex(), null, this);

            ReloadDataClickListener reloadListener = new ReloadDataClickListener(m_activityModel);
            //findViewById(R.id.c0124_accounts_keys_overall_baner).setOnClickListener(reloadListener);
            findViewById(R.id.c0124_accounts_keys_overall_text).setOnClickListener(reloadListener);
            findViewById(R.id.c0124_accounts_keys_overall_image).setOnClickListener(reloadListener);

            findViewById(R.id.c0124_accounts_keys_recreate_keys_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    createKeysForAllAccounts();
                }
            });

        } catch (Exception e) {
            ClientHelper.e("exception", "e:" + e.toString() + ", call stack:" + ClientHelper.getExceptionCallStack(e));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(keyUpdatedIntentActionTag);
        registerReceiver(m_BroadcastReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(m_BroadcastReceiver);

        m_activityModel.saveState();
    }

    @Override
    public void onStart() {
        super.onStart();
        m_activityModel.restoreState();
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        ClientHelper.v(K9.LOG_TAG, "menu created");
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.c0124_menu_accounts_keys, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        int id = item.getItemId();
        ClientHelper.v(K9.LOG_TAG, "menu clicked");

        if (id == R.id.c0124_menu_accounts_keys_refresh) {
            m_activityModel.refreshAccountsKeys();
            return true;
        } else if (id == R.id.c0124_menu_accounts_keys_recreate_keys) {
            createKeysForAllAccounts();
        } else if (id == R.id.c0124_menu_accounts_keys_clear_all_keys) {
            // TODO: ask, tell the consequence and then clear.
            clearAllKeys();
        }
        else if (id == R.id.c0124_menu_accounts_keys_export_all_keys)
        {
            ClientHelper.v(ClientHelper.LogTag, "export all keys");
            ExportKeysTask exportKeysTask = new ExportKeysTask(this);
            exportKeysTask.execute();
        }
        else if (id == R.id.c0124_menu_accounts_keys_import_all_keys)
        {
            ClientHelper.v(ClientHelper.LogTag, "import all keys");
            startImportKeys();
        }
        return super.onOptionsItemSelected(item);
    }

    /*==============================================================================================
            Following three method are for LoaderManager.LoaderCallbacks<Cursor>
    ==============================================================================================*/
    @Override
    public Loader<AbstractList<AccountsKeyEntry>> onCreateLoader(int i, Bundle bundle) {
        return new AccountsKeysEntryLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<AbstractList<AccountsKeyEntry>> loader, AbstractList<AccountsKeyEntry> accountsKeyEntries) {
        m_activityModel.setAccountsKeysData(accountsKeyEntries);
    }

    @Override
    public void onLoaderReset(Loader<AbstractList<AccountsKeyEntry>> loader) {
        m_activityModel.setAccountsKeysData(new ArrayList<AccountsKeyEntry>());
    }

    /*==============================================================================================
            These task should be put into m_activityModel.
    ===============================================================================================*/
    public void clearAllKeys() {
        FunctionExecuter<Void> yesAction = new Func1<Void, Activity>(this) {
            public Void executor() {
                KeyStoreOpenHelper keyStore = new KeyStoreOpenHelper(m_p0);
                keyStore.clearDB();
                m_activityModel.refreshAccountsKeys();
                ClientHelper.i(ClientHelper.LogTag, "Doing keys clear!");
                return null;
            }
        };

        FunctionExecuter<Void> noAction = new FunctionExecuter<Void>() {
            public Void executor() {
                ClientHelper.i(ClientHelper.LogTag, "Canceled keys clear!");
                return null;
            }
        };
        final String title = getString(R.string.c0124_string_accounts_keys_clear_all_keys_warning);
        final String message = getString(R.string.c0124_string_accounts_keys_clear_all_keys_warning_yes_no);
        ClientHelper.showYesOrNoMessage(title, message, this, yesAction, noAction);
    }

    public void refreshAccountsKeys() {
        m_activityModel.refreshAccountsKeys();
    }

    public void createKeyForAccount(String email) {
        CreateKeyTask asyncTask = new CreateKeyTask(this, 1);
        asyncTask.execute(new String[]{email});
    }

    protected void createKeysForAllAccounts() {
        BaseAccount[] accounts = Preferences.getPreferences(this).getAccounts();
        String[] emails = new String[accounts.length];
        int i = 0;
        for (BaseAccount a : accounts) {
            ClientHelper.i("KeyGeneration", "email address:" + a.getEmail() + ", desc:"
                    + a.getDescription());
            emails[i] = a.getEmail();
            ++i;
        }
        CreateKeyTask asyncTask = new CreateKeyTask(this, accounts.length);
        asyncTask.execute(emails);
    }

    private void startImportKeys() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/PgpKeys*.txt");

        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> infoList = packageManager.queryIntentActivities(i, 0);

        if (infoList.size() > 0) {
            startActivityForResult(Intent.createChooser(i, null),
                    ACTIVITY_REQUEST_PICK_KEYS_FILE);
        } else {
            ClientHelper.showMessage("Import Keys", "Cannot show file pickup interface!", this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ClientHelper.i(ClientHelper.LogTag, "onActivityResult requestCode = " + requestCode + ", resultCode = " + resultCode + ", data = " + data);

        if (resultCode != RESULT_OK)
            return;

        if (data == null) {
            return;
        }

        switch (requestCode) {
            case ACTIVITY_REQUEST_PICK_KEYS_FILE:
                onImportKeys(data.getData());
                break;
        }
    }

    private void onImportKeys(android.net.Uri filename)
    {
        ClientHelper.i(ClientHelper.LogTag, "importing:" + filename);
        ImportKeysTask importKeysTask = new ImportKeysTask(this, filename.getPath());
        importKeysTask.execute();
    }
}


