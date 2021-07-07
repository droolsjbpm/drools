/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.drools.core.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;

import org.drools.core.RuleBaseConfiguration;

import static org.drools.core.util.KeyStoreConstants.KEY_CERTIFICATE_TYPE;
import static org.drools.core.util.KeyStoreConstants.KEY_PASSWORD_TYPE;
import static org.drools.core.util.KeyStoreConstants.PROP_PUB_KS_PWD;
import static org.drools.core.util.KeyStoreConstants.PROP_PUB_KS_URL;
import static org.drools.core.util.KeyStoreConstants.PROP_PVT_ALIAS;
import static org.drools.core.util.KeyStoreConstants.PROP_PVT_KS_PWD;
import static org.drools.core.util.KeyStoreConstants.PROP_PVT_KS_URL;
import static org.drools.core.util.KeyStoreConstants.PROP_PVT_PWD;
import static org.drools.core.util.KeyStoreConstants.PROP_PWD_KS_PWD;
import static org.drools.core.util.KeyStoreConstants.PROP_PWD_KS_URL;

/**
 * A helper class to deal with the key store and signing process during
 * Serialisation
 * <p>
 * This class will read and use the following system properties:
 * <p>
 * drools.serialization.sign = <false|true>
 * drools.serialization.private.keyStoreURL = <URL>
 * drools.serialization.private.keyStorePwd = <password>
 * drools.serialization.private.keyAlias = <key>
 * drools.serialization.private.keyPwd = <password>
 * drools.serialization.public.keyStoreURL = <URL>
 * drools.serialization.public.keyStorePwd = <password>
 */
public class KeyStoreHelper {

    private boolean signed;

    private URL pvtKeyStoreURL;
    private char[] pvtKeyStorePwd;
    private String pvtKeyAlias;
    private char[] pvtKeyPassword;

    private URL pubKeyStoreURL;
    private char[] pubKeyStorePwd;

    private URL pwdKeyStoreURL;
    private char[] pwdKeyStorePwd;

    private KeyStore pvtKeyStore;
    private KeyStore pubKeyStore;
    private KeyStore pwdKeyStore;

    /**
     * Creates a KeyStoreHelper and initialises the KeyStore, by loading its entries.
     * @throws RuntimeException in case any error happens when initialising and loading the keystore.
     */
    public KeyStoreHelper() {
        try {
            this.signed = Boolean.valueOf(System.getProperty(KeyStoreConstants.PROP_SIGN,
                                                             RuleBaseConfiguration.DEFAULT_SIGN_ON_SERIALIZATION)).booleanValue();

            loadPrivateKeyStoreProperties();
            loadPublicKeyStoreProperties();
            loadPasswordKeyStoreProperties();

            initKeyStore();
        } catch (Exception e) {
            throw new RuntimeException("Error initialising KeyStore: " + e.getMessage(), e);
        }
    }

    private void loadPrivateKeyStoreProperties() throws MalformedURLException {
        String url = System.getProperty(PROP_PVT_KS_URL, "");
        if (url.length() > 0) {
            this.pvtKeyStoreURL = new URL(url);
        }
        this.pvtKeyStorePwd = System.getProperty(PROP_PVT_KS_PWD, "").toCharArray();
        this.pvtKeyAlias = System.getProperty(PROP_PVT_ALIAS, "");
        this.pvtKeyPassword = System.getProperty(PROP_PVT_PWD, "").toCharArray();
    }

    private void loadPublicKeyStoreProperties() throws MalformedURLException {
        String url = System.getProperty(PROP_PUB_KS_URL, "");
        if (url.length() > 0) {
            this.pubKeyStoreURL = new URL(url);
        }
        this.pubKeyStorePwd = System.getProperty(PROP_PUB_KS_PWD, "").toCharArray();
    }

    private void loadPasswordKeyStoreProperties() throws MalformedURLException {
        String url = System.getProperty(PROP_PWD_KS_URL, "");
        if (url.length() > 0) {
            pwdKeyStoreURL = new URL(url);
        }
        pwdKeyStorePwd = System.getProperty(PROP_PWD_KS_PWD, "").toCharArray();
    }

