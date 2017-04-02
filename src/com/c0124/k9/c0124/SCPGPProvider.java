package com.c0124.k9.c0124;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.c0124.PublicKey;
import com.c0124.k9.Account;
import com.c0124.k9.PRNGFixes;
import com.c0124.k9.activity.MessageCompose;
import com.c0124.k9.c0124.exception.GetPublicKeyFailedException;
import com.c0124.k9.c0124.exception.NoConnectionException;
import com.c0124.k9.c0124.exception.PublicKeyNotFoundInRespositoryException;
import com.c0124.k9.c0124.exception.SCPGPException;
import com.c0124.k9.c0124.exception.SCPGPPrivateKeyNotFoundException;
import com.c0124.k9.c0124.exception.TokenNotFound;
import com.c0124.k9.crypto.Apg;
import com.c0124.k9.crypto.CryptoProvider;
import com.c0124.k9.crypto.PgpData;
import com.c0124.k9.mail.Message;
import com.c0124.k9.mail.MessagingException;
import com.c0124.k9.mail.Part;
import com.c0124.k9.mail.internet.MimeUtility;
import com.c0124.utility.C0124Helper;

import org.apache.commons.io.IOUtils;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPException;
import org.apache.commons.io.IOUtils;
import org.apache.thrift.TException;

@SuppressWarnings("unused")
public class SCPGPProvider extends CryptoProvider {

    static final long serialVersionUID = 0x31415926;

    final static protected String password = "142857abcdef";
    public static final String NAME = "SCPGPProvider";
    private static final String TAG = "SCPGPProvider";
    private static final boolean DEBUG = true;

    private static class LazyHolder {
        private static final SCPGPProvider INSTANCE = new SCPGPProvider();
    }

    public static SCPGPProvider getInstance() {
        return LazyHolder.INSTANCE;
    }
    
    @Override
    public String getName() {
        return NAME;
    }

    //private static final int mMinRequiredVersion = 8;

    //see http://tools.ietf.org/html/rfc3156 for PGP message format
    public static Pattern PGP_MESSAGE =
        Pattern.compile(".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*",
                        Pattern.DOTALL);

    //RFC3156 is different from class apg, follow RFC3156
    public static Pattern PGP_SIGNED_MESSAGE = PGP_MESSAGE;

    public static String PGP_PREFIX = "-----BEGIN PGP MESSAGE-----";
    public static String PGP_SUFFIX = "-----END PGP MESSAGE-----";
    public static String DELIMITTER1 = "\r\n\r\n";
    public static String DELIMITTER2 = "\n\n";
    
    // In HTML, the <br> tag has no end tag.
    // In XHTML, the <br> tag must be properly closed, like this: <br />.
    // ? means do not greedy match
    public static String HTML_NEWLINE = "<br.*?/>";
    public static String HTML_TAG = "<.*?>";
    
    private SCPGPProvider() {
        /*
         * Sets Bouncy (Spongy) Castle as preferred security provider
         *
         * insertProviderAt() position starts from 1
         */
        Security.insertProviderAt(new BouncyCastleProvider(), 1);

        /*
         * apply RNG fixes
         *
         * among other things, executes Security.insertProviderAt(new
         * LinuxPRNGSecureRandomProvider(), 1) for Android <= SDK 17
         */
        PRNGFixes.apply();
        Log.d(TAG, "Bouncy Castle set and PRNG Fixes applied!");

        if (DEBUG) {
            Provider[] providers = Security.getProviders();
            Log.d(TAG, "Installed Security Providers:");
            for (Provider p : providers) {
                Log.d(TAG, "provider class: " + p.getClass().getName());
            }
        }
    }

    @Override
    public boolean isAvailable(Context context) {
        return true;
    }

    /**
     * Select the signature key.
     *
     * @param activity
     * @param pgpData
     * @return success or failure
     */
    @Override
    public boolean selectSecretKey(Activity activity, PgpData pgpData) {
        //TODO:
    	//This method is not useful but have to implement it
    	//Re-consider it if we need sign
    	((MessageCompose) activity).updateEncryptLayout();
        return true;
    }

    /**
     * Get secret key ids based on a given email.
     *
     * @param context
     * @param email The email in question.
     * @return key ids
     */
    @Override
    public long[] getSecretKeyIdsFromEmail(Context context, String email) {
        //TODO:
    	//This method is not really useful but have to implement it
    	//Re-consider it if we need sign
    	return null;
    }

    /**
     * Handle the activity results that concern us.
     *
     * @param activity
     * @param requestCode
     * @param resultCode
     * @param data
     * @return handled or not
     */
    @Override
    public boolean onActivityResult(Activity activity, int requestCode, int resultCode,
                                    android.content.Intent data, PgpData pgpData) {
        throw new UnsupportedOperationException();
    }

    /**
     * Handle the activity results that concern us.
     */
    @Override
    public boolean onDecryptActivityResult(CryptoDecryptCallback callback,
            int requestCode, int resultCode, Intent data, PgpData pgpData) {
    	throw new UnsupportedOperationException();
    }


