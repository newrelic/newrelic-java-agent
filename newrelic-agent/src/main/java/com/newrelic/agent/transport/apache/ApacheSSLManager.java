/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.apache;

import com.newrelic.agent.Agent;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Level;

public class ApacheSSLManager {
    private static final String NEW_RELIC_CERT = "META-INF/newrelic-com.pem";


    public static SSLContext createSSLContext(String caBundlePath) {
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        try {
            if (caBundlePath != null) {
                sslContextBuilder.loadTrustMaterial(getKeyStore(caBundlePath), null);
            } else {
                addNewRelicCertToTrustStore(sslContextBuilder);
            }
            return sslContextBuilder.build();
        } catch (Exception e) {
            Agent.LOG.log(Level.WARNING, e, "Unable to create SSL context");
            return null;
        }
    }

    private static void addNewRelicCertToTrustStore(SSLContextBuilder sslContextBuilder)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        URL nrCertUrl = ApacheSSLManager.class.getClassLoader().getResource(NEW_RELIC_CERT);
        if (nrCertUrl != null) {
            try (InputStream is = nrCertUrl.openStream()) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
                boolean sslCertIsValid = isSslCertValid(cert);
                if (sslCertIsValid) {
                    logIfExpiringSoon(cert.getNotAfter());
                    // Initialize keystore and add valid New Relic certificate
                    keystore.load(null, null);
                    keystore.setCertificateEntry("newrelic", cert);
                    Agent.LOG.log(Level.FINEST, "Installed New Relic ssl certificate at alias: newrelic. ");
                    Agent.LOG.log(Level.FINEST, "SSL Certificate expires on: {0}", cert.getNotAfter());
                }
            } catch (IOException e) {
                Agent.LOG.log(Level.INFO, "Unable to add bundled New Relic ssl certificate.", e);
            }
        } else {
            Agent.LOG.log(Level.INFO, "Unable to find bundled New Relic ssl certificate.");
        }
        sslContextBuilder.loadTrustMaterial(keystore, null);
    }

    private static void logIfExpiringSoon(Date expiry) {
        // log if less than 3 months left until certificate expires
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, +3);
        if (cal.getTime().compareTo(expiry) > 0) {
            Agent.LOG.log(Level.WARNING, "New Relic ssl certificate expire on {0}.\n" +
                    "Applications using a custom Trustore may need to update the agent " +
                    "or provide a valid certificate using the ca_bundle_path config", expiry);
        }
    }

    private static boolean isSslCertValid(X509Certificate cert) {
        try {
            cert.checkValidity();
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            Agent.LOG.log(Level.WARNING, "New Relic ssl certificate has expired.\n" +
                    "Applications using a custom Trustore may need to update the agent " +
                    "or provide a valid certificate using the ca_bundle_path config", e);
            return false;
        }
        return true;
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
                                "Unable to generate ca_bundle_path certificate. Will not process further certs.", t);
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