    private void initKeyStore() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
        if (pvtKeyStoreURL != null) {
            this.pvtKeyStore = loadKeystore(KEY_CERTIFICATE_TYPE, pvtKeyStoreURL, pvtKeyStorePwd);
        }
        if (pubKeyStoreURL != null) {
            this.pubKeyStore = loadKeystore(KEY_CERTIFICATE_TYPE, pubKeyStoreURL, pubKeyStorePwd);
        }
        if (pwdKeyStoreURL != null) {
            this.pwdKeyStore = loadKeystore(KEY_PASSWORD_TYPE, pwdKeyStoreURL, pwdKeyStorePwd);
        }
    }

    private KeyStore loadKeystore(String keyCertificateType, URL url, char[] password) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance(keyCertificateType);
        keyStore.load(url.openStream(), password);

        return keyStore;
    }

    /**
     * Generates the signature for the given byte[] using MD5 with RSA algorithm and the
     * private key with which this helper was initialised.
     *
     * @param data the byte[] of data to be signed
     *
     * @return the signature, encrypted with the private key
     *
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    public byte[] signDataWithPrivateKey(byte[] data) throws UnrecoverableKeyException,
                                                     KeyStoreException,
                                                     NoSuchAlgorithmException,
                                                     InvalidKeyException,
                                                     SignatureException {
        if( pvtKeyStore == null ) {
            throw new RuntimeException( "Key store with private key not configured. Please configure it properly before using signed serialization." );
        }
        PrivateKey pvtkey = (PrivateKey) pvtKeyStore.getKey( pvtKeyAlias,
                                                             pvtKeyPassword );
        Signature sig = Signature.getInstance( "MD5withRSA" );
        sig.initSign( pvtkey );
        sig.update( data );
        return sig.sign();
    }

    /**
     * Checks the given byte[] data against the signature, using the
     * public key with which this helper was initialised and the algorithm
     * MD5 with RSA.
     *
     * @param data the original data that was signed
     * @param signature the provided signature
     *
     * @return true in case the signature matches, false otherwise.
     *
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    public boolean checkDataWithPublicKey(final String publicKeyAlias,
                                          final byte[] data,
                                          final byte[] signature) throws KeyStoreException,
                                                                 NoSuchAlgorithmException,
                                                                 InvalidKeyException,
                                                                 SignatureException {
        if( pubKeyStore == null ) {
            throw new RuntimeException( "Key store with public key not configured. Please configure it properly before using signed serialization." );
        }
        Certificate cert = pubKeyStore.getCertificate( publicKeyAlias );
        if( cert == null ) {
            throw new RuntimeException( "Public certificate for key '"+publicKeyAlias+"' not found in the configured key store. Impossible to deserialize the object." );
        }
        Signature sig = Signature.getInstance( "MD5withRSA" );
        sig.initVerify( cert.getPublicKey() );
        sig.update( data );
        return sig.verify( signature );
    }

    public String getPasswordKey(String pwdKeyAlias, char[] pwdKeyPassword) {
        SecretKey passwordKey;
        try {
            passwordKey = (SecretKey) pwdKeyStore.getKey(pwdKeyAlias, pwdKeyPassword);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load a key from Key Store. Source " + e.getCause());
        }
        return new String(passwordKey.getEncoded());
    }

    public boolean isSigned() {
        return signed;
    }

    public URL getPvtKeyStoreURL() {
        return pvtKeyStoreURL;
    }

    public char[] getPvtKeyStorePwd() {
        return pvtKeyStorePwd;
    }

    public String getPvtKeyAlias() {
        return pvtKeyAlias;
    }

    public char[] getPvtKeyPassword() {
        return pvtKeyPassword;
    }

    public URL getPubKeyStoreURL() {
        return pubKeyStoreURL;
    }

    public char[] getPubKeyStorePwd() {
        return pubKeyStorePwd;
    }

    public KeyStore getPvtKeyStore() {
        return pvtKeyStore;
    }

    public KeyStore getPubKeyStore() {
        return pubKeyStore;
    }
}