    /**
     * Test the APG installation.
     *
     * @return success for native provider
     */
    @Override
    public boolean test(Context context) {
        return true;
    }

    @Override
    public boolean selectEncryptionKeys(Activity activity, String emails,
            PgpData pgpData) {
    	throw new UnsupportedOperationException();
    }

    @Override
    public long[] getPublicKeyIdsFromEmail(Context context, String email) {
    	throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasSecretKeyForEmail(Context context, String email) {
    	throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPublicKeyForEmail(Context context, String email) {
    	throw new UnsupportedOperationException();
    }

    @Override
    public String getUserId(Context context, long keyId) {
    	throw new UnsupportedOperationException();
    }
    
    @SuppressLint("NewApi")
	public boolean isEncrypted(String text) {
        if( text == null) return false;
        int begin = text.indexOf(PGP_PREFIX);
        if ( begin < 0) {
            return false;
        }
        String prefix = text.substring(0,begin);
        if( prefix != null && !prefix.isEmpty()) {
            prefix = prefix.replaceAll(HTML_TAG, "");
            prefix = prefix.trim();
            if ( !prefix.isEmpty()) {
                return false;
            }
        }
        Matcher matcher = PGP_MESSAGE.matcher(text);
        return matcher.matches();
    }
    
    //remove what is before -----BEGIN PGP MESSAGE----- and
    //what is after -----END PGP MESSAGE-----
    public String trimEncrypted(String text) {
        int begin = text.indexOf(PGP_PREFIX);
        int end = text.indexOf(PGP_SUFFIX);
        text = text.substring(begin, end + PGP_SUFFIX.length());
        //To work around HTML <br> tags
        //This is a temporary solution, ideally should find where
        //those tags are inserted. But on the other hand, this may
        //be safer because quite a few places may change the format
        //and insert tags
        //I think these tags are inserted in sender side because emails
        //sent by Mailvelop do not have is issue
        
        text = text.replaceAll(HTML_NEWLINE, "\r\n");
        //To work around stupid Mailvelope (not MailEvelope) added text
        begin = text.indexOf(DELIMITTER1);
        if ( begin > 0) {
            return PGP_PREFIX + DELIMITTER1 + text.substring(begin + DELIMITTER1.length());
        }
        begin = text.indexOf(DELIMITTER2);
        if ( begin > 0) {
            return PGP_PREFIX + DELIMITTER2 + text.substring(begin + DELIMITTER2.length());
        }
        return text;
    }
    
    @Override
    public boolean isEncrypted(Message message) {
        String data = null;
        try {
            Part part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part == null) {
                part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            }
            if (part != null) {
                data = MimeUtility.getTextFromPart(part);
            }
        } catch (MessagingException e) {
            // guess not...
            // TODO: maybe log this?
        }

        if (data == null) {
            return false;
        }

        Matcher matcher = PGP_MESSAGE.matcher(data);
        return matcher.matches();
    }

    @Override
    public boolean isSigned(Message message) {
        String data = null;
        try {
            Part part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part == null) {
                part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            }
            if (part != null) {
                data = MimeUtility.getTextFromPart(part);
            }
        } catch (MessagingException e) {
            // guess not...
            // TODO: maybe log this?
        }

        if (data == null) {
            return false;
        }

        Matcher matcher = PGP_SIGNED_MESSAGE.matcher(data);
        return matcher.matches();
    }

    public boolean createNewPair(Activity activity, String myEmail) {
        try {
            KeyManager.getInstance(activity).startCreateNewPair(activity, myEmail);
        } catch (Exception e) {
            e.printStackTrace();
            ClientHelper.w("SCPGPProvider", "ex:" + e.getMessage() + ", trace:" + e.getStackTrace());
            return false;
        }

        return true;
    }

