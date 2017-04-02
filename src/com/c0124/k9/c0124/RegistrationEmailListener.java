package com.c0124.k9.c0124;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.c0124.k9.Account;
import com.c0124.k9.K9;
import com.c0124.k9.R;
import com.c0124.k9.c0124.android.KeyGeneratedReceiver;
import com.c0124.k9.c0124.data.KeyStoreOpenHelper;
import com.c0124.k9.c0124.data.PublicKeyEntry;
import com.c0124.k9.c0124.data.RegistrationEntry;
import com.c0124.k9.c0124.gui.accounts_keys.activity.AccountsKeys;
import com.c0124.k9.controller.MessagingListener;
import com.c0124.k9.mail.Address;
import com.c0124.k9.mail.Message;
import com.c0124.k9.mail.MessagingException;
import com.c0124.utility.StaticUtilityMethods;
import com.c0124.utility.verifyemail.VerifyEmail;

public class RegistrationEmailListener extends MessagingListener {

    static public final String LogTag;

    static {
        LogTag = "RegistrationEmailListener";
    }

    private boolean isThisMessageForTheAccount(Account account, Message message) throws MessagingException
    {
        boolean isToMe = false;
        Address[] toEmails = message.getRecipients(Message.RecipientType.TO);
        for(Address addr : toEmails)
        {
            if (addr.getAddress().compareToIgnoreCase(account.getEmail()) == 0)
            {
                isToMe = true;
                break;
            }
        }

        return isToMe;
    }
    
    public void synchronizeMailboxAddOrUpdateMessage(Account account, String folder, Message message) {

        KeyStoreOpenHelper helper = null;
        SQLiteDatabase db = null;
        
        VerifyEmail verifyEmail = new VerifyEmail();
        
        try {
            // Send from netenvelop, and subject is right.
            if (message.getFrom().length == 1
                && message.getFrom()[0].getAddress().compareToIgnoreCase(
                    verifyEmail.getSenderEmailAddress()) == 0) {
             
                if (!isThisMessageForTheAccount(account, message))
                {
                    // We might allow this, since user might forward his email while no old copy.
                    ClientHelper.i(LogTag,
                        "email not for this account:"
                            + account.getEmail()
                            + ", msg receiver:"
                            + (message.getRecipients(Message.RecipientType.TO).length > 1 ? "none"
                                : message.getRecipients(Message.RecipientType.TO)[0]));
                    return;
                }
                
                if (message.getPreview()==null || message.getPreview().length()==0)
                {
                    ClientHelper.i(LogTag, "empty email, but got subject+"
                        + message.getSubject());
                    return;
                }
                
                // TODO: be clear how long the email content will get into preview.
                long timestamp =
                    verifyEmail.getTimeStampFromMailText(
                        message.getPreview().toString());
                String verifyCode =
                    verifyEmail.getVerifyCodeFromMailText(
                        message.getPreview().toString());

                ClientHelper.i(LogTag, "verify code:" + verifyCode + ", timestamp:" + timestamp);

                Context context = K9.app.getApplicationContext();
                helper = new KeyStoreOpenHelper(context);
                db = helper.getWritableDatabase();

                RegistrationEntry entry =
                    helper.findRegistrationEntry(db, account.getEmail(), timestamp);
                if (entry != null) {
                    try {
                        KeyManager.getInstance(context)
                            .createAndUploadKeyPair(context, account.getEmail(),
                                SCPGPProvider.password, entry.signature, verifyCode);
                    } catch (Exception e) {
                        ClientHelper.i(
                            LogTag,
                            "cannot create and upload public key for" + account.getEmail()
                                    + ", vcode:" + verifyCode
                                    + ", timestamp:" + timestamp
                                    + ", rsig:" + entry.signature
                                    + ", e:" + e.getMessage()
                                    + ", et:" + StaticUtilityMethods.getExceptionStackTrace(e));
                        notifyMessage(account, message, false);
                        return;
                    }
                    // Successfully uploaded a new public key.
                    helper.clearRegistrationEntriesForAnEmail(db, account.getEmail());
                    notifyMessage(account, message, true);

                    if (ClientHelper.isAppActivelyRunning())
                    {
                        Intent intent = new Intent();
                        intent.setAction(AccountsKeys.keyUpdatedIntentActionTag);
                        K9.app.getApplicationContext().sendBroadcast(intent);
                    }
                } else {
                    ClientHelper.i(LogTag, "no such registration going on");
                }
            } else {
                ClientHelper.i(LogTag, "got subject+" + message.getSubject());
            }

        } catch (Exception e) {
            ClientHelper.i(
                LogTag,
                "cannot process new message" + e.getMessage()
                    + StaticUtilityMethods.getExceptionStackTrace(e));
            notifyMessage(account, message, false);
        } finally {
            if (helper != null)
                helper.close();
            if (db != null)
                db.close();
        }
    }

