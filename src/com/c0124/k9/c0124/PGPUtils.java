package com.c0124.k9.c0124;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.PublicKeyAlgorithmTags;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPEncryptedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPOnePassSignatureList;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPPublicKeyRingCollection;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSecretKeyRingCollection;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.spongycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.spongycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.spongycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;

import com.c0124.k9.c0124.exception.SCPGPPrivateKeyNotFoundException;

import android.util.Log;

/**
 * @author TengJing
 * I guess the code is from:
 * http://sloanseaman.com/wordpress/2012/05/13/revisited-pgp-encryptiondecryption-in-java/
 *
 */
public class PGPUtils {

    /*
    private static final int   BUFFER_SIZE = 1 << 16; // should always be power of 2
    */
    private static final int   KEY_FLAGS = 27;
    private static final int[] MASTER_KEY_CERTIFICATION_TYPES = new int[]{
        PGPSignature.POSITIVE_CERTIFICATION,
        PGPSignature.CASUAL_CERTIFICATION,
        PGPSignature.NO_CERTIFICATION,
        PGPSignature.DEFAULT_CERTIFICATION
    };

    private static final String TAG = "PGPUtils";

    public static PGPPublicKey readPublicKey(InputStream in)
        throws IOException, PGPException
    {

        PGPPublicKeyRingCollection keyRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(in));

        //
        // we just loop through the collection till we find a key suitable for encryption, in the real
        // world you would probably want to be a bit smarter about this.
        //
        PGPPublicKey publicKey = null;

        //
        // iterate through the key rings.
        //
        @SuppressWarnings("unchecked")
        Iterator<PGPPublicKeyRing> rIt = keyRingCollection.getKeyRings();

        while (publicKey == null && rIt.hasNext()) {
            PGPPublicKeyRing kRing = rIt.next();
            @SuppressWarnings("unchecked")
            Iterator<PGPPublicKey> kIt = kRing.getPublicKeys();
            while (publicKey == null && kIt.hasNext()) {
                PGPPublicKey key = kIt.next();
                if (key.isEncryptionKey()) {
                    publicKey = key;
                }
            }
        }

        if (publicKey == null) {
            Log.w(TAG, "Can't find public key in the key ring.");
            return publicKey;
        }
        
        if (!isForEncryption(publicKey)) {
            Log.w(TAG, "KeyID " + publicKey.getKeyID() + " not flagged for encryption.");
        }