    public Map<String, PublicKey> getPublicKeys(Context context, String senderEmail, Set<String> recipients) 
                                            throws NoConnectionException, TokenNotFound, GetPublicKeyFailedException, 
                                            PublicKeyNotFoundInRespositoryException, TException {
        int nRetrys = 3;
        while(true) {
            try {
                return KeyManager.getInstance(context).getPublicKeys(context, senderEmail, recipients);
            } catch(GetPublicKeyFailedException e) {
                if ( nRetrys-- < 0) {
                    throw e;
                } else {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            
        }
    }

    public boolean isGoodForSendingPGPEmail(Context context, String email) {
  	  return KeyManager.getInstance(context).isGoodForSendingPGPEmail(email);
    }
    
    //-----BEGIN PGP MESSAGE-----
    //Version: BCPG v@RELEASE_NAME@
    //Change second line to
    //Version: NetEnvelop v1.0
    static private String pgpHeaderForNetEnvelop;
    
    static private String GetNetEnvelopLine(Context context) {
        if (pgpHeaderForNetEnvelop.length() == 0) {
            final String Header = "-----BEGIN PGP MESSAGE-----\n";

            String version = "?";
            try {
                PackageInfo pi =
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                version = pi.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                ClientHelper.w(TAG, "Package name not found: " + e.getMessage());
            }

            String neString =
                Header + "Version: NetEnvelop " + version + "\n"
                       + "Comment: http://www.netenvelop.com \n";
            pgpHeaderForNetEnvelop = neString;
        }

        return pgpHeaderForNetEnvelop;

    }
    
    //pos is the position of the first char in the num line (line num start from 0)
    int getLengthOfLines(byte[] input, int num) {
        int pos = 0;
        for(int i = 0; i < num; ++i ) {
            while( pos < input.length && input[pos] != '\n' && input[pos] != '\r') {
                pos++;
            }
            if ( pos + 1 < input.length) {
                if( (input[pos] == '\n' && input[pos+1] == '\r') || 
                    (input[pos] == '\r' && input[pos+1] == '\n') ) {
                    pos += 2;
                }
                else {
                    pos +=1;
                }
            } else {
                pos +=1;
            }
            if ( pos >= input.length) {
                break;
            }
        }
        return pos;
    }
    
    private void changeToNetEnvelopText(Context context, byte[] input, OutputStream output) throws IOException {
        int newPos = getLengthOfLines(input, 2);
        ByteArrayInputStream sIn = new ByteArrayInputStream(input, newPos, input.length );
        
        output.write(GetNetEnvelopLine(context).getBytes());
        IOUtils.copy(sIn, output);
    }
    
    public void encrypt(Context context, InputStream sIn, long length, OutputStream sOut,
                    ArrayList<InputStream> publicKeys) {
        boolean asciiArmored = true;
        boolean integrityCheck = true;
        ByteArrayOutputStream sOut1 = new ByteArrayOutputStream();
        try {
            PGPUtils.encrypt(sIn, length, sOut1, publicKeys, asciiArmored, integrityCheck);
            changeToNetEnvelopText(context, sOut1.toByteArray(), sOut);
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("SCPGPProvider", "ex:" + e.getMessage() + ", trace:" + e.getStackTrace());
        }
    }

	public void decrypt(Context context, Account account, InputStream sIn, OutputStream sOut) throws SCPGPPrivateKeyNotFoundException {
		boolean asciiArmored = true;
	    boolean integrityCheck = true;
	    try {
	        PGPUtils.decrypt(
	              			sIn,
	              			sOut,
	              			KeyManager.getInstance(context).getPrivateKeys(account.getEmail()),
	              			SCPGPProvider.password.toCharArray()
	              			);
	    } catch (IOException e) {
	      e.printStackTrace();
	      Log.w("SCPGPProvider", "ex:" + e.getMessage() + ", trace:" + e.getStackTrace());
	    } catch (PGPException e) {
	          e.printStackTrace();
	          Log.w("SCPGPProvider", "ex:" + e.getMessage() + ", trace:" + e.getStackTrace());
	    }   		
	}
	
	@Override
	public boolean decrypt(Fragment fragment, String data, PgpData pgpData) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean encrypt(Activity activity, String data, PgpData pgpData) {
		throw new UnsupportedOperationException();
	}

    /*
	public void dumpPublicKeys(Context context, Map<String, ByteArrayInputStream> publicKeyForUser)  {
	    if ( !com.c0124.utility.DebugPolicy.getInstance().toDumpKeysLocally())  {
	        return;
	    }
		File outputDir = context.getExternalCacheDir();
		try {
			File outputFile = File.createTempFile("keys", "bpg", outputDir);
			FileOutputStream str = new FileOutputStream(outputFile);
			Log.d(TAG, outputFile.getAbsolutePath());
			for (Map.Entry<String, ByteArrayInputStream> entry : publicKeyForUser.entrySet())
			{
			    str.write(entry.getKey().getBytes());
			    str.write("\r\n".getBytes());
			    IOUtils.copy(entry.getValue(), str);
			    entry.getValue().reset();
			    str.write("\r\n".getBytes());
			    
			}
			str.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void dumpMyPrivateKeys(Context context)  {
        if ( !com.c0124.utility.DebugPolicy.getInstance().toDumpKeysLocally()) {
            return;
        }
		File outputDir = context.getExternalCacheDir();
		ArrayList<ByteArrayInputStream> keys = KeyManager.getInstance(context).getPrivateKeys();
		try {
			File outputFile = File.createTempFile("keys", "bpg", outputDir);
			FileOutputStream str = new FileOutputStream(outputFile);
			Log.d(TAG, outputFile.getAbsolutePath());
			for (ByteArrayInputStream key : keys)
			{
			    IOUtils.copy(key, str);
			    key.reset();
			    str.write("\r\n".getBytes());			    
			}
			str.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	*/
	static
	{
	    pgpHeaderForNetEnvelop = "";
	}
	
}