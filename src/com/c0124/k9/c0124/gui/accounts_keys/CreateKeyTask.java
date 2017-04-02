package com.c0124.k9.c0124.gui.accounts_keys;

import android.app.Activity;
import android.app.ProgressDialog;
import android.widget.Toast;

import com.c0124.k9.R;
import com.c0124.k9.activity.misc.ExtendedAsyncTask;
import com.c0124.k9.c0124.SCPGPProvider;
import com.c0124.k9.c0124.gui.accounts_keys.activity.AccountsKeys;

/**
 * Created by xinqian on 7/10/15.
 */
// TODO: put string into R.
public class CreateKeyTask extends ExtendedAsyncTask<String[], Integer, Boolean> {
    final int m_max;
    String m_emailAddress;

    public CreateKeyTask(Activity activity, int p_max) {
        super(activity);
        this.m_max = p_max;
    }

    @Override
    protected void showProgressDialog() {
        final String progressDialogMessage = mContext.getString(R.string.c0124_string_create_key_task_progress_dialog_message);
        mProgressDialog = new ProgressDialog(mActivity);
        mProgressDialog.setMessage(progressDialogMessage);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMax(m_max);
        mProgressDialog.setProgress(0);
        mProgressDialog.show();
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        mProgressDialog.setProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            Toast.makeText(mContext,
                    R.string.c0124_string_create_key_task_success_toast_message,
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(
                    mContext,
                    R.string.c0124_string_create_key_task_failed_toast_message,
                    Toast.LENGTH_LONG).show();
        }

        removeProgressDialog();
        if (this.mActivity instanceof AccountsKeys) {
            AccountsKeys accountsKeys = (AccountsKeys) this.mActivity;
            accountsKeys.refreshAccountsKeys();
        }
    }

    @Override
    protected Boolean doInBackground(String[]... arg0) {
        String[] accounts = arg0[0];
        Boolean result = true;
        int finished = 0;
        for (String account : accounts) {
            m_emailAddress = account;
            // TODO, return the failed reason from createNewPair.
            result = result && SCPGPProvider.getInstance().createNewPair(mActivity, account);
            if (!result)
                break;
            ++finished;
            publishProgress(finished);
        }

        return result;
    }
}
