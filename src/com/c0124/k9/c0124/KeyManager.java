package com.c0124.k9.c0124;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.c0124.GetPublicKeyRequest;
import com.c0124.GetPublicKeyResult;
import com.c0124.PublicKey;
import com.c0124.PublishPublicKeyRequest;
import com.c0124.PublishPublicKeyResult;
import com.c0124.PublishResultEnum;
import com.c0124.RegisteRequest;
import com.c0124.RegisteResult;
import com.c0124.RegisteResultCodeEnum;
import com.c0124.k9.Account;
import com.c0124.k9.K9;
import com.c0124.k9.Preferences;
import com.c0124.k9.R;
import com.c0124.k9.c0124.ThriftClient.ClientStub;
import com.c0124.k9.c0124.data.KeyStoreOpenHelper;
import com.c0124.k9.c0124.data.PrivateKeyEntry;
import com.c0124.k9.c0124.data.PublicKeyEntry;
import com.c0124.k9.c0124.data.RegistrationEntry;
import com.c0124.k9.c0124.exception.GetPublicKeyFailedException;
import com.c0124.k9.c0124.exception.KeyStoreWrittenFailedException;
import com.c0124.k9.c0124.exception.NoConnectionException;
import com.c0124.k9.c0124.exception.PublicKeyNotFoundInRespositoryException;
import com.c0124.k9.c0124.exception.PublishPublicKeyFailedException;
import com.c0124.k9.c0124.exception.RegisteFailedException;
import com.c0124.k9.c0124.exception.TokenNotFound;
import com.c0124.k9.controller.MessagingController;
import com.c0124.transfer.KeyEntry;
import com.c0124.utility.C0124Helper;
import com.c0124.utility.DebugPolicy;
import com.c0124.utility.security.RandomGenerator;

