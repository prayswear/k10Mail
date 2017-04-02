package com.c0124.k9.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.*;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.c0124.k9.Account;
import com.c0124.k9.BaseAccount;
import com.c0124.k9.K9;
import com.c0124.k9.activity.Accounts;
import com.c0124.k9.activity.misc.ExtendedAsyncTask;
import com.c0124.k9.c0124.ClientHelper;
import com.c0124.k9.c0124.K9ModificationAnnotation;
import com.c0124.k9.c0124.SCPGPProvider;
import com.c0124.k9.controller.MessagingController;
import com.c0124.k9.controller.MessagingListener;
import com.c0124.k9.crypto.CryptoProvider;
import com.c0124.k9.c0124.exception.SCPGPPrivateKeyNotFoundException;
import com.c0124.k9.crypto.PgpData;
import com.c0124.k9.fragment.MessageViewFragment;
import com.c0124.k9.helper.ClipboardManager;
import com.c0124.k9.helper.Contacts;
import com.c0124.k9.helper.HtmlConverter;
import com.c0124.k9.helper.Utility;
import com.c0124.k9.mail.*;
import com.c0124.k9.mail.internet.MimeBodyPart;
import com.c0124.k9.mail.internet.MimeMessage;
import com.c0124.k9.mail.internet.MimeMultipart;
import com.c0124.k9.mail.internet.MimeUtility;
import com.c0124.k9.mail.internet.TextBody;
import com.c0124.k9.mail.store.LocalStore;
import com.c0124.k9.mail.store.LocalStore.LocalAttachmentBodyPart;
import com.c0124.k9.mail.store.LocalStore.LocalFolder;
import com.c0124.k9.mail.store.LocalStore.LocalMessage;
import com.c0124.k9.provider.AttachmentProvider;
import com.c0124.k9.provider.AttachmentProvider.AttachmentProviderColumns;
import com.c0124.k9.R;

import org.acra.ACRA;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.james.mime4j.util.MimeUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import android.app.AlertDialog;
import android.content.DialogInterface;

