/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.apache;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.DataSenderConfig;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;

public class ApacheSSLManager {

    public static SSLContext createSSLContext(DataSenderConfig config) {
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        try {
            if (config.getCaBundlePath() != null) {
                Agent.LOG.log(Level.INFO, "Using ca_bundle_path: {0}", config.getCaBundlePath());
                sslContextBuilder.loadTrustMaterial(getKeyStore(config.getCaBundlePath()), null);
            }
            return sslContextBuilder.build();
        } catch (Exception e) {
            Agent.LOG.log(Level.WARNING, e, "Unable to create SSL context");
            return null;
        }
    }

    private static KeyStore getKeyStore(String caBundlePath)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        Agent.LOG.finer("SSL Keystore Provider: " + keystore.getProvider().getName());

        Collection<X509Certificate> caCerts = new LinkedList<>();
        if (caBundlePath != null) {
            Agent.LOG.log(Level.FINEST, "Checking ca_bundle_path at: {0}", caBundlePath);

            try (InputStream is = new BufferedInputStream(new FileInputStream(caBundlePath))) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                while (is.available() > 0) {
                    try {
                        caCerts.add((X509Certificate) cf.generateCertificate(is));
                    } catch (Throwable t) {
                        Agent.LOG.log(Level.SEVERE,
                                "Unable to generate ca_bundle_path certificate. Verify the certificate format. Will not process further certs.", t);
                        break;
                    }
                }
            }

            Agent.LOG.log(
                    caCerts.size() > 0 ? Level.INFO : Level.SEVERE,
                    "Read ca_bundle_path {0} and found {1} certificates.",
                    caBundlePath,
                    caCerts.size());

            // Initialize the keystore
            keystore.load(null, null);

            int i = 1;
            for (X509Certificate caCert : caCerts) {
                if (caCert != null) {
                    String alias = "ca_bundle_path_" + i;
                    keystore.setCertificateEntry(alias, caCert);

                    Agent.LOG.log(Level.FINEST, "Installed certificate {0} at alias: {1}", i, alias);
                    if (Agent.isDebugEnabled()) {
                        Agent.LOG.log(Level.FINEST, "Installed certificate {0} at alias: {1}", caCert, alias);
                    }
                }
                i++;
            }
        }

        return keystore;
    }
}
