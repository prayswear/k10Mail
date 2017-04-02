package com.c0124.k9.c0124.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.c0124.k9.c0124.ClientHelper;
import com.c0124.k9.c0124.exception.KeyStoreWrittenFailedException;
import com.c0124.transfer.KeyEntry;
import com.c0124.transfer.KeyEntryList;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.AutoExpandingBufferWriteTransport;
import org.apache.thrift.transport.TMemoryInputTransport;


public class KeyStoreOpenHelper extends SQLiteOpenHelper {

    public static final String Table_Email_Key = "Email2Key";
    public static final String Column_EK_EMail = "Email";
    public static final String Column_EK_Key_Id = "KeyId";

    public static final String Table_Keys = "Keys";
    public static final String Column_K_Id = "_id";
    public static final String Column_K_PrivateKey = "PrivateKey";
    public static final String Column_K_PublicKey = "PublicKey";
    public static final String Column_K_Token = "Token";
    public static final String Column_K_TimeStamp = "TimeStamp";

    public static final String Table_Registration = "Registrations";
    public static final String Column_RG_EMail = "email";
    public static final String Column_RG_RegSig = "signature";
    public static final String Column_RG_RegTimeStamp = "timestamp";
    public static final String Column_RG_RegState = "state"; // created 1, done 2 (got the reg email)

    private static String DATABASE_NAME;
    private static final int DATABASE_VERSION;
    private static final String TAG = "KeyStoreOpenHelper";

    static {
        DATABASE_NAME = "C0124KeyStore";
        DATABASE_VERSION = 22;
    }

    public static void setupToUseTestDb() {
        DATABASE_NAME = "C0124KeyStoreTest";
    }
    
    public KeyStoreOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private void createRegistrationTable(SQLiteDatabase db) {
        final String CMD_REGISTRATION_TABLE_CREATE =
                "CREATE TABLE " + Table_Registration + " ("
                        + Column_RG_EMail + " TEXT, "
                        + Column_RG_RegSig + " TEXT, "
                        + Column_RG_RegTimeStamp + " integer, "
                        + Column_RG_RegState + " integer);";
        db.execSQL(CMD_REGISTRATION_TABLE_CREATE);
        db.execSQL("CREATE INDEX Registrations_emails ON Registrations(email);");
        db.execSQL("CREATE INDEX Registrations_timestamp ON Registrations(timestamp);");
        ClientHelper.i(TAG, "Create " + DATABASE_NAME + " table " + Table_Registration + " succeeded.");
    }

    private void createKeysTable(SQLiteDatabase db)
    {
        final String CMD_KEY_TABLE_CREATE = "CREATE TABLE " + Table_Keys + " ("
                + Column_K_Id + " integer primary key autoincrement, "
                + Column_K_PrivateKey + " TEXT, "
                + Column_K_PublicKey + " TEXT, "
                + Column_K_TimeStamp + " integer, "
                + Column_K_Token + " TEXT );";
        db.execSQL(CMD_KEY_TABLE_CREATE);
        db.execSQL("CREATE INDEX Keys_id ON Keys(_id);");
        db.execSQL("CREATE INDEX Keys_TimeStamp ON Keys(TimeStamp);");
    }

    private void createEmails2KeysTable(SQLiteDatabase db)
    {
        final String CMD_EMAIL_KEY_TABLE_CREATE = "CREATE TABLE " + Table_Email_Key + " ("
                + Column_EK_EMail + " TEXT, "
                + Column_EK_Key_Id + " integer);";
        db.execSQL(CMD_EMAIL_KEY_TABLE_CREATE);
        db.execSQL("CREATE INDEX Email2Key_email ON Email2Key(email);");
        db.execSQL("CREATE INDEX Email2Key_id ON Email2Key(KeyId);");

    }

