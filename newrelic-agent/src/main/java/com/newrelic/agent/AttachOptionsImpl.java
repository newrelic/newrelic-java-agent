package com.newrelic.agent;

import java.util.zip.Deflater;

import org.apache.commons.cli.CommandLine;
import org.json.simple.JSONValue;

import com.newrelic.agent.discovery.AttachOptions;
import com.newrelic.agent.discovery.JsonSerializer;
import com.newrelic.agent.transport.DataSenderWriter;

class AttachOptionsImpl implements AttachOptions {

    static final String ATTACH_PID = "pid";
    static final String LICENSE_KEY = "license";
    static final char ATTACH_LIST_OPTION = 'l';
    static final String JSON_OPTION = "json";

    private final boolean list;
    private final String pid;
    private final String appName;
    private final JsonSerializer jsonSerializer;
    private final boolean jsonFormat;
    private final String licenseKey;

    public AttachOptionsImpl(CommandLine cmd) {
        this.jsonSerializer = new JsonSerializer() {
            @Override
            public String serialize(Object object, boolean encode) {
                return encode ? DataSenderWriter.getJsonifiedCompressedEncodedString(object,
                        Deflater.DEFAULT_COMPRESSION) : JSONValue.toJSONString(object);
            }
        };
        jsonFormat = cmd.hasOption(JSON_OPTION);
        this.list = cmd.hasOption(ATTACH_LIST_OPTION);
        if (cmd.hasOption(ATTACH_PID)) {
            final String[] args = cmd.getOptionValues(ATTACH_PID);
            if (args.length < 1) {
                throw new RuntimeException("The pid option requires a process id");
            }
            this.pid = args[0];
            this.appName = args.length > 1 ? args[1] : null;
        } else {
            this.pid = null;
            this.appName = null;
        }
        this.licenseKey = cmd.getOptionValue(LICENSE_KEY);
    }

    @Override
    public boolean isJsonFormat() {
        return jsonFormat;
    }

    @Override
    public boolean isList() {
        return list;
    }

    @Override
    public String getPid() {
        return pid;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public String toString() {
        return "AttachOptionsImpl [list=" + list + ", pid=" + pid + ", appName=" + appName + "]";
    }

    @Override
    public JsonSerializer getSerializer() {
        return jsonSerializer;
    }

    @Override
    public String getLicenseKey() {
        return licenseKey;
    }
}