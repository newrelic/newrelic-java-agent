/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.fake;

import javax.xml.namespace.QName;
import javax.xml.rpc.Call;
import javax.xml.rpc.ParameterMode;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MockCall implements Call {

    @Override
    public boolean isParameterAndReturnSpecRequired(QName qName) {
        return false;
    }

    @Override
    public void addParameter(String s, QName qName, ParameterMode parameterMode) {
    }

    @Override
    public void addParameter(String s, QName qName, Class aClass, ParameterMode parameterMode) {
    }

    @Override
    public QName getParameterTypeByName(String s) {
        return null;
    }

    @Override
    public void setReturnType(QName qName) {
    }

    @Override
    public void setReturnType(QName qName, Class aClass) {
    }

    @Override
    public QName getReturnType() {
        return null;
    }

    @Override
    public void removeAllParameters() {
    }

    @Override
    public QName getOperationName() {
        return null;
    }

    @Override
    public void setOperationName(QName qName) {
    }

    @Override
    public QName getPortTypeName() {
        return null;
    }

    @Override
    public void setPortTypeName(QName qName) {
    }

    @Override
    public void setTargetEndpointAddress(String s) {
    }

    @Override
    public String getTargetEndpointAddress() {
        return "http://newrelic.com";
    }

    @Override
    public void setProperty(String s, Object o) {
    }

    @Override
    public Object getProperty(String s) {
        return null;
    }

    @Override
    public void removeProperty(String s) {
    }

    @Override
    public Iterator getPropertyNames() {
        return null;
    }

    @Override
    public Object invoke(Object[] objects) throws RemoteException {
        return new Object();
    }

    @Override
    public Object invoke(QName qName, Object[] objects) throws RemoteException {
        return new Object();
    }

    @Override
    public void invokeOneWay(Object[] objects) {
    }

    @Override
    public Map getOutputParams() {
        return new HashMap();
    }

    @Override
    public List getOutputValues() {
        return new LinkedList();
    }

}