        return publicKey;
    }

    public static PGPSecretKey readSecretKey(InputStream in)
        throws IOException, PGPException
    {

        PGPSecretKeyRingCollection keyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(in));

        //
        // We just loop through the collection till we find a key suitable for signing.
        // In the real world you would probably want to be a bit smarter about this.
        //
        PGPSecretKey secretKey = null;
        
        @SuppressWarnings("unchecked")
        Iterator<PGPSecretKeyRing> rIt = keyRingCollection.getKeyRings();
        while (secretKey == null && rIt.hasNext()) {
            PGPSecretKeyRing keyRing = rIt.next();
            
            @SuppressWarnings("unchecked")
            Iterator<PGPSecretKey> kIt = keyRing.getSecretKeys();
            while (secretKey == null && kIt.hasNext()) {
                PGPSecretKey key = kIt.next();
                if (key.isSigningKey()) {
                    secretKey = key;
                }
            }
        }

        // Validate secret key
        if (secretKey == null) {
            throw new IllegalArgumentException("Can't find private key in the key ring.");
        }
        if (!secretKey.isSigningKey()) {
            throw new IllegalArgumentException("Private key does not allow signing.");
        }
        if (secretKey.getPublicKey().isRevoked()) {
            throw new IllegalArgumentException("Private key has been revoked.");
        }
        if (!hasKeyFlags(secretKey.getPublicKey(), KeyFlags.SIGN_DATA)) {
            throw new IllegalArgumentException("Key cannot be used for signing.");
        }

        return secretKey;
    }

    /**
     * Load a secret key and find the private key in it
     * @param pgpSecKey The secret key
     * @param pass passphrase to decrypt secret key with
     * @return
     * @throws PGPException
     */
    public static PGPPrivateKey findPrivateKey(PGPSecretKey pgpSecKey, char[] pass)
        throws PGPException
    {
        if (pgpSecKey == null) return null;

        PBESecretKeyDecryptor decryptor = new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(pass);
        return pgpSecKey.extractPrivateKey(decryptor);
    }

    /**
     * decrypt the passed in message stream
     * @throws IOException 
     * @throws PGPException 
     */
    public static void decrypt(InputStream in, OutputStream out,
        ArrayList<ByteArrayInputStream> keyIns, char[] passwd) throws SCPGPPrivateKeyNotFoundException, IOException, PGPException
    {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        in = org.spongycastle.openpgp.PGPUtil.getDecoderStream(in);

        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList enc;

        Object o = pgpF.nextObject();
        //
        // the first object might be a PGP marker packet.
        //
        if (o instanceof  PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        //
        // find the secret key
        //
        PGPPrivateKey sKey = null;
        PGPPublicKeyEncryptedData pbe = null;

        for (InputStream keyIn : keyIns) {
            PGPSecretKeyRingCollection pgpSec =
                new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(keyIn));

            @SuppressWarnings("unchecked")
            Iterator<PGPPublicKeyEncryptedData> it = enc.getEncryptedDataObjects();
            while (sKey == null && it.hasNext()) {
                pbe = it.next();
                sKey = findPrivateKey(pgpSec.getSecretKey(pbe.getKeyID()), passwd);
            }
            if (sKey != null) {
                break;
            }
        }
        if (sKey == null) {
            throw new SCPGPPrivateKeyNotFoundException("Secret key for message not found.");
        }

        InputStream clear = pbe.getDataStream(new BcPublicKeyDataDecryptorFactory(sKey));

        PGPObjectFactory plainFact = new PGPObjectFactory(clear);

        Object message = plainFact.nextObject();

        if (message instanceof  PGPCompressedData) {
            PGPCompressedData cData = (PGPCompressedData) message;
            PGPObjectFactory pgpFact = new PGPObjectFactory(cData.getDataStream());

            message = pgpFact.nextObject();
        }

        if (message instanceof  PGPLiteralData) {
            PGPLiteralData ld = (PGPLiteralData) message;
            IOUtils.copy(ld.getInputStream(), out);
        } else if (message instanceof  PGPOnePassSignatureList) {
            throw new PGPException("Encrypted message contains a signed message - not literal data.");
        } else {
            throw new PGPException("Message is not a simple encrypted file - type unknown.");
        }

        if (pbe.isIntegrityProtected()) {
            if (!pbe.verify()) {
                throw new PGPException("Message failed integrity check");
            }
        }
    }

    private static void compress(InputStream input, long length, OutputStream output, String fileName, int algorithm) throws IOException
    {
        PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(algorithm);
        OutputStream cos = comData.open(output); // open it with the final destination

        PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
        OutputStream  pOut = lData.open(cos, // the compressed output stream
                                        PGPLiteralData.BINARY,
                                        fileName,  // "filename" to store
                                        length, // length of clear data
                                        new Date()  // current time
                                      );
        IOUtils.copy(input,pOut);
        pOut.close();
        comData.close();
    }
    
    public static void encrypt(InputStream input, 
                               long length, OutputStream output,
                               ArrayList<InputStream> encKeys, 
                               boolean armor, 
                               boolean withIntegrityCheck)
        throws IOException, NoSuchProviderException, PGPException 
    {
        // Security.addProvider(new BouncyCastleProvider());
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        compress(input, length, compressed, PGPLiteralData.CONSOLE, PGPCompressedData.ZIP);

        BcPGPDataEncryptorBuilder dataEncryptor =
            new BcPGPDataEncryptorBuilder(PGPEncryptedData.TRIPLE_DES);
        dataEncryptor.setWithIntegrityPacket(withIntegrityCheck);
        /* Eclipse Warning:
         * Potentially insecure random numbers on Android 4.3 and older. 
         * Read https://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html for more info.
         * */
        dataEncryptor.setSecureRandom(new SecureRandom());

        PGPEncryptedDataGenerator encryptedDataGenerator =
            new PGPEncryptedDataGenerator(dataEncryptor);
        for (InputStream key : encKeys) {
            encryptedDataGenerator.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(
                PGPUtils.readPublicKey(key)));
        }
        OutputStream out1 = output;
        if (armor) {
            out1 = new ArmoredOutputStream(out1);
        }

        OutputStream cOut = encryptedDataGenerator.open(out1, compressed.toByteArray().length);
        cOut.write(compressed.toByteArray());
        cOut.close();
        if (armor) {
            out1.close();
        }
    }
