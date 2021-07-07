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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class KeyStoreHelperTest {

    private static final String KEYSTORE_SERVER_RESOURCE_NAME = "droolsServer.keystore";
    private static final String KEYSTORE_CLIENT_RESOURCE_NAME = "droolsClient.keystore";
    private static final String KEYSTORE_JCEKS_RESOURCE_NAME = "droolsServer.jceks";
    private static final String KEYSTORE_JCEKS_FILENAME = "target/test-classes/org/drools/core/util/droolsServer.jceks";
    private static final String KEYSTORE_SERVER_PASSWORD = "serverpwd";
    private static final String KEYSTORE_CLIENT_PASSWORD = "clientpwd";
    private static final String KEY_ALIAS = "droolsKey";
    private static final String KEY_PASSWORD = "keypwd";
    private static final String KEY_PHRASE = "secretkey";

    @Test
    public void testSignDataWithPrivateKey() throws UnsupportedEncodingException,
                                            UnrecoverableKeyException,
                                            InvalidKeyException,
                                            KeyStoreException,
                                            NoSuchAlgorithmException,
                                            SignatureException {
        // The server signs the data with the private key
        
        // Set properties to simulate the server
        final URL serverKeyStoreURL = getClass().getResource(KEYSTORE_SERVER_RESOURCE_NAME);
        System.setProperty( KeyStoreConstants.PROP_SIGN, Boolean.TRUE.toString() );
        System.setProperty( KeyStoreConstants.PROP_PVT_KS_URL, serverKeyStoreURL.toExternalForm() );
        System.setProperty(KeyStoreConstants.PROP_PVT_KS_PWD, KEYSTORE_SERVER_PASSWORD);
        System.setProperty( KeyStoreConstants.PROP_PVT_ALIAS, KEY_ALIAS );
        System.setProperty( KeyStoreConstants.PROP_PVT_PWD, KEY_PASSWORD );
        final KeyStoreHelper serverHelper = new KeyStoreHelper();

        // get some data to sign
        final byte[] data = "Hello World".getBytes("UTF8" );

        // sign the data
        final byte[] signature = serverHelper.signDataWithPrivateKey(data );

        // now, initialise the client helper
        
        // Set properties to simulate the client
        final URL clientKeyStoreURL = getClass().getResource(KEYSTORE_CLIENT_RESOURCE_NAME );
        System.setProperty( KeyStoreConstants.PROP_SIGN, Boolean.TRUE.toString() );
        System.setProperty( KeyStoreConstants.PROP_PUB_KS_URL, clientKeyStoreURL.toExternalForm() );
        System.setProperty( KeyStoreConstants.PROP_PUB_KS_PWD, KEYSTORE_CLIENT_PASSWORD );
        // client needs no password to access the certificate and public key
        final KeyStoreHelper clientHelper = new KeyStoreHelper( );

        // check the signature against the data
        assertTrue( clientHelper.checkDataWithPublicKey( KEY_ALIAS,
                                                         data,
                                                         signature ) );

        // check some fake data
        assertFalse( clientHelper.checkDataWithPublicKey( KEY_ALIAS,
                                                          "fake".getBytes( "UTF8" ), 
                                                          signature ) );
    }

    @Test
    public void testLoadPasswordNoKeystore() {
        final KeyStoreHelper serverHelper = new KeyStoreHelper();
        try {
            serverHelper.getPasswordKey(null, null);
            fail();
        } catch (final RuntimeException re) {
            assertTrue(true);
        }
    }

    @Test
    public void testLoadPassword() throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, InvalidKeyException, InvalidKeySpecException {
        final SecretKey storedSecretKey = storeKeyIntoKeyStoreFile(KEY_PHRASE);

        // Set properties to simulate the server
        final URL serverKeyStoreURL = getClass().getResource(KEYSTORE_JCEKS_RESOURCE_NAME);
        System.setProperty(KeyStoreConstants.PROP_PWD_KS_URL, serverKeyStoreURL.toExternalForm());
        System.setProperty(KeyStoreConstants.PROP_PWD_KS_PWD, KEYSTORE_SERVER_PASSWORD);

        try {
            final KeyStoreHelper serverHelper = new KeyStoreHelper();

            final String passwordKey = serverHelper.getPasswordKey(KEY_ALIAS, KEY_PASSWORD.toCharArray());
            assertEquals(new String(storedSecretKey.getEncoded()), passwordKey);
        } catch (final RuntimeException re) {
            re.printStackTrace();
            fail(re.getMessage());
        }
    }

    private SecretKey storeKeyIntoKeyStoreFile(final String keyPhrase)
            throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException,
            InvalidKeyException, InvalidKeySpecException {
        final KeyStore keyStore = KeyStore.getInstance("JCEKS");
        keyStore.load(null, KEYSTORE_SERVER_PASSWORD.toCharArray());

        final SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("DES");
        final SecretKey mySecretKey = secretKeyFactory.generateSecret(new DESKeySpec(keyPhrase.getBytes()));
        final KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(mySecretKey);
        keyStore.setEntry(KEY_ALIAS, skEntry, new KeyStore.PasswordProtection(KEY_PASSWORD.toCharArray()));

        try (FileOutputStream fos = new java.io.FileOutputStream(KEYSTORE_JCEKS_FILENAME, false)) {
            keyStore.store(fos, KEYSTORE_SERVER_PASSWORD.toCharArray());
        }
        return mySecretKey;
    }
}