import org.apache.thrift.TException;
import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyPair;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.operator.PGPDigestCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class KeyManager{

    private static class LazyHolder {
        private static final KeyManager INSTANCE = new KeyManager();
    }

    public static KeyManager getInstance(Context context) {
        KeyManager km = LazyHolder.INSTANCE;
        
        if (!km.isInitialized.get()) {
            String url;
            if (DebugPolicy.isUsingIntegrationService()) {
                url = context.getResources().getString(R.string.int_serving_url);
            } else {
                url = context.getResources().getString(R.string.main_serving_url);
            }
            ClientHelper.i(TAG, "going to set url to " + url + ", isInitialized:" + km.isInitialized);
            C0124Helper.setServiceUrl(url);
            km.init(context);
            ClientHelper.i(TAG, "isInitialized: " + km.isInitialized);
        }
        return km;
    }

    private KeyManager() {
        isInitialized = new AtomicBoolean(false);
    }

    private static final String TAG = "KeyManager";
    private static final int bitLength = 2048;
    private Context appContent;
    private AtomicBoolean isInitialized;

    /**
     * Steps in this method:
     * <li> Create private/public key pair. 
     * <li> upload public key to cloud, got token from cloud. 
     * <li> save private/public key and token to sqlite.
     * 
     * @param context
     * @param myEmail
     * @return If success, return true. If failed, return false.
     * @throws NoConnectionException
     * @throws TException
     * @throws RegisteFailedException
     * @throws KeyStoreWrittenFailedException
     */
    public synchronized void startCreateNewPair(Context context, String myEmail) throws TException,
        RegisteFailedException, KeyStoreWrittenFailedException, NoConnectionException {
        
        if (!ClientHelper.isConnected(context)) {
            throw new NoConnectionException("", C0124Helper.getUtcTimeStamp());
        }
        
        ClientStub stub = null;
        KeyStoreOpenHelper helper = null;
        SQLiteDatabase db = null;
        
        try {
            stub = ThriftClient.getInstance().createClientStub();
            RegisteRequest RRequest = new RegisteRequest();
            RRequest.emailAddress = myEmail.toLowerCase(Locale.getDefault());
            RRequest.registerTimestamp = C0124Helper.getUtcTimeStamp();
            RRequest.registerRandomSig = rang.GetRandomString(8);
            // TODO: Ideally we want to use languageTag when client API supports API 21+.
            // But forLanguageTag is added in API 21+. Refer
            // http://developer.android.com/reference/java/util/Locale.html#forLanguageTag(java.lang.String)
            RRequest.languageTag = Locale.getDefault().getLanguage();

            RegisteResult RResult;
            RResult = stub.client.Registe(RRequest);

            if (RResult.result != RegisteResultCodeEnum.Success) {
                ClientHelper.e(TAG, "Register email failed, result enum:" + RResult.result);
                throw new RegisteFailedException("email:" + myEmail, RResult.result);
            }

            helper = new KeyStoreOpenHelper(context.getApplicationContext());
            db = helper.getWritableDatabase();

            helper.addRegistrationEntry(db, new RegistrationEntry(myEmail,
                RRequest.registerRandomSig, RRequest.registerTimestamp,
                RegistrationEntry.CreatedState));

            // Check mail for email
            checkEmail(context, myEmail);

        } finally {
            stub.close();
            db.close();
            helper.close();
        }

        return;
    }

    public synchronized void createAndUploadKeyPair(Context context, String myEmail,
                                                    String password, String regSig, String verifyCode)
            throws NoConnectionException,
            TException, RegisteFailedException, PublishPublicKeyFailedException,
            KeyStoreWrittenFailedException, InvalidKeyException, NoSuchProviderException,
            SignatureException, IOException, PGPException, NoSuchAlgorithmException {
        
        final boolean armor = true;
        if (!ClientHelper.isConnected(context)) {
            throw new NoConnectionException("", C0124Helper.getUtcTimeStamp());
        }
        
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

        ByteArrayOutputStream privateOut = new ByteArrayOutputStream();
        ByteArrayOutputStream publicOut = new ByteArrayOutputStream();

        // http://stackoverflow.com/questions/7560974/what-crypto-algroithms-does-android-support
        // https://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(bitLength);
        KeyPair kp = kpg.generateKeyPair();
        /*
         * the format is: from the Real Name, Comment and Email Address in this form:
         * "Heinrich Heine (Der Dichter) <heinrichh@duesseldorf.de>" 
         *  Since we do not know what user will put in myIdentity, did not include it into the key itself. 
         *  
         *  Reference:
         *  http://www.seas.upenn.edu/cets/answers/pgp_keys.html 
         *  help from "gpg --gen-key"
         */
        long timeStamp = C0124Helper.getUtcTimeStamp();
        
        String keyId = "(" + timeStamp + ") <" + myEmail + ">";
        createKeyPair(context, privateOut, publicOut, kp, keyId, password.toCharArray(),
            armor, myEmail);

        String token =
            UploadPublicKey(ByteBuffer.wrap(publicOut.toByteArray()), myEmail, regSig, verifyCode,
                timeStamp);
        if (token == null || token.length()==0) {
            throw new PublishPublicKeyFailedException("token not right, email"+ myEmail, PublishResultEnum.Failed);
        }

            KeyStoreOpenHelper helper = new KeyStoreOpenHelper(context.getApplicationContext());
            SQLiteDatabase db = helper.getWritableDatabase();
            long id = helper.insertKey(db, new KeyEntry(
                ByteBuffer.wrap(privateOut.toByteArray()), 
                ByteBuffer.wrap(publicOut.toByteArray()),
                token, 
                timeStamp, 
                myEmail));
            db.close();
            helper.close();
            if (id == -1)
            {
                throw new KeyStoreWrittenFailedException("failed email:"+myEmail);
            }
    }
    
    /**
     * This method will go remote to get the public keys.
     * The result will include the senderEmail's public key. 
     * 
     * @param context
     * @param senderEmail
     * @param recipients not case sensitive
     * @return If no exception, the returned map will contain all recipients and senderEmail.
     * @throws TException
     * @throws TokenNotFound
     * @throws GetPublicKeyFailedException 
     * @throws PublicKeyNotFoundInRespositoryException 
     */
    public Map<String, PublicKey> getPublicKeys(Context context, String senderEmail,
        Set<String> recipients) throws NoConnectionException, TokenNotFound,
        GetPublicKeyFailedException, PublicKeyNotFoundInRespositoryException, TException 
    {        
        if (!ClientHelper.isConnected(context)) {
            throw new NoConnectionException("", C0124Helper.getUtcTimeStamp());
        }
        PublicKeyEntry pubKeyEntry;
        {
            KeyStoreOpenHelper helper = new KeyStoreOpenHelper(context.getApplicationContext());
            SQLiteDatabase db = helper.getReadableDatabase();
            pubKeyEntry = helper.getCurrentPublicKey(db, senderEmail);
            db.close();
            helper.close();
        }

        if (pubKeyEntry == null) {
            ClientHelper.w(TAG, "Can not find token for:" + senderEmail);
            throw new TokenNotFound("", senderEmail); // should not happen, if no token is found,
        }

        Set<String> requestEmails = new HashSet<String>();
        for (String email : recipients) {
            requestEmails.add(email.toLowerCase(Locale.getDefault()));
        }
        // TODO: sender email should be dealing differently.
        requestEmails.add(senderEmail.toLowerCase(Locale.getDefault()));

        Map<String, PublicKey> res = new HashMap<String, PublicKey>();

        ThriftClient.ClientStub stub = ThriftClient.getInstance().createClientStub();
        try {
            GetPublicKeyRequest request =
                new GetPublicKeyRequest(senderEmail, pubKeyEntry.token, requestEmails);
            
            GetPublicKeyResult   result = stub.client.GetPublicKey(request);
            
            if (result.result != com.c0124.GetPublicKeyResultEnum.Success) {
                ClientHelper.w(TAG, "GetPublicKey failed with:" + result.result);
                throw new GetPublicKeyFailedException("GetPublicKey failed", result.result);
            }

            TreeSet<String> missingPubKeyEmails = new TreeSet<String>();
            for (String email : recipients) {
                com.c0124.PublicKey key =
                    result.email2PublickeyMap.get(email.toLowerCase(Locale.getDefault()));
                if (key == null) {
                    ClientHelper.w(TAG, "cannot find public key for:" + email);
                    missingPubKeyEmails.add(email);
                }
                res.put(email, key);
            }
            if (!missingPubKeyEmails.isEmpty()) {
                ClientHelper.w(TAG, "emails missing pub key:" + missingPubKeyEmails.toString());
                throw new PublicKeyNotFoundInRespositoryException("missing pub key:"
                    + missingPubKeyEmails.toString(), missingPubKeyEmails);
            }
            res.put(senderEmail,
                result.email2PublickeyMap.get(senderEmail.toLowerCase(Locale.getDefault())));
        } finally {
            stub.close();
        }
        
        return res;
    }

    /**
     * @param email It is not case sensitive. It should not be null or empty.
     * @return whether the email have its key pair generated and stored in DB.
     */
    public boolean isGoodForSendingPGPEmail(String email) {
        if (email == null || email.length() == 0)
            return false;
        
        PublicKeyEntry pubKeyEntry;
        KeyStoreOpenHelper helper = new KeyStoreOpenHelper(this.appContent);
        SQLiteDatabase db = helper.getReadableDatabase();
        pubKeyEntry = helper.getCurrentPublicKey(db, email);
        db.close();
        helper.close();

        return (pubKeyEntry!=null);
    }

    public ArrayList<ByteArrayInputStream> getPrivateKeys() {
        return getPrivateKeys(null);
      }

    /**
     * @param email It is not case sensitive.
     * @return The private keys that are belonging to this email if the email is provided. This
     *         method will return all private keys for any email if the email parameter is null
     *         or empty.
     */
    public ArrayList<ByteArrayInputStream> getPrivateKeys(String email)
    {
        ArrayList<ByteArrayInputStream> res = new ArrayList<ByteArrayInputStream>();
        ArrayList<PrivateKeyEntry> priKeyEntries;
        {
            KeyStoreOpenHelper helper = new KeyStoreOpenHelper(this.appContent);
            SQLiteDatabase db = helper.getWritableDatabase();
            priKeyEntries = helper.getPrivateKeys(db, email);
            db.close();
            helper.close();
        }

        for(PrivateKeyEntry entry : priKeyEntries)
        {
            ClientHelper.i(TAG, "private key timestap:"+entry.timeStamp);
            res.add(new ByteArrayInputStream(entry.priKey.array()));
        }
        ClientHelper.i(TAG, "loaded pkey number:"+res.size());
        
        return res;
    }
    
    private synchronized void init(Context context) {
        MessagingController.getInstance(K9.app).addListener(new RegistrationEmailListener());
        appContent = context.getApplicationContext();
        this.isInitialized.set(true);
    }

    // TODO: stop so frequent checking after all key is ok.
    private class CheckMailTask implements Runnable {
        private String emailAddress;
        private Context context;

        public CheckMailTask(Context context, String emailAddress) {
            this.emailAddress = emailAddress;
            this.context = context;
        }

        @Override
        public void run() {
            Preferences prefs = Preferences.getPreferences(appContent.getApplicationContext());
            Collection<Account> accounts = prefs.getAvailableAccounts();

            Account theAccount = null;
            for (Account act : accounts) {
                if (act.getEmail().compareToIgnoreCase(emailAddress) == 0) {
                    theAccount = act;
                }
            }
            if (theAccount != null) {
                ClientHelper.i(TAG, "checking email for " + emailAddress);
                MessagingController.getInstance(K9.app).checkMail(context, theAccount, true, true,
                    null);
            } else {
                ClientHelper.w(TAG, "cannot checking email for " + emailAddress);
            }
        }
    }

    private synchronized void checkEmail(Context context, String emailAddress) {
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(5);
        scheduledThreadPool
            .schedule(new CheckMailTask(context, emailAddress), 10, TimeUnit.SECONDS);
    }

    private void createKeyPair(
            Context context,
            OutputStream secretOut,
            OutputStream publicOut,
            KeyPair pair,
            String identity,
            char[] passPhrase,
            boolean armor,
            String myEmail)
            throws IOException, InvalidKeyException, NoSuchProviderException, SignatureException,
            PGPException {
        if (armor) {
            secretOut = new ArmoredOutputStream(secretOut);
        }

        PGPDigestCalculator sha1Calc =
                new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);

        PGPKeyPair keyPair = new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, pair, new Date());
        PGPSecretKey secretKey =
                new PGPSecretKey(PGPSignature.DEFAULT_CERTIFICATION, keyPair, identity, sha1Calc, null,
                        null, new JcaPGPContentSignerBuilder(keyPair.getPublicKey().getAlgorithm(),
                        HashAlgorithmTags.SHA1), new JcePBESecretKeyEncryptorBuilder(
                        PGPEncryptedData.AES_128, sha1Calc).setProvider("BC").build(passPhrase));

        secretKey.encode(secretOut);
        secretOut.close();

        PGPPublicKey key = secretKey.getPublicKey();

        if (armor) {
            publicOut = new ArmoredOutputStream(publicOut);
        }
        key.encode(publicOut);
        publicOut.close();
    }
    
    private static RandomGenerator rang = new RandomGenerator(true);
    private String UploadPublicKey(ByteBuffer p_pubKey, String myEmail, String regSig,
        String verifyCode, long timeStamp) throws TException, RegisteFailedException,
        PublishPublicKeyFailedException {

        ClientStub stub = null;
        String result = null;
        
        stub = ThriftClient.getInstance().createClientStub();

        try {
            PublishPublicKeyRequest PRequest = new PublishPublicKeyRequest();
            PRequest.myEmailAddress = myEmail;
            PRequest.createTimestamp = timeStamp;
            PRequest.registerRandomSig = regSig;
            PRequest.verifyCode = verifyCode;
            PRequest.publicKey = p_pubKey;

            PublishPublicKeyResult PResult = stub.client.PublishPublicKey(PRequest);
            if (PResult.result != PublishResultEnum.Success) {
                ClientHelper.e(TAG, "Publish public public key failed, result:"+ PResult.result );
                throw new PublishPublicKeyFailedException("email:"+myEmail, PResult.result);
            } else {
                ClientHelper.i(TAG, "Publish public key for " + myEmail + " succeeded, token is: "
                    + PResult.accessToken);
                result = PResult.accessToken;
            }
        } finally {
            stub.close();
        }

        return result;
    }
}

