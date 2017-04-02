
package com.c0124.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.c0124.k9.*;
import com.c0124.k9.activity.Accounts;
import com.c0124.k9.activity.K9Activity;
import com.c0124.k9.helper.Utility;
import com.c0124.k9.R;

public class AccountSetupNames extends K9Activity implements OnClickListener {
    private static final String EXTRA_ACCOUNT = "account";

    private EditText mDescription;

    private EditText mName;

    private Account mAccount;

    private Button mDoneButton;
    
    private WebView mWelcomeText;
    
    public static void actionSetNames(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupNames.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_names);
       
        mDescription = (EditText)findViewById(R.id.account_description);
        mName = (EditText)findViewById(R.id.account_name);
        mDoneButton = (Button)findViewById(R.id.done);
        mDoneButton.setOnClickListener(this);
        mWelcomeText = (WebView) findViewById(R.id.webview_welcome_afteraccount);
        
        TextWatcher validationTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                validateFields();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        mName.addTextChangedListener(validationTextWatcher);

        mName.setKeyListener(TextKeyListener.getInstance(false, Capitalize.WORDS));

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        // display message text via webview
        if (mAccount != null) {
            String customHtml = getString(R.string.account_setup_names_welcome);
            String replaced;
            if (mAccount.getName() != null)
            {
                replaced = customHtml.replace("{EmailAccount}", mAccount.getEmail());
            }
            else
            {
                replaced = customHtml.replace("{EmailAccount}", "this email account");
            }
            replaced =
                replaced.replace("{New Key Pair}", "\"" + getString(R.string.c0124_key_pair_manager) + "\"");

            mWelcomeText.setBackgroundColor(Color.TRANSPARENT);
            mWelcomeText.loadDataWithBaseURL("file:///android_res/drawable/", replaced,
                "text/html", "utf-8", null);
        }

        if (mAccount.getName() != null) {
            mName.setText(mAccount.getName()); 
        }
        if (mAccount.getEmail() != null) {
            mDescription.setText(mAccount.getEmail());
        }
        if (!Utility.requiredFieldValid(mName)) {
            mDoneButton.setEnabled(false);
        }
        else
        {
            // Name is the default focus, if we fill it, switch to button.
            if (mAccount.getName()!=null && mAccount.getName().length()>0)
            {
                // TODO: try to hide keyboard and make a clear reading space for user.
                mDoneButton.setFocusableInTouchMode(true);
                mDoneButton.requestFocus();
            }
        }
    }

    private void validateFields() {
        mDoneButton.setEnabled(Utility.requiredFieldValid(mName));
        Utility.setCompoundDrawablesAlpha(mDoneButton, mDoneButton.isEnabled() ? 255 : 128);
    }

    protected void onNext() {
        if (Utility.requiredFieldValid(mDescription)) {
            mAccount.setDescription(mDescription.getText().toString());
        }
        mAccount.setName(mName.getText().toString());
        mAccount.save(Preferences.getPreferences(this));
        Accounts.listAccounts(this);
        finish();
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.done:
            onNext();
            break;
        }
    }
}