@K9ModificationAnnotation(author = "Jing Teng", description = "Receive emails")
public class SingleMessageView extends LinearLayout implements OnClickListener,
        MessageHeader.OnLayoutChangedListener, OnCreateContextMenuListener {
    private static final int MENU_ITEM_LINK_VIEW = Menu.FIRST;
    private static final int MENU_ITEM_LINK_SHARE = Menu.FIRST + 1;
    private static final int MENU_ITEM_LINK_COPY = Menu.FIRST + 2;

    private static final int MENU_ITEM_IMAGE_VIEW = Menu.FIRST;
    private static final int MENU_ITEM_IMAGE_SAVE = Menu.FIRST + 1;
    private static final int MENU_ITEM_IMAGE_COPY = Menu.FIRST + 2;

    private static final int MENU_ITEM_PHONE_CALL = Menu.FIRST;
    private static final int MENU_ITEM_PHONE_SAVE = Menu.FIRST + 1;
    private static final int MENU_ITEM_PHONE_COPY = Menu.FIRST + 2;

    private static final int MENU_ITEM_EMAIL_SEND = Menu.FIRST;
    private static final int MENU_ITEM_EMAIL_SAVE = Menu.FIRST + 1;
    private static final int MENU_ITEM_EMAIL_COPY = Menu.FIRST + 2;

    private static final String[] ATTACHMENT_PROJECTION = new String[] {
        AttachmentProviderColumns._ID,
        AttachmentProviderColumns.DISPLAY_NAME
    };
    private static final int DISPLAY_NAME_INDEX = 1;

    private static final String TAG = "SingleMessageView";
    
    private Fragment mFragment;
    private boolean mScreenReaderEnabled;
    private MessageCryptoView mCryptoView;
    private MessageWebView mMessageContentView;
    private AccessibleWebView mAccessibleMessageContentView;
    private MessageHeader mHeaderContainer;
    private LinearLayout mAttachments;
    private Button mShowHiddenAttachments;
    private LinearLayout mHiddenAttachments;
    private View mShowPicturesAction;
    private View mShowMessageAction;
    private View mShowAttachmentsAction;
    private boolean mShowPictures;
    private boolean mHasAttachments;
    private Button mDownloadRemainder;
    private LayoutInflater mInflater;
    private Contacts mContacts;
    private AttachmentView.AttachmentFileDownloadCallback attachmentCallback;
    private View mAttachmentsContainer;
    private SavedState mSavedState;
    private ClipboardManager mClipboardManager;
    private String mText;


    public void initialize(Fragment fragment) {
    	mFragment = fragment;
        Activity activity = fragment.getActivity();
        mMessageContentView = (MessageWebView) findViewById(R.id.message_content);
        mAccessibleMessageContentView = (AccessibleWebView) findViewById(R.id.accessible_message_content);
        mMessageContentView.configure();
        activity.registerForContextMenu(mMessageContentView);
        mMessageContentView.setOnCreateContextMenuListener(this);

        mHeaderContainer = (MessageHeader) findViewById(R.id.header_container);
        mHeaderContainer.setOnLayoutChangedListener(this);

        mAttachmentsContainer = findViewById(R.id.attachments_container);
        mAttachments = (LinearLayout) findViewById(R.id.attachments);
        mHiddenAttachments = (LinearLayout) findViewById(R.id.hidden_attachments);
        mHiddenAttachments.setVisibility(View.GONE);
        mShowHiddenAttachments = (Button) findViewById(R.id.show_hidden_attachments);
        mShowHiddenAttachments.setVisibility(View.GONE);
        mCryptoView = (MessageCryptoView) findViewById(R.id.layout_decrypt);
        mCryptoView.setFragment(fragment);
        mCryptoView.setupChildViews();
        mShowPicturesAction = findViewById(R.id.show_pictures);
        mShowMessageAction = findViewById(R.id.show_message);

        mShowAttachmentsAction = findViewById(R.id.show_attachments);

        mShowPictures = false;

        mContacts = Contacts.getInstance(activity);

        mInflater = ((MessageViewFragment) fragment).getFragmentLayoutInflater();
        mDownloadRemainder = (Button) findViewById(R.id.download_remainder);
        mDownloadRemainder.setVisibility(View.GONE);
        mAttachmentsContainer.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
                isScreenReaderActive(activity)) {
            // Only use the special screen reader mode on pre-ICS devices with active screen reader
            mAccessibleMessageContentView.setVisibility(View.VISIBLE);
            mMessageContentView.setVisibility(View.GONE);
            mScreenReaderEnabled = true;
        } else {
            mAccessibleMessageContentView.setVisibility(View.GONE);
            mMessageContentView.setVisibility(View.VISIBLE);
            mScreenReaderEnabled = false;

            // the HTC version of WebView tries to force the background of the
            // titlebar, which is really unfair.
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.messageViewHeaderBackgroundColor, outValue, true);
            mHeaderContainer.setBackgroundColor(outValue.data);
            // also set background of the whole view (including the attachments view)
            setBackgroundColor(outValue.data);
        }

        mShowHiddenAttachments.setOnClickListener(this);
        mShowMessageAction.setOnClickListener(this);
        mShowAttachmentsAction.setOnClickListener(this);
        mShowPicturesAction.setOnClickListener(this);

        mClipboardManager = ClipboardManager.getInstance(activity);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu);

        WebView webview = (WebView) v;
        WebView.HitTestResult result = webview.getHitTestResult();

        if (result == null) {
            return;
        }

        int type = result.getType();
        Context context = getContext();

        switch (type) {
            case HitTestResult.SRC_ANCHOR_TYPE: {
                final String url = result.getExtra();
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case MENU_ITEM_LINK_VIEW: {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                getContext().startActivity(intent);
                                break;
                            }
                            case MENU_ITEM_LINK_SHARE: {
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("text/plain");
                                intent.putExtra(Intent.EXTRA_TEXT, url);
                                getContext().startActivity(intent);
                                break;
                            }
                            case MENU_ITEM_LINK_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_link_clipboard_label);
                                mClipboardManager.setText(label, url);
                                break;
                            }
                        }
                        return true;
                    }
                };

                menu.setHeaderTitle(url);

                menu.add(Menu.NONE, MENU_ITEM_LINK_VIEW, 0,
                        context.getString(R.string.webview_contextmenu_link_view_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_LINK_SHARE, 1,
                        context.getString(R.string.webview_contextmenu_link_share_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_LINK_COPY, 2,
                        context.getString(R.string.webview_contextmenu_link_copy_action))
                        .setOnMenuItemClickListener(listener);

                break;
            }
            case HitTestResult.IMAGE_TYPE:
            case HitTestResult.SRC_IMAGE_ANCHOR_TYPE: {
                final String url = result.getExtra();
                final boolean externalImage = url.startsWith("http");
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case MENU_ITEM_IMAGE_VIEW: {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                if (!externalImage) {
                                    // Grant read permission if this points to our
                                    // AttachmentProvider
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                }
                                getContext().startActivity(intent);
                                break;
                            }
                            case MENU_ITEM_IMAGE_SAVE: {
                                new DownloadImageTask().execute(url);
                                break;
                            }
                            case MENU_ITEM_IMAGE_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_image_clipboard_label);
                                mClipboardManager.setText(label, url);
                                break;
                            }
                        }
                        return true;
                    }
                };

                menu.setHeaderTitle((externalImage) ?
                        url : context.getString(R.string.webview_contextmenu_image_title));

                menu.add(Menu.NONE, MENU_ITEM_IMAGE_VIEW, 0,
                        context.getString(R.string.webview_contextmenu_image_view_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_IMAGE_SAVE, 1,
                        (externalImage) ?
                            context.getString(R.string.webview_contextmenu_image_download_action) :
                            context.getString(R.string.webview_contextmenu_image_save_action))
                        .setOnMenuItemClickListener(listener);

                if (externalImage) {
                    menu.add(Menu.NONE, MENU_ITEM_IMAGE_COPY, 2,
                            context.getString(R.string.webview_contextmenu_image_copy_action))
                            .setOnMenuItemClickListener(listener);
                }

                break;
            }
            case HitTestResult.PHONE_TYPE: {
                final String phoneNumber = result.getExtra();
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case MENU_ITEM_PHONE_CALL: {
                                Uri uri = Uri.parse(WebView.SCHEME_TEL + phoneNumber);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                getContext().startActivity(intent);
                                break;
                            }
                            case MENU_ITEM_PHONE_SAVE: {
                                Contacts contacts = Contacts.getInstance(getContext());
                                contacts.addPhoneContact(phoneNumber);
                                break;
                            }
                            case MENU_ITEM_PHONE_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_phone_clipboard_label);
                                mClipboardManager.setText(label, phoneNumber);
                                break;
                            }
                        }

                        return true;
                    }
                };

                menu.setHeaderTitle(phoneNumber);

                menu.add(Menu.NONE, MENU_ITEM_PHONE_CALL, 0,
                        context.getString(R.string.webview_contextmenu_phone_call_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_PHONE_SAVE, 1,
                        context.getString(R.string.webview_contextmenu_phone_save_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_PHONE_COPY, 2,
                        context.getString(R.string.webview_contextmenu_phone_copy_action))
                        .setOnMenuItemClickListener(listener);

                break;
            }
            case WebView.HitTestResult.EMAIL_TYPE: {
                final String email = result.getExtra();
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case MENU_ITEM_EMAIL_SEND: {
                                Uri uri = Uri.parse(WebView.SCHEME_MAILTO + email);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                getContext().startActivity(intent);
                                break;
                            }
                            case MENU_ITEM_EMAIL_SAVE: {
                                Contacts contacts = Contacts.getInstance(getContext());
                                contacts.createContact(new Address(email));
                                break;
                            }
                            case MENU_ITEM_EMAIL_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_email_clipboard_label);
                                mClipboardManager.setText(label, email);
                                break;
                            }
                        }

                        return true;
                    }
                };

                menu.setHeaderTitle(email);

                menu.add(Menu.NONE, MENU_ITEM_EMAIL_SEND, 0,
                        context.getString(R.string.webview_contextmenu_email_send_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_EMAIL_SAVE, 1,
                        context.getString(R.string.webview_contextmenu_email_save_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_EMAIL_COPY, 2,
                        context.getString(R.string.webview_contextmenu_email_copy_action))
                        .setOnMenuItemClickListener(listener);

                break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.show_hidden_attachments: {
                onShowHiddenAttachments();
                break;
            }
            case R.id.show_message: {
                onShowMessage();
                break;
            }
            case R.id.show_attachments: {
                onShowAttachments();
                break;
            }
            case R.id.show_pictures: {
                // Allow network access first...
                setLoadPictures(true);
                // ...then re-populate the WebView with the message text
                loadBodyFromText(mText);
                break;
            }
        }
    }

    private void onShowHiddenAttachments() {
        mShowHiddenAttachments.setVisibility(View.GONE);
        mHiddenAttachments.setVisibility(View.VISIBLE);
    }

    public void onShowMessage() {
        showShowMessageAction(false);
        showAttachments(false);
        showShowAttachmentsAction(mHasAttachments);
        showMessageWebView(true);
    }

    public void onShowAttachments() {
        showMessageWebView(false);
        showShowAttachmentsAction(false);
        showShowMessageAction(true);
        showAttachments(true);
    }

    public SingleMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    private boolean isScreenReaderActive(Activity activity) {
        final String SCREENREADER_INTENT_ACTION = "android.accessibilityservice.AccessibilityService";
        final String SCREENREADER_INTENT_CATEGORY = "android.accessibilityservice.category.FEEDBACK_SPOKEN";
        // Restrict the set of intents to only accessibility services that have
        // the category FEEDBACK_SPOKEN (aka, screen readers).
        Intent screenReaderIntent = new Intent(SCREENREADER_INTENT_ACTION);
        screenReaderIntent.addCategory(SCREENREADER_INTENT_CATEGORY);
        List<ResolveInfo> screenReaders = activity.getPackageManager().queryIntentServices(
                                              screenReaderIntent, 0);
        ContentResolver cr = activity.getContentResolver();
        Cursor cursor = null;
        int status = 0;
        for (ResolveInfo screenReader : screenReaders) {
            // All screen readers are expected to implement a content provider
            // that responds to
            // content://<nameofpackage>.providers.StatusProvider
            cursor = cr.query(Uri.parse("content://" + screenReader.serviceInfo.packageName
                                        + ".providers.StatusProvider"), null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    // These content providers use a special cursor that only has
                    // one element,
                    // an integer that is 1 if the screen reader is running.
                    status = cursor.getInt(0);
                    if (status == 1) {
                        return true;
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return false;
    }

    public boolean showPictures() {
        return mShowPictures;
    }

    public void setShowPictures(Boolean show) {
        mShowPictures = show;
    }

    /**
     * Enable/disable image loading of the WebView. But always hide the
     * "Show pictures" button!
     *
     * @param enable true, if (network) images should be loaded.
     *               false, otherwise.
     */
    public void setLoadPictures(boolean enable) {
        mMessageContentView.blockNetworkData(!enable);
        setShowPictures(enable);
        showShowPicturesAction(false);
    }

    public Button downloadRemainderButton() {
        return  mDownloadRemainder;
    }

    public void showShowPicturesAction(boolean show) {
        mShowPicturesAction.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    public void showShowMessageAction(boolean show) {
        mShowMessageAction.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    public void showShowAttachmentsAction(boolean show) {
        mShowAttachmentsAction.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Fetch the message header view.  This is not the same as the message headers; this is the View shown at the top
     * of messages.
     * @return MessageHeader View.
     */
    public MessageHeader getMessageHeaderView() {
        return mHeaderContainer;
    }

    public void setHeaders(final Message message, Account account) {
        try {
            mHeaderContainer.populate(message, account);
            mHeaderContainer.setVisibility(View.VISIBLE);


        } catch (Exception me) {
            Log.e(K9.LOG_TAG, "setHeaders - error", me);
        }
    }

    public void setShowDownloadButton(Message message) {
        if (message.isSet(Flag.X_DOWNLOADED_FULL)) {
            mDownloadRemainder.setVisibility(View.GONE);
        } else {
            mDownloadRemainder.setEnabled(true);
            mDownloadRemainder.setVisibility(View.VISIBLE);
        }
    }

    public void setOnFlagListener(OnClickListener listener) {
        mHeaderContainer.setOnFlagListener(listener);
    }

    public void showAllHeaders() {
        mHeaderContainer.onShowAdditionalHeaders();
    }

    public boolean additionalHeadersVisible() {
        return mHeaderContainer.additionalHeadersVisible();
    }

    static private InputStream decryptMessage(Account account, InputStream input, Context context) throws SCPGPPrivateKeyNotFoundException{
        final SCPGPProvider crypto = (SCPGPProvider) account.getCryptoProvider();
        // in k10 SCPGPProvider is the only CryptoProvider that can be created
        ByteArrayOutputStream t1 = new ByteArrayOutputStream();
        try {
            IOUtils.copy(input, t1);
            String str = new String(t1.toByteArray());
            ByteArrayInputStream t2 = new ByteArrayInputStream(t1.toByteArray());
            input = t2;
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        ByteArrayOutputStream strOut = new ByteArrayOutputStream();
        crypto.decrypt(context, account, input, strOut);
        return new ByteArrayInputStream(strOut.toByteArray());
    }
    
    private static class DownloadAttachmentTask extends ExtendedAsyncTask<Object, Integer, Integer> {

        protected DownloadAttachmentTask(Activity activity) {
            super(activity);
        }

        @Override
        protected void showProgressDialog() {
            mProgressDialog = ProgressDialog.show(mActivity, null, "Downloading message", true);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {}

        @Override
        protected void onPostExecute(Integer result) {
            removeProgressDialog();
        }

        @Override
        protected Integer doInBackground(Object... arg0) {
            MessagingController controller = (MessagingController) arg0[0];
            Account account = (Account) arg0[1];
            LocalMessage message = (LocalMessage)arg0[2];
            LocalAttachmentBodyPart part = (LocalAttachmentBodyPart)arg0[3];
            Object tag = arg0[4];
            controller.loadAttachmentSynchronous(account, message, part, tag);//TODO check it
			return 0;
        }
    }
    
    private static class DecryptTask extends ExtendedAsyncTask<Object, Integer, Integer> {
        private SingleMessageView mView = null;
        private Context mContext = null;
        private String mMessageUid = null;
        private Account mAccount = null;
        private MessagingController mController = null;
        private MessagingListener mListener = null;
        private String mSubject = null;
        private Exception mLastException = null;
        protected DecryptTask(SingleMessageView view, Activity activity, Context context, Account account, 
                                MessagingController controller, MessagingListener listener, String uid, String subject) {
            super(activity);
            mView = view;
            mContext = context;
            mAccount = account;
            mController = controller;
            mListener = listener;
            mMessageUid = uid;
            mSubject = subject;
        }

        final static private Integer STATUS_SUCCESS = 0;
        final static private Integer STATUS_DOWNLOAD_FAIL = 1;
        final static private Integer STATUS_DECRYPTION_FAIL = 2;
        final static private Integer STATUS_MESSAGE_FORMAT_WRONG = 2;
        
        @Override
        protected void showProgressDialog() {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setTitle("Decrypting message ...");
            mProgressDialog.setMessage("Decrypting in progress ...");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setProgress(0);
            mProgressDialog.setMax(100);
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == STATUS_SUCCESS)
                Toast.makeText(mContext, "Decrypt message \"" + mSubject + "\" Successfully", Toast.LENGTH_LONG).show();
            else if (result == STATUS_DOWNLOAD_FAIL)
                Toast.makeText(mContext, "Download message \"" + mSubject + "\" Failed", Toast.LENGTH_LONG).show();
            else if (result == STATUS_DECRYPTION_FAIL)
                Toast.makeText(mContext, "Decrypt message \"" + mSubject + "\" Failed", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(mContext, "Format of message \"" + mSubject + "\" is wrong", Toast.LENGTH_LONG).show();
            

            removeProgressDialog();
            
            if ( result != STATUS_SUCCESS && com.c0124.utility.DebugPolicy.getInstance().toSendUnexpectedExceptionReport()) {
               
                ACRA.getErrorReporter().handleException(mLastException);
                return;
            }
            
            LocalMessage localMsg = null;
            String text = null;
            try {
                LocalFolder localFolder = (LocalFolder)mAccount.getLocalStore().getFolder(mAccount.getDecryptionFolderName());
                localFolder.open(Folder.OPEN_MODE_RW);      
                localMsg = localFolder.getMessage(mMessageUid);
                FetchProfile fp = new FetchProfile();
                fp.add(FetchProfile.Item.ENVELOPE);
                fp.add(FetchProfile.Item.BODY);
                localFolder.fetch(new Message[] {
                        localMsg
                    }, fp, null);
                localMsg.setFlag(Flag.X_DOWNLOADED_FULL, true);         
                localFolder.close();
                mView.mHasAttachments = localMsg.hasAttachments();   
                text = localMsg.getTextForDisplay();
                
            } catch (MessagingException e) {
                localMsg = null;
                e.printStackTrace();
            } catch( Exception e) {
                localMsg = null;
                Toast.makeText(mContext, "Render message " + mSubject + " fail " + ExceptionUtils.getStackTrace(e), Toast.LENGTH_LONG).show();
                if(com.c0124.utility.DebugPolicy.getInstance().toSendUnexpectedExceptionReport()) {
                    ACRA.getErrorReporter().handleException(e);
                }
            }
            if ( localMsg != null) {
                try {
                    mView.updateViewOverall(mAccount, localMsg, mController, mListener, text);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected Integer doInBackground(Object... arg0) {
            boolean ret = true;
            Account account = (Account) arg0[1];
            LocalMessage message = (LocalMessage)arg0[2];
            LocalAttachmentBodyPart thisPart = (LocalAttachmentBodyPart)arg0[3];
            {
                //part 1: download attachment           
                try {
                    MessagingController controller = (MessagingController) arg0[0];                           
                    Object tag = arg0[4];
                    controller.loadAttachmentSynchronous(account, message, thisPart, tag);
                } catch( Exception e) {
                    ret = false;
                    mLastException = e;
                }
            }
            if (!ret) {
                return STATUS_DOWNLOAD_FAIL;
            }
            publishProgress(30);
            
            //part 2: decrypt                
            Uri uri = AttachmentProvider.getAttachmentUri(account, thisPart.getAttachmentId());
            InputStream str = null;
            try {
                str = mActivity.getBaseContext().getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
                ret = false;               
                e.printStackTrace();   
                mLastException = e;
            } catch( Exception e) {
                ret = false;
                mLastException = e;
            }
            
            if (!ret) {
                return STATUS_DECRYPTION_FAIL;
            }
            
            try {
                InputStream input = null;
                try {
                    input = decryptMessage(account,str, mContext);
                } catch (SCPGPPrivateKeyNotFoundException e1) {
                    
                }

                try {
                    MimeMessage newMsg = new MimeMessage(input);
                    message.setBody(newMsg.getBody());                     
                } catch (IOException e) {
                    ret = false;
                    e.printStackTrace();
                    mLastException = e;
                } catch (MessagingException e) {
                   ret = false;
                    e.printStackTrace();
                    mLastException = e;
                }                
            } catch( Exception e) {
                ret = false;
                e.printStackTrace();
                mLastException = e;
            }
            if ( !ret) {
                return STATUS_DECRYPTION_FAIL;
            }
            publishProgress(80);
            
            //part 3: save to new folder and update view
            try {
                LocalFolder localFolder =
                    (LocalFolder) account.getLocalStore().getFolder(
                        account.getDecryptionFolderName());

                localFolder.open(Folder.OPEN_MODE_RW);
                
                
                long cutoff = System.currentTimeMillis()  - 14L * 24L * 60L * 60L * 1000L; //14 days
                
                localFolder.clearMessagesOlderThan(cutoff);
                localFolder.appendMessages(new Message[] {message});
                
                localFolder.close();
            } catch (MessagingException e) {
                ret = false;
                e.printStackTrace();
                mLastException = e;
            } catch( Exception e) {
                ret = false;
                e.printStackTrace();
                mLastException = e;
            }
            
            if( !ret) {
                return STATUS_MESSAGE_FORMAT_WRONG;
            }
            return STATUS_SUCCESS;
        }
    }
    
    @SuppressLint("NewApi")
	@K9ModificationAnnotation(author = "Jing Teng", description = "Decryption")
    public void setMessage(Account account, LocalMessage message, PgpData pgpData,
            MessagingController controller, MessagingListener listener) throws MessagingException {
             
        String text = null;
        if (MimeUtility.isCryptoType(message.getMimeType()) && message.getBody() instanceof Multipart)
        {
            LocalFolder localFolder = (LocalFolder)account.getLocalStore().getFolder(account.getDecryptionFolderName());
            localFolder.open(Folder.OPEN_MODE_RW);      
            LocalMessage localMsg = localFolder.getMessage(message.getUid());
            if( localMsg == null) {
                Multipart mp = (Multipart) message.getBody();
                LocalAttachmentBodyPart part = (LocalAttachmentBodyPart) mp.getBodyPart(1); //the second part
                DecryptTask decryptTask = new DecryptTask(this, mFragment.getActivity(), this.getContext(), 
                                                account, controller, listener, message.getUid(), message.getSubject());
                mHasAttachments = false; //still working on
                text = null;
                decryptTask.execute(controller, account, message, part, new Object[] { false, this } );                 
            }
            else {
                FetchProfile fp = new FetchProfile();
                fp.add(FetchProfile.Item.ENVELOPE);
                fp.add(FetchProfile.Item.BODY);
                localFolder.fetch(new Message[] {
                        localMsg
                    }, fp, null);
                localMsg.setFlag(Flag.X_DOWNLOADED_FULL, true);         
                localFolder.close();
                message = localMsg; 
                mHasAttachments = message.hasAttachments();   
                text = message.getTextForDisplay();
            }
        } 
        else
        {       
            // Save the text so we can reset the WebView when the user clicks the "Show pictures" button
            text = message.getTextForDisplay();

            if (SCPGPProvider.getInstance().isEncrypted(text) ) {
                do {
                    String newText = SCPGPProvider.getInstance().trimEncrypted(text);
                    ByteArrayInputStream input = new ByteArrayInputStream(newText.getBytes());
                    InputStream input2 = null;
                    try {
                        input2 = decryptMessage(account, input, this.getContext());
                    } catch (SCPGPPrivateKeyNotFoundException e1) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
                        builder.setMessage(R.string.private_key_not_found).setTitle(R.string.decryption_fail);
                        builder.setIcon(R.drawable.ic_notify_encryption_fail);
                        builder.setPositiveButton("Got it!",
                            new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                        AlertDialog dialog = builder.create();        
                        dialog.show();                        
                    }
                    if ( input2 == null) {
                        break;
                    }
                    try {
                        String myString = IOUtils.toString(input2, "UTF-8");

                        newText = myString.replace("\r\n", "<br />");
                        newText = newText.replace("\n", "<br />");
                        if ( newText != null && !newText.isEmpty()) {
                            text = newText;
                            pgpData.setDecryptedData(text); 
                        }
                        // NetEnvelop usually do not use PgpData, but this is a convenient way to pass
                        // decrypted message to (finally) MessageCompose for reply or forward.
                    } catch (IOException e) {
                        e.printStackTrace();
                        if(com.c0124.utility.DebugPolicy.getInstance().toSendUnexpectedExceptionReport()) {
                            ACRA.getErrorReporter().handleException(e);
                        }
                    }
                } while(false);
            }
           
            mHasAttachments = message.hasAttachments();    
        }
        
        updateViewOverall(account, message, controller, listener, text);
    }

    private void updateViewOverall(Account account, LocalMessage message,
        MessagingController controller, MessagingListener listener, String text)
        throws MessagingException {
        mText = text;
        
        resetView();

        if (mHasAttachments) {
            renderAttachments(message, 0, message, account, controller, listener);
        }

        mHiddenAttachments.setVisibility(View.GONE);

        boolean lookForImages = true;
        if (mSavedState != null) {
            if (mSavedState.showPictures) {
                setLoadPictures(true);
                lookForImages = false;
            }

            if (mSavedState.attachmentViewVisible) {
                onShowAttachments();
            } else {
                onShowMessage();
            }

            if (mSavedState.hiddenAttachmentsVisible) {
                onShowHiddenAttachments();
            }

            mSavedState = null;
        } else {
            onShowMessage();
        }

        if (text != null && lookForImages) {
            // If the message contains external pictures and the "Show pictures"
            // button wasn't already pressed, see if the user's preferences has us
            // showing them anyway.
            if (Utility.hasExternalImages(text) && !showPictures()) {
                Address[] from = message.getFrom();
                if ((account.getShowPictures() == Account.ShowPictures.ALWAYS) ||
                        ((account.getShowPictures() == Account.ShowPictures.ONLY_FROM_CONTACTS) &&
                         // Make sure we have at least one from address
                         (from != null && from.length > 0) &&
                         mContacts.isInContacts(from[0].getAddress()))) {
                    setLoadPictures(true);
                } else {
                    showShowPicturesAction(true);
                }
            }
        }

        if (text != null) {
            loadBodyFromText(text);
            //updateCryptoLayout(account.getCryptoProvider(), pgpData, message);
        } else {
            showStatusMessage(getContext().getString(R.string.webview_empty_message));
        }
    }

    public void showStatusMessage(String status) {
        String text = "<div style=\"text-align:center; color: grey;\">" + status + "</div>";
        loadBodyFromText(text);
        mCryptoView.hide();
    }

    private void loadBodyFromText(String emailText) {
        if (mScreenReaderEnabled) {
            mAccessibleMessageContentView.setText(emailText);
        } else {
            mMessageContentView.setText(emailText);
        }

    }

    public void updateCryptoLayout(CryptoProvider cp, PgpData pgpData, Message message) {
        mCryptoView.updateLayout(cp, pgpData, message);
    }

    public void showAttachments(boolean show) {
        mAttachmentsContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        boolean showHidden = (show && mHiddenAttachments.getVisibility() == View.GONE &&
                mHiddenAttachments.getChildCount() > 0);
        mShowHiddenAttachments.setVisibility(showHidden ? View.VISIBLE : View.GONE);
    }

    public void showMessageWebView(boolean show) {
        mMessageContentView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void setAttachmentsEnabled(boolean enabled) {
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
            AttachmentView attachment = (AttachmentView) mAttachments.getChildAt(i);
            attachment.viewButton.setEnabled(enabled);
            attachment.downloadButton.setEnabled(enabled);
        }
    }

    public void removeAllAttachments() {
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
            mAttachments.removeView(mAttachments.getChildAt(i));
        }
    }

    public void renderAttachments(Part part, int depth, Message message, Account account,
                                  MessagingController controller, MessagingListener listener) throws MessagingException {

        if (part.getBody() instanceof Multipart) {
            Multipart mp = (Multipart) part.getBody();
            for (int i = 0; i < mp.getCount(); i++) {
                renderAttachments(mp.getBodyPart(i), depth + 1, message, account, controller, listener);
            }
        } else if (part instanceof LocalStore.LocalAttachmentBodyPart) {
            AttachmentView view = (AttachmentView)mInflater.inflate(R.layout.message_view_attachment, null);
            view.setCallback(attachmentCallback);

            try {
                if (view.populateFromPart(part, message, account, controller, listener)) {
                    addAttachment(view);
                } else {
                    addHiddenAttachment(view);
                }
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Error adding attachment view", e);
            }
        }
    }

    public void addAttachment(View attachmentView) {
        mAttachments.addView(attachmentView);
    }

    public void addHiddenAttachment(View attachmentView) {
        mHiddenAttachments.addView(attachmentView);
    }

    public void zoom(KeyEvent event) {
        if (mScreenReaderEnabled) {
            mAccessibleMessageContentView.zoomIn();
        } else {
            if (event.isShiftPressed()) {
                mMessageContentView.zoomIn();
            } else {
                mMessageContentView.zoomOut();
            }
        }
    }

    public void beginSelectingText() {
        mMessageContentView.emulateShiftHeld();
    }

    public void resetView() {
        mDownloadRemainder.setVisibility(View.GONE);
        setLoadPictures(false);
        showShowAttachmentsAction(false);
        showShowMessageAction(false);
        showShowPicturesAction(false);
        mAttachments.removeAllViews();
        mHiddenAttachments.removeAllViews();

        /*
         * Clear the WebView content
         *
         * For some reason WebView.clearView() doesn't clear the contents when the WebView changes
         * its size because the button to download the complete message was previously shown and
         * is now hidden.
         */
        loadBodyFromText("");
    }

    public void resetHeaderView() {
        mHeaderContainer.setVisibility(View.GONE);
    }

    public AttachmentView.AttachmentFileDownloadCallback getAttachmentCallback() {
        return attachmentCallback;
    }

    public void setAttachmentCallback(
        AttachmentView.AttachmentFileDownloadCallback attachmentCallback) {
        this.attachmentCallback = attachmentCallback;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState savedState = new SavedState(superState);

        savedState.attachmentViewVisible = (mAttachmentsContainer != null &&
                mAttachmentsContainer.getVisibility() == View.VISIBLE);
        savedState.hiddenAttachmentsVisible = (mHiddenAttachments != null &&
                mHiddenAttachments.getVisibility() == View.VISIBLE);
        savedState.showPictures = mShowPictures;

        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mSavedState = savedState;
    }

    @Override
    public void onLayoutChanged() {
        if (mMessageContentView != null) {
            mMessageContentView.invalidate();
        }
    }

    static class SavedState extends BaseSavedState {
        boolean attachmentViewVisible;
        boolean hiddenAttachmentsVisible;
        boolean showPictures;

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };


        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.attachmentViewVisible = (in.readInt() != 0);
            this.hiddenAttachmentsVisible = (in.readInt() != 0);
            this.showPictures = (in.readInt() != 0);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt((this.attachmentViewVisible) ? 1 : 0);
            out.writeInt((this.hiddenAttachmentsVisible) ? 1 : 0);
            out.writeInt((this.showPictures) ? 1 : 0);
        }
    }

    class DownloadImageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String urlString = params[0];
            try {
                boolean externalImage = urlString.startsWith("http");

                String filename = null;
                String mimeType = null;
                InputStream in = null;

                try {
                    if (externalImage) {
                        URL url = new URL(urlString);
                        URLConnection conn = url.openConnection();
                        in = conn.getInputStream();

                        String path = url.getPath();

                        // Try to get the filename from the URL
                        int start = path.lastIndexOf("/");
                        if (start != -1 && start + 1 < path.length()) {
                            filename = URLDecoder.decode(path.substring(start + 1), "UTF-8");
                        } else {
                            // Use a dummy filename if necessary
                            filename = "saved_image";
                        }

                        // Get the MIME type if we couldn't find a file extension
                        if (filename.indexOf('.') == -1) {
                            mimeType = conn.getContentType();
                        }
                    } else {
                        ContentResolver contentResolver = getContext().getContentResolver();
                        Uri uri = Uri.parse(urlString);

                        // Get the filename from AttachmentProvider
                        Cursor cursor = contentResolver.query(uri, ATTACHMENT_PROJECTION, null, null, null);
                        if (cursor != null) {
                            try {
                                if (cursor.moveToNext()) {
                                    filename = cursor.getString(DISPLAY_NAME_INDEX);
                                }
                            } finally {
                                cursor.close();
                            }
                        }

                        // Use a dummy filename if necessary
                        if (filename == null) {
                            filename = "saved_image";
                        }

                        // Get the MIME type if we couldn't find a file extension
                        if (filename.indexOf('.') == -1) {
                            mimeType = contentResolver.getType(uri);
                        }

                        in = contentResolver.openInputStream(uri);
                    }

                    // Do we still need an extension?
                    if (filename.indexOf('.') == -1) {
                        // Use JPEG as fallback
                        String extension = "jpeg";
                        if (mimeType != null) {
                            // Try to find an extension for the given MIME type
                            String ext = MimeUtility.getExtensionByMimeType(mimeType);
                            if (ext != null) {
                                extension = ext;
                            }
                        }
                        filename += "." + extension;
                    }

                    String sanitized = Utility.sanitizeFilename(filename);

                    File directory = new File(K9.getAttachmentDefaultPath());
                    File file = Utility.createUniqueFile(directory, sanitized);
                    FileOutputStream out = new FileOutputStream(file);
                    try {
                        IOUtils.copy(in, out);
                        out.flush();
                    } finally {
                        out.close();
                    }

                    return file.getName();

                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String filename) {
            String text;
            if (filename == null) {
                text = getContext().getString(R.string.image_saving_failed);
            } else {
                text = getContext().getString(R.string.image_saved_as, filename);
            }

            Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();
        }
    }
}