    private void createIndexes(SQLiteDatabase db)
    {
        db.execSQL("CREATE INDEX Keys_id ON Keys(_id);");
        db.execSQL("CREATE INDEX Keys_TimeStamp ON Keys(TimeStamp);");

        db.execSQL("CREATE INDEX Email2Key_email ON Email2Key(email);");
        db.execSQL("CREATE INDEX Email2Key_id ON Email2Key(KeyId);");

        db.execSQL("CREATE INDEX Registrations_emails ON Registrations(email);");
        db.execSQL("CREATE INDEX Registrations_timestamp ON Registrations(timestamp);");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createKeysTable(db);
        createEmails2KeysTable(db);
        createRegistrationTable(db);

        ClientHelper.i(TAG, "Create " + DATABASE_NAME + " table " + Table_Keys + ", "
                + Table_Email_Key + " succeeded.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 20 && newVersion == 22) {
            // 20->22
            ClientHelper.i(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", creating ");
            createRegistrationTable(db);
            createIndexes(db);
        }
        else if (oldVersion == 21 && newVersion == 22)
        {
            //21->22
            ClientHelper.i(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", creating ");
            createIndexes(db);
        }
        else {
            // 0->22
            ClientHelper.i(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");

            final String CMD_TABLE_DROP_Key = "DROP TABLE IF EXISTS " + Table_Keys;
            final String CMD_TABLE_DROP_Email_Key = "DROP TABLE IF EXISTS " + Table_Email_Key;
            final String CMD_TABLE_DROP_Registration = "DROP TABLE IF EXISTS " + Table_Registration;

            db.execSQL(CMD_TABLE_DROP_Registration);
            db.execSQL(CMD_TABLE_DROP_Key);
            db.execSQL(CMD_TABLE_DROP_Email_Key);
            onCreate(db);
        }
    }

    public void clearDB() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from " + KeyStoreOpenHelper.Table_Keys);
        db.execSQL("delete from " + KeyStoreOpenHelper.Table_Email_Key);
        ClientHelper.i(TAG, "cleared DB");
        db.close();
        this.close();
    }
    
    /**
     * please pass in WritableDatabase.
     * return inserted keyId.
     */
    public long insertKey(SQLiteDatabase db, KeyEntry entry) {
        long insertedId;
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(
                KeyStoreOpenHelper.Column_K_PrivateKey,
                ClientHelper.encodeBytesToString(entry.privateKey.array())); //array might not contains only the pricate key here.
            values.put(
                KeyStoreOpenHelper.Column_K_PublicKey,
                ClientHelper.encodeBytesToString(entry.publicKey.array()));
            values.put(KeyStoreOpenHelper.Column_K_Token, entry.token);
            values.put(KeyStoreOpenHelper.Column_K_TimeStamp, entry.timeStamp);
            insertedId = db.insert(KeyStoreOpenHelper.Table_Keys, null, values);

            values = new ContentValues();
            values.put(KeyStoreOpenHelper.Column_EK_EMail, entry.email.toLowerCase(Locale.getDefault()));
            values.put(KeyStoreOpenHelper.Column_EK_Key_Id, insertedId);
            db.insert(KeyStoreOpenHelper.Table_Email_Key, null, values);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return insertedId;
    }


