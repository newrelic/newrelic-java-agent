/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;


import java.util.Map;
import java.util.HashMap;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class TestJarFile {
    // Since they are stings in the manifest, that's how we will represent them here
    private String name = "weave_unittest_jar";
    private String alias = null;
    private String vendorId = null;
    private String version = null;
    private String enabled = null;
    private String clearReturnStacksDefault = null;

    // Internal jar data
    private Manifest jarManifest;
    private ByteArrayOutputStream jarFile;

    /**
     * Only required prop is the implementation-title which becomes 'name'
     */
    public TestJarFile() throws IOException {
        this.createManifest();
        this.createJarFile(this.jarManifest);
    }

    /**
     * Allow setting manifest props
     */
    public TestJarFile(String name, String alias, String vendorId, String version, String enabled) throws IOException {
        this(name, alias, vendorId, version, enabled, null);
    }

    /**
     * Allow setting manifest props, including Clear-Return-Stacks-Default
     */
    public TestJarFile(String name, String alias, String vendorId, String version, String enabled,
            String clearReturnStacksDefault) throws IOException {
        if (name != null)     this.name = name;
        if (alias != null)    this.alias = alias;
        if (vendorId != null) this.vendorId = vendorId;
        if (version != null)  this.version = version;
        if (enabled != null)  this.enabled = enabled;
        if (clearReturnStacksDefault != null) this.clearReturnStacksDefault = clearReturnStacksDefault;

        this.createManifest();
        this.createJarFile(this.jarManifest);
    }

    private void createManifest() throws IOException {
        this.jarManifest = new Manifest();

        this.jarManifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        this.jarManifest.getMainAttributes().put(new Attributes.Name("Implementation-Title"), this.name);

        if (this.enabled != null)  this.jarManifest.getMainAttributes().put(new Attributes.Name("Enabled"), this.enabled);
        if (this.clearReturnStacksDefault != null) this.jarManifest.getMainAttributes().put(new Attributes.Name("Clear-Return-Stacks-Default"), this.clearReturnStacksDefault);
        if (this.version != null)  this.jarManifest.getMainAttributes().put(new Attributes.Name("Implementation-Version"), String.valueOf(this.version));
        if (this.alias != null)    this.jarManifest.getMainAttributes().put(new Attributes.Name("Implementation-Title-Alias"), this.alias);
        if (this.vendorId != null) this.jarManifest.getMainAttributes().put(new Attributes.Name("Implementation-Vendor-Id"), this.vendorId);
    }

    private void createJarFile(Manifest manifest) throws IOException {
        this.jarFile = new ByteArrayOutputStream();

        if (manifest != null) {
            JarOutputStream testJarOutputStream = new JarOutputStream(this.jarFile, manifest);
            testJarOutputStream.close();
        }
    }

    public JarInputStream getInputStream() throws IOException {
        JarInputStream jarStream = null;

        if (jarFile != null) {
            jarStream = new JarInputStream(new ByteArrayInputStream(jarFile.toByteArray()));
        }

        return jarStream;
    }
}
