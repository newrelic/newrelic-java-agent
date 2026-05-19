package io.ktor.server.http.content;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.ktor.server.routing.Route;

import java.io.File;

@Weave(originalName = "io.ktor.server.http.content.StaticContentKt")
public class StaticContentKt_Instrumentation {

    @Trace
    public static File getStaticRootFolder(Route route) {
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Route", route.toString());
        return Weaver.callOriginal();
    }

    @Trace
    public static void setStaticRootFolder(Route route, File rootFolder) {
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Route", route.toString());
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("RootFolder", rootFolder.getAbsolutePath());
        Weaver.callOriginal();
    }

    @Trace
    public static void file(Route route, String remotePath, String localPath) {
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Route", route.toString());
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("RemotePath", remotePath);
        Weaver.callOriginal();
    }

    @Trace
    public static void file(Route route, String remotePath, File localPath) {
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Route", route.toString());
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("RemotePath", remotePath);
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("LocalPath", localPath.getAbsolutePath());
        Weaver.callOriginal();
    }

    @Trace
    public static void files(Route route, String localPath) {
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Route", route.toString());
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("LocalPath", localPath);
        Weaver.callOriginal();
    }

    @Trace
    public static void files(Route route, File localPath) {
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Route", route.toString());
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("LocalPath", localPath.getAbsolutePath());
        Weaver.callOriginal();
    }

    @Trace
    public static String getStaticBasePackage(Route route) {
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Route", route.toString());
        return Weaver.callOriginal();
    }

    @Trace
    public static void resource(Route route, String remotePath, String resource, String resourcePackage) {
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Route", route.toString());
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("RemotePath", remotePath);
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Resource", resource);
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("ResourcePackage", resourcePackage);
        Weaver.callOriginal();
    }

    @Trace
    public static void resources(Route route, String resourcePackage) {
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Route", route.toString());
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("ResourcePackage", resourcePackage);
        Weaver.callOriginal();
    }

    @Trace
    public static void defaultResource(Route route, String resource, String resourcePackage) {
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Route", route.toString());
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Resource", resource);
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("ResourcePackage", resourcePackage);
        Weaver.callOriginal();
    }
}