    private void sendToastMessage(String message) {
        if (ClientHelper.isAppActivelyRunning()) {
            Intent intent = new Intent();
            intent.putExtra(KeyGeneratedReceiver.intentMessageDataTag, message);
            intent.setAction(KeyGeneratedReceiver.actionTag);
            K9.app.getApplicationContext().sendBroadcast(intent);
        }
    }

    // http://developer.android.com/reference/android/os/Looper.html
    // http://www.oschina.net/question/163910_31439
    // http://prasanta-paul.blogspot.com/2013/09/android-looper-and-toast-from.html
    // TODO: notify message in R.
    private void notifyMessage(Account account, Message message , boolean isSuccess) {
        Context context = K9.app.getApplicationContext();

        TaskStackBuilder stack;
        {
            stack = TaskStackBuilder.create(context);
            Intent intent = new Intent(context, AccountsKeys.class);
            stack.addNextIntent(intent);
        }
        
        PublicKeyEntry pubKeyEntry = null;
        {
            KeyStoreOpenHelper helper = new KeyStoreOpenHelper(context.getApplicationContext());
            SQLiteDatabase db = helper.getReadableDatabase();
            pubKeyEntry = helper.getCurrentPublicKey(db, account.getEmail());
            db.close();
            helper.close();
        }
        if (pubKeyEntry==null)
        {
            isSuccess = false;
        }
        
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(context);
        String notifyContentText;
        final String notifyTitle = context.getString(R.string.c0124_string_registrationEmailListener_text_verified) + account.getEmail();;
        if (isSuccess)
        {   
            notifyContentText = "Setup is finished successfully for: " + account.getEmail();
            inboxStyle.setBigContentTitle(notifyTitle);
            inboxStyle.addLine(notifyContentText);
            inboxStyle.addLine("This email have a token:" + pubKeyEntry.token + ".");
        }
        else
        {
            notifyContentText =
                "Verification is done, but failed in uploading public key for: "
                    + account.getEmail();

            inboxStyle.setBigContentTitle(notifyTitle);
            inboxStyle.addLine(notifyContentText);
            inboxStyle
                .addLine("Create keys for"
                    + account.getEmail() + " again or create all keys again.");
        }
        mBuilder.setContentTitle(notifyTitle).setContentText(notifyContentText);
        

        mBuilder.setSmallIcon(R.drawable.icon)
                .setContentTitle("Verified: " + account.getEmail())
                .setContentText(notifyContentText)
                .setContentIntent(
                    stack.getPendingIntent(account.getAccountNumber(),
                        Notification.FLAG_AUTO_CANCEL))
                .setStyle(inboxStyle)
                .setAutoCancel(true)
                ;

        NotificationManager notificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


        sendToastMessage(notifyContentText);
        notificationManager.notify("RegistrationEmailListener", // notification tag
                                   account.getAccountNumber(),  // notification id, if an account be verified several times, only one notification will appears.
                                   mBuilder.build());
        
        ClientHelper.i(
            LogTag,
            "sent notitications");
    }
}
    