    /**
     * return all private keys if myEmail is empty or null.
     * */
    public ArrayList<PrivateKeyEntry> getPrivateKeys(SQLiteDatabase db, String myEmail) {

        ArrayList<PrivateKeyEntry> privateKeys = new ArrayList<PrivateKeyEntry>();
        Cursor cursor;

        if (myEmail == null || myEmail.length() == 0) {
            final String sqlStatement =
                    "select Keys.PrivateKey, Email2Key.Email, Keys.TimeStamp from Email2Key,Keys"
                            + " where Email2Key.KeyId = Keys._id ORDER BY Keys.TimeStamp desc";
            cursor = getReadableDatabase().rawQuery(sqlStatement, null);
        } else {
            final String sqlStatementWithEmail =
                    "select Keys.PrivateKey, Email2Key.Email, Keys.TimeStamp from Email2Key,Keys"
                            + " where Email2Key.KeyId = Keys._id and Email2Key.email = ?  ORDER BY Keys.TimeStamp desc";
            cursor =
                    getReadableDatabase().rawQuery(sqlStatementWithEmail,
                            new String[]{myEmail.toLowerCase(Locale.getDefault())});
        }

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                privateKeys.add(new PrivateKeyEntry(
                    ByteBuffer.wrap(ClientHelper.decodeStringToBytes(cursor.getString(0))),
                    cursor.getLong(2)));
                cursor.moveToNext();
            }
        }
        return privateKeys;
    }

    /**
     * return null is myEmail is null or empty.
     * */
    public PublicKeyEntry getCurrentPublicKey(SQLiteDatabase db, String myEmail) {
        Cursor cursor;
        if (myEmail == null || myEmail.length() == 0) {
            return null;
        } else {
            final String sqlStatementWithEmail =
                    "select Keys.PublicKey, Keys.Token, Email2Key.Email, Keys.TimeStamp from Email2Key,Keys"
                            + " where Email2Key.KeyId = Keys._id and Email2Key.email = ?  ORDER BY Keys.TimeStamp desc";
            cursor =
                    getReadableDatabase().rawQuery(sqlStatementWithEmail,
                            new String[]{myEmail.toLowerCase(Locale.getDefault())});
        }

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                return new PublicKeyEntry(
                    ByteBuffer.wrap(ClientHelper.decodeStringToBytes(cursor.getString(0))),
                    cursor.getString(1), cursor.getLong(3));
            }
        }
        return null;
    }

    // Stored email should be lower case.
    public void addRegistrationEntry(SQLiteDatabase db, RegistrationEntry entry)
            throws KeyStoreWrittenFailedException {
        long insertedId;
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KeyStoreOpenHelper.Column_RG_EMail, entry.myEmail.toLowerCase(Locale.getDefault()));
            values.put(KeyStoreOpenHelper.Column_RG_RegSig, entry.signature);
            values.put(KeyStoreOpenHelper.Column_RG_RegTimeStamp, entry.timeStamp);
            values.put(KeyStoreOpenHelper.Column_RG_RegState, entry.state);
            insertedId = db.insert(KeyStoreOpenHelper.Table_Registration, null, values);
            if (insertedId == -1) {
                throw new KeyStoreWrittenFailedException("failed add registration entry, email:"
                        + entry.myEmail + "timestap:" + entry.timeStamp);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public RegistrationEntry findRegistrationEntry(SQLiteDatabase db, String email, long timestamp) {
        Cursor cursor;
        if (email == null || email.length() == 0 || timestamp <= 0) {
            return null;
        } else {
            final String sqlStatementToFindRegistationEntry =
                    "select signature, state from Registrations"
                            + " where timestamp = ? and email = ?";
            cursor =
                    getReadableDatabase().rawQuery(sqlStatementToFindRegistationEntry,
                            new String[]{Long.toString(timestamp), email.toLowerCase(Locale.getDefault())});
        }

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                return new RegistrationEntry(email, cursor.getString(0), timestamp,
                        cursor.getLong(1));
            }
        }
        return null;
    }

    /**
     * Delete the registration entry in registration table.
     *
     * @param db,        writable db
     * @param email
     * @param timestamp, the timestamp of the reg entry
     */
    public void setRegistrationEntryToDone(SQLiteDatabase db, String email, long timestamp) {
        if (email == null || email.length() == 0 || timestamp <= 0) {
            throw new IllegalArgumentException();
        } else {
            final String sqlStatementFinishAnRegistationEntry =
                    "DELETE from Registrations" + " where timestamp = ? and email = ?";
            db.execSQL(sqlStatementFinishAnRegistationEntry,
                    new String[]{Long.toString(timestamp), email.toLowerCase(Locale.getDefault())});
        }

        return;
    }

    public void clearRegistrationEntriesForAnEmail(SQLiteDatabase db, String email) {
        if (email == null || email.length() == 0) {
            throw new IllegalArgumentException();
        } else {
            final String sqlStatementFinishAnRegistrationEntry =
                    "DELETE from Registrations" + " where email = ?";
            db.execSQL(sqlStatementFinishAnRegistrationEntry,
                    new String[]{email.toLowerCase(Locale.getDefault())});
        }

        return;
    }
    
    protected Set<String> getAllEmailsInRegistration() {
        Cursor cursor;
        HashSet<String> result = new HashSet<String>();
        final String findAllEmailInRegistration =
                "SELECT DISTINCT Registrations.email" +
                        " from Registrations";
        cursor = getReadableDatabase().rawQuery(findAllEmailInRegistration, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                result.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        return result;
    }

    protected HashMap<String, AccountsKeyEntry> getAllEmailsCurrentKeys() {
        Cursor cursor;
        HashMap<String, AccountsKeyEntry> emails2TheirKeys = new HashMap<String, AccountsKeyEntry>();

        final String findAllEmails =
                "SELECT DISTINCT Email2Key.email" +
                        " from Email2Key";
        cursor = getReadableDatabase().rawQuery(findAllEmails, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                emails2TheirKeys.put(cursor.getString(0), null);
            } while (cursor.moveToNext());
        }

        // TODO: switch to find key for every email after adding index for Keys table.
        // When there are many keys in the keys table, the current operation will be more expensive.
        final String findAllEmailsKeys =
                "SELECT DISTINCT Email2Key.email, Keys.Token, Keys.TimeStamp" +
                        " from Email2Key, Keys" +
                        " where Email2Key.KeyId = Keys._id" +
                        " order by Keys.TimeStamp desc";
        cursor = getReadableDatabase().rawQuery(findAllEmailsKeys, null);

        int foundCount = 0;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                if (!emails2TheirKeys.containsKey(cursor.getString(0))) {
                    ClientHelper.v(TAG, "email:" + cursor.getString(0) + ", not found in Email2Key table");
                }
                if (emails2TheirKeys.get(cursor.getString(0)) == null) {
                    emails2TheirKeys.put(cursor.getString(0),
                            new AccountsKeyEntry(
                                    cursor.getString(1),
                                    cursor.getLong(2),
                                    cursor.getString(0),
                                    false,
                                    true,
                                    true));
                    ++foundCount;
                    if (foundCount == emails2TheirKeys.size())
                        break;
                } else {
                    ClientHelper.v(TAG, "email:" + cursor.getString(0) + ", already have key:" + cursor.getLong(2));
                }
            } while (cursor.moveToNext());
        }

        return emails2TheirKeys;
    }

    public AbstractList<AccountsKeyEntry> getAllAccountsKeys(AbstractList<String> emailNames) {
        Set<String> emailsInRegistration = getAllEmailsInRegistration();
        HashMap<String, AccountsKeyEntry> emailsCurrentKey = getAllEmailsCurrentKeys();
        Set<String> emailNamesSet = new TreeSet<String>();
        ArrayList<AccountsKeyEntry> result = new ArrayList<AccountsKeyEntry>();

        // for every email in emailNames
        for (String email : emailNames) {
            ClientHelper.v(TAG, "setup email:" + email);
            AccountsKeyEntry entry = emailsCurrentKey.get(email);
            if (entry == null) {
                entry = new AccountsKeyEntry("", 0, email, false, true, false);
            }
            if (emailsInRegistration.contains(email)) {
                entry.isRegistrationPending = true;
            }
            result.add(entry);
            emailNamesSet.add(email);
        }

        // For email not in emailNames, but still have keys in emailsCurrentKey.
        for (Map.Entry<String, AccountsKeyEntry> entry : emailsCurrentKey.entrySet()) {
            String email = entry.getKey();
            AccountsKeyEntry accountKey = entry.getValue();

            if (!emailNamesSet.contains(email)) {
                ClientHelper.v(TAG, "adding not setup email:" + email);
                accountKey.isEmailHaveAccountSetup = false;
                accountKey.isRegistrationPending = false;
                result.add(accountKey);
            }
        }

        return result;
    }

    public AbstractList<KeyEntry> GetAllKeys() {
        ArrayList<KeyEntry> keys = new ArrayList<KeyEntry>();
        try {
            Cursor cursor;
            {
                final String sqlStatement =
                    "select Keys.PrivateKey, Keys.PublicKey, Keys.Token, Keys.TimeStamp, Email2Key.Email from Email2Key,Keys"
                        + " where Email2Key.KeyId = Keys._id ORDER BY Keys.TimeStamp desc";
                cursor = getReadableDatabase().rawQuery(sqlStatement, null);
            }

            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    keys.add(new KeyEntry(
                        ByteBuffer.wrap(ClientHelper.decodeStringToBytes(cursor.getString(0))),
                        ByteBuffer.wrap(ClientHelper.decodeStringToBytes(cursor.getString(1))),
                        cursor.getString(2),
                        cursor.getLong(3),
                        cursor.getString(4)));
                    cursor.moveToNext();
                }
            }

        } catch (Exception ex) {
            return null;
        }

        return keys;
    }

    public boolean isKeyAlreadyInDb(KeyEntry p_entry) {
        Cursor cursor;
        {
            final String sqlStatementWithEmail =
                    "select Keys.PublicKey, Keys.PrivateKey, Keys.Token, Email2Key.Email, Keys.TimeStamp from Email2Key,Keys"
                            + " where Email2Key.KeyId = Keys._id and Email2Key.email = ? and Keys.TimeStamp = ? ";
            cursor =
                    getReadableDatabase().rawQuery(sqlStatementWithEmail,
                            new String[]{p_entry.getEmail().toLowerCase(Locale.getDefault()), Long.toString(p_entry.getTimeStamp())});
        }

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                if (cursor.getString(2).compareTo(p_entry.getToken()) == 0
                        && java.util.Arrays.equals(p_entry.getPublicKey(), ClientHelper.decodeStringToBytes(cursor.getString(0)))
                        && java.util.Arrays.equals(p_entry.getPrivateKey(), ClientHelper.decodeStringToBytes(cursor.getString(1)))
                        ) {
                    ClientHelper.i(ClientHelper.LogTag, "find key with same timestamp:" + p_entry.getTimeStamp() + ", token:" + p_entry.getToken());
                    return true;
                }
            }
        }

        return false;
    }

    public int importKeys(SQLiteDatabase p_db, List<KeyEntry> p_keys)
    {
        int importedKeysCount = 0;
        for(KeyEntry key : p_keys)
        {
            if (isKeyAlreadyInDb(key))
            {
                ClientHelper.i(ClientHelper.LogTag, "skip import for key, email:" + key.getEmail() + ", token:" + key.getToken() + ", timestamp:"+ key.timeStamp);
            }
            else
            {
                insertKey(p_db, key);
                ++importedKeysCount;
            }
        }

        return importedKeysCount;
    }

    /**
     * This method will serialize a list of KeyEntry into a buffer via <B>Json Protocol</>.
     * @param keyList the list of KeyEntry.
     * @return
     * @throws TException
     * @throws UnsupportedEncodingException
     */
    public final static ByteBuffer getSerializedBytesFromKeys(final List<KeyEntry> keyList)
            throws TException, UnsupportedEncodingException {

        KeyEntryList keys = new KeyEntryList(keyList);
        AutoExpandingBufferWriteTransport transportOut;

        {
            final int s_SerializedBufferInitialBuffer = 1024;
            final double s_SerializedGrowthCoefficient = 1.5;
            transportOut =
                    new AutoExpandingBufferWriteTransport(s_SerializedBufferInitialBuffer,
                            s_SerializedGrowthCoefficient);

            TJSONProtocol json = new TJSONProtocol(transportOut);
            keys.write(json);
        }

        return ByteBuffer.wrap(transportOut.getBuf().array(), 0, transportOut.getPos());
    }

    /**
     * This method will de-serialize a <B>json</> buffer into a list of keys.
     * @param buffer
     * @return
     * @throws TException
     */
    public final static List<KeyEntry> getDeSerializedKeysFromBytes(ByteBuffer buffer)
            throws TException {

        KeyEntryList keyList = new KeyEntryList();
        TMemoryInputTransport transportIn =
                new TMemoryInputTransport(buffer.array(), buffer.arrayOffset(), buffer.remaining());
        TJSONProtocol json = new TJSONProtocol(transportIn);
        keyList.read(json);

        return keyList.keys;
    }


}
