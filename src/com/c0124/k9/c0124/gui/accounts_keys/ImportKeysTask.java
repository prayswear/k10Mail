package com.c0124.k9.c0124.gui.accounts_keys;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;

import com.c0124.k9.R;
import com.c0124.k9.activity.misc.ExtendedAsyncTask;
import com.c0124.k9.c0124.ClientHelper;
import com.c0124.k9.c0124.data.KeyStoreOpenHelper;
import com.c0124.k9.c0124.exception.ExportKeysFailedException;
import com.c0124.k9.helper.Utility;
import com.c0124.transfer.KeyEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.List;

/**
 * Created by xinqian on 9/15/15.
 */
public class ImportKeysTask extends ExtendedAsyncTask<Void, Void, Boolean> {
    String m_filename;
    Exception m_exception;
    int m_importedKeysCount;

    public ImportKeysTask(Activity p_activity, String p_filename) {
        super(p_activity);

        ClientHelper.i(ClientHelper.LogTag, "importing:" + p_filename);
        m_filename = p_filename;
        m_exception = null;
        m_importedKeysCount = 0;
    }

    @Override
    protected void showProgressDialog() {
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            ByteBuffer buffer = readBufferFromFile(m_filename);
            if (buffer == null)
                return false;

            List<KeyEntry> keys = KeyStoreOpenHelper.getDeSerializedKeysFromBytes(buffer);
            KeyStoreOpenHelper helper =
                    new KeyStoreOpenHelper(this.mActivity);
            SQLiteDatabase db = helper.getWritableDatabase();
            m_importedKeysCount = helper.importKeys(db, keys);

        } catch (Exception e) {
            ClientHelper.w(ClientHelper.LogTag, "Exception during import" + e.getMessage());
            m_exception = e;
            return false;
        }
        return true;
    }

    ByteBuffer readBufferFromFile(String filename){
        FileInputStream fin = null;
        try {
            File file = new File(filename);
            fin = new FileInputStream(file);
            byte fileContent[] = new byte[(int)file.length()];
            fin.read(fileContent);
            return ByteBuffer.wrap(fileContent);
        }
        catch (FileNotFoundException e) {
            ClientHelper.w(ClientHelper.LogTag, "FileNotFoundException during import" + e.getMessage());
        }
        catch (IOException ioe) {
            ClientHelper.w(ClientHelper.LogTag, "IOException during import" + ioe.getMessage());
        }
        finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            }
            catch (IOException ioe) {
                ClientHelper.w(ClientHelper.LogTag, "Exception during export" + ioe.getMessage());
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success && m_importedKeysCount > 0) {
            ClientHelper.showMessage(mActivity.getString(R.string.c0124_string_import_keys_imported_text),
                    String.format(mContext.getString(R.string.c0124_string_import_keys_imported_message), m_importedKeysCount, m_filename), mActivity);
        } else {
            String message = (m_exception == null) ? String.format(mContext.getString(R.string.c0124_string_import_keys_nokey_text), m_filename)
                    : (mContext.getString(R.string.c0124_string_failed_text) + ", " + m_exception.getMessage() + ".");
            ClientHelper.showMessage(mContext.getString(R.string.c0124_string_import_key_importing_text), message, mActivity);
        }
    }
}