/*
    @SuppressWarnings("unchecked")
    public static void signEncryptFile(
        OutputStream out,
        String fileName,
        PGPPublicKey publicKey,
        PGPSecretKey secretKey,
        String password,
        boolean armor,
        boolean withIntegrityCheck )
        throws Exception
    {

        // Initialize Bouncy Castle security provider
        Provider provider = new BouncyCastleProvider();
        Security.addProvider(provider);

        if (armor) {
            out = new ArmoredOutputStream(out);
        }

        BcPGPDataEncryptorBuilder dataEncryptor = new BcPGPDataEncryptorBuilder(PGPEncryptedData.TRIPLE_DES);
        dataEncryptor.setWithIntegrityPacket(withIntegrityCheck);
        dataEncryptor.setSecureRandom(new SecureRandom());

        PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(dataEncryptor);
        encryptedDataGenerator.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(publicKey));

        OutputStream encryptedOut = encryptedDataGenerator.open(out, new byte[PGPUtils.BUFFER_SIZE]);

        // Initialize compressed data generator
        PGPCompressedDataGenerator compressedDataGenerator = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
        OutputStream compressedOut = compressedDataGenerator.open(encryptedOut, new byte [PGPUtils.BUFFER_SIZE]);

        // Initialize signature generator
        PGPPrivateKey privateKey = findPrivateKey(secretKey, password.toCharArray());

        PGPContentSignerBuilder signerBuilder = new BcPGPContentSignerBuilder(secretKey.getPublicKey().getAlgorithm(),
                HashAlgorithmTags.SHA1);

        PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(signerBuilder);
        signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);

        boolean firstTime = true;
        Iterator<String> it = secretKey.getPublicKey().getUserIDs();
        while (it.hasNext() && firstTime) {
            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            spGen.setSignerUserID(false, it.next());
            signatureGenerator.setHashedSubpackets(spGen.generate());
            // Exit the loop after the first iteration
            firstTime = false;
        }
        signatureGenerator.generateOnePassVersion(false).encode(compressedOut);

        // Initialize literal data generator
        PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
        OutputStream literalOut = literalDataGenerator.open(
            compressedOut,
            PGPLiteralData.BINARY,
            fileName,
            new Date(),
            new byte [PGPUtils.BUFFER_SIZE] );

        // Main loop - read the "in" stream, compress, encrypt and write to the "out" stream
        FileInputStream in = new FileInputStream(fileName);
        byte[] buf = new byte[PGPUtils.BUFFER_SIZE];
        int len;
        while ((len = in.read(buf)) > 0) {
            literalOut.write(buf, 0, len);
            signatureGenerator.update(buf, 0, len);
        }

        in.close();
        literalDataGenerator.close();
        // Generate the signature, compress, encrypt and write to the "out" stream
        signatureGenerator.generate().encode(compressedOut);
        compressedDataGenerator.close();
        encryptedDataGenerator.close();
        if (armor) {
            out.close();
        }
    }

    public static boolean verifyFile(
        InputStream in,
        InputStream keyIn,
        String extractContentFile)
        throws Exception
    {
        in = PGPUtil.getDecoderStream(in);

        PGPObjectFactory pgpFact = new PGPObjectFactory(in);
        PGPCompressedData c1 = (PGPCompressedData)pgpFact.nextObject();

        pgpFact = new PGPObjectFactory(c1.getDataStream());

        PGPOnePassSignatureList p1 = (PGPOnePassSignatureList)pgpFact.nextObject();

        PGPOnePassSignature ops = p1.get(0);

        PGPLiteralData p2 = (PGPLiteralData)pgpFact.nextObject();

        InputStream dIn = p2.getInputStream();

        IOUtils.copy(dIn, new FileOutputStream(extractContentFile));

        int ch;
        PGPPublicKeyRingCollection pgpRing = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(keyIn));

        PGPPublicKey key = pgpRing.getPublicKey(ops.getKeyID());

        FileOutputStream out = new FileOutputStream(p2.getFileName());

        ops.init(new BcPGPContentVerifierBuilderProvider(), key);

        while ((ch = dIn.read()) >= 0)
        {
            ops.update((byte)ch);
            out.write(ch);
        }

        out.close();

        PGPSignatureList p3 = (PGPSignatureList)pgpFact.nextObject();
        return ops.verify(p3.get(0));
    }
*/
    /**
     * From LockBox Lobs PGP Encryption tools.
     * http://www.lockboxlabs.org/content/downloads
     *
     * I didn't think it was worth having to import a 4meg lib for three methods
     * @param key
     * @return
     */
    public static boolean isForEncryption(PGPPublicKey key)
    {
        if (key.getAlgorithm() == PublicKeyAlgorithmTags.RSA_SIGN
            || key.getAlgorithm() == PublicKeyAlgorithmTags.DSA
            || key.getAlgorithm() == PublicKeyAlgorithmTags.EC
            || key.getAlgorithm() == PublicKeyAlgorithmTags.ECDSA)
        {
            return false;
        }

        return hasKeyFlags(key, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE);
    }

    /**
     * From LockBox Lobs PGP Encryption tools.
     * http://www.lockboxlabs.org/content/downloads
     *
     * I didn't think it was worth having to import a 4meg lib for three methods
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    private static boolean hasKeyFlags(PGPPublicKey encKey, int keyUsage) {
        if (encKey.isMasterKey()) {
            for (int i = 0; i != PGPUtils.MASTER_KEY_CERTIFICATION_TYPES.length; i++) {
                for (Iterator<PGPSignature> eIt = encKey.getSignaturesOfType(PGPUtils.MASTER_KEY_CERTIFICATION_TYPES[i]); eIt.hasNext();) {
                    PGPSignature sig = eIt.next();
                    if (!isMatchingUsage(sig, keyUsage)) {
                        return false;
                    }
                }
            }
        }
        else {
            for (Iterator<PGPSignature> eIt = encKey.getSignaturesOfType(PGPSignature.SUBKEY_BINDING); eIt.hasNext();) {
                PGPSignature sig = eIt.next();
                if (!isMatchingUsage(sig, keyUsage)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * From LockBox Lobs PGP Encryption tools.
     * http://www.lockboxlabs.org/content/downloads
     *
     * I didn't think it was worth having to import a 4meg lib for three methods
     * @param key
     * @return
     */
    private static boolean isMatchingUsage(PGPSignature sig, int keyUsage) {
        if (sig.hasSubpackets()) {
            PGPSignatureSubpacketVector sv = sig.getHashedSubPackets();
            if (sv.hasSubpacket(PGPUtils.KEY_FLAGS)) {
                // code fix suggested by kzt (see comments)
                if ((sv.getKeyFlags() & keyUsage) == 0) {
                    return false;
                }
            }
        }
        return true;
    }
}