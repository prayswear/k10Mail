package com.c0124.k9.c0124.gui.accounts_keys;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;


import com.c0124.k9.K9;
import com.c0124.k9.R;
import com.c0124.k9.activity.misc.ExtendedAsyncTask;
import com.c0124.k9.c0124.ClientHelper;
import com.c0124.k9.c0124.data.KeyStoreOpenHelper;
import com.c0124.k9.helper.Utility;
import com.c0124.transfer.KeyEntry;
import com.c0124.k9.c0124.exception.ExportKeysFailedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.AbstractList;

/**
 * Handles exporting of global settings and/or accounts in a background thread.
 */
public class ExportKeysTask extends ExtendedAsyncTask<Void, Void, Boolean> {
    String m_filename;
    Exception m_exception;
    public final static String ExportFileName;

    static
    {
        // TODO: chinese version might have different names.
        ExportFileName = "PgpKeys.txt";
    }


    public ExportKeysTask(Activity activity) {
        super(activity);
        m_exception = null;
    }

    @Override
    protected void showProgressDialog() {
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            KeyStoreOpenHelper helper =
                    new KeyStoreOpenHelper(this.mActivity);
            SQLiteDatabase db = helper.getWritableDatabase();
            AbstractList<KeyEntry> keys = helper.GetAllKeys();
            ByteBuffer serializedBuffer;
            serializedBuffer = KeyStoreOpenHelper.getSerializedBytesFromKeys(keys);
            m_filename = exportBuffer(serializedBuffer);
        } catch (Exception e) {
            m_exception = e;
            ClientHelper.w(ClientHelper.LogTag, "Exception during export" + e.getMessage());
            return false;
        }
        return true;
    }

    String exportBuffer(ByteBuffer serializedBuffer) throws ExportKeysFailedException {
        OutputStream os = null;
        String filename = null;
        try {
            File dir = new File(Environment.getExternalStorageDirectory() + File.separator
                    + this.mContext.getPackageName());
            dir.mkdirs();
            File file = Utility.createUniqueFile(dir, ExportFileName);
            filename = file.getAbsolutePath();
            os = new FileOutputStream(filename);
            os.write(serializedBuffer.array(), serializedBuffer.arrayOffset(), serializedBuffer.remaining());
            return filename;
        } catch (Exception e) {
            throw new ExportKeysFailedException("cannot write to" + filename + ", exception :" + e.getMessage(), e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e) {
                    ClientHelper.w(ClientHelper.LogTag, "Couldn't close exported keys file: " + filename);
                    throw new ExportKeysFailedException("Couldn't close exported keys file: " + filename + ", exception :" + e.getMessage(), e);
                }
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            ClientHelper.showMessage(mContext.getString(com.c0124.k9.R.string.c0124_string_export_keys_text_exported), m_filename, mActivity);

            MediaScannerConnection.scanFile(this.mActivity,
                    new String[]{m_filename}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            ClientHelper.i(ClientHelper.LogTag, "Scanned " + path + ":");
                            ClientHelper.i(ClientHelper.LogTag, "-> uri=" + uri);
                        }
                    });
        } else {
            ClientHelper.showMessage(mContext.getString(R.string.c0124_string_export_keys_failed_title),
                    mContext.getString(R.string.c0124_string_export_keys_failed_message), mActivity);
        }
    }
}
