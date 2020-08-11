package com.newrelic.agent.autoname;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.Agent;
import com.newrelic.agent.config.EnvironmentFacade;
import com.newrelic.agent.discovery.AgentArguments;
import com.newrelic.bootstrap.BootstrapAgent;

public class ApplicationAutoName {
    private static final String MARATHON_APP_DOCKER_IMAGE = "MARATHON_APP_DOCKER_IMAGE";

    /**
     * May be null
     */
    private final String name;

    private ApplicationAutoName(String name) {
        this.name = name;
    }

    public String getName(EnvironmentFacade environment) {
        if (name == null) {
            final String commandLine = 
                    environment.getenv(AgentArguments.NEW_RELIC_COMMAND_LINE_ENV_VARIABLE);
            if (commandLine != null) {
                // fall back to naming with the command line as seen through the attach tool
                return commandLine;
            }
        }
        return name;
    }

    public boolean enableAutoAppNaming() {
        return false;
    }

    @Override
    public String toString() {
        return "ApplicationServerDetails [name=" + name + "]";
    }

    private static ApplicationAutoName createApplicationAutoName(AppServer appServer,
            Set<ServletContextDetails> contexts) {
        String appName = contexts.size() == 1 ?
            contexts.iterator().next().getDisplayName() :
            appServer.getName(contexts);

        return new AppServerApplicationAutoName(appServer.name() + " : " + appName, contexts);
    }

    private static class AppServerApplicationAutoName extends ApplicationAutoName {
        private final Set<ServletContextDetails> servletContexts;

        private AppServerApplicationAutoName(String name, Set<ServletContextDetails> servletContexts) {
            super(name);
            this.servletContexts = servletContexts;
        }

        @Override
        public boolean enableAutoAppNaming() {
            return servletContexts.size() > 1;
        }
    }

    enum AppServer {
        None {
            @Override
            String getName(Set<ServletContextDetails> contexts) {
                return null;
            }

            @Override
            ApplicationAutoName getApplicationServerDetails(MBeanServer mbeanServer) {
                return null;
            }
            
        },
        Tomcat {
            private static final String EXAMPLES_APP = "examples";

            @Override
            String getName(Set<ServletContextDetails> tomcatContexts) {
                final Set<String> defaultTomcatApps =
                        ImmutableSet.of("docs", "ROOT", EXAMPLES_APP, "manager", "host-manager");

                ServletContextDetails examples = null;
                List<ServletContextDetails> filtered = new ArrayList<>();
                for (ServletContextDetails sc : tomcatContexts) {
                    if (!defaultTomcatApps.contains(sc.getBaseName())) {
                        filtered.add(sc);
                    } else if (defaultTomcatApps.contains(sc.getBaseName())) {
                        examples = sc;
                    }
                }
                if (filtered.isEmpty() && examples != null) {
                    return examples.getDisplayName();
                }
                if (filtered.size() == 1) {
                    return filtered.iterator().next().getDisplayName();
                }
                return null;
            }

            @Override
            ApplicationAutoName getApplicationServerDetails(MBeanServer mbeanServer) {
                return ApplicationAutoName.getApplicationServerDetails(mbeanServer, this, 
                        "Catalina:j2eeType=WebModule,name=//*/*,J2EEApplication=*,J2EEServer=*",
                        "baseName", "displayName");
            }
        },
        JBoss {
            @Override
            ApplicationAutoName getApplicationServerDetails(MBeanServer mbeanServer) {
                return ApplicationAutoName.getApplicationServerDetails(mbeanServer, this, 
                        "jboss.as:deployment=*",
                        "name", "name");
            }
        },
        /**
         * JMX must be enabled.
         * java -jar start.jar --add-to-start=jmx
         */
        Jetty {

            @Override
            ApplicationAutoName getApplicationServerDetails(MBeanServer mbeanServer) {
                return ApplicationAutoName.getApplicationServerDetails(mbeanServer, this, 
                        "org.eclipse.jetty.webapp:context=*,type=webappcontext,*",
                        "contextPath", "displayName");
            }
        },
        WebSphereLiberty {
            @Override
            ApplicationAutoName getApplicationServerDetails(MBeanServer mbeanServer) {
                try {
                    String nameQuery = "WebSphere:service=com.ibm.websphere.application.ApplicationMBean,name=*";
                    Set<ObjectName> results = mbeanServer.queryNames(new ObjectName(nameQuery), null);
                    Agent.LOG.log(Level.INFO, "{0} JMX: {1}", this.name(), results);
                    if (!results.isEmpty()) {
                        final Set<ServletContextDetails> contexts = new HashSet<>();
                        for (ObjectName name : results) {
                            String appName = name.getKeyProperty("name");
                            contexts.add(new ServletContextDetails(appName, appName));
                        }
                        return createApplicationAutoName(this, contexts);
                    }
                } catch (MalformedObjectNameException e) {
                    // ignore
                }
                return null;
            }
        },
        WebLogic {
            @Override
            ApplicationAutoName getApplicationServerDetails(MBeanServer mbeanServer) {
                return null;
            }
        };

        String getName(Set<ServletContextDetails> contexts) {
            return this.name();
        }

        abstract ApplicationAutoName getApplicationServerDetails(MBeanServer mbeanServer);
    }

    private static ApplicationAutoName getApplicationServerDetails(
            MBeanServer mbeanServer,
            AppServer appServer,
            String nameQuery, String baseAttributeName, String displayNameAttributeName) {
        try {
            Set<ObjectName> results = mbeanServer.queryNames(new ObjectName(nameQuery), null);
            Agent.LOG.log(Level.INFO, "{0} JMX: {1}", appServer.name(), results);
            if (!results.isEmpty()) {
                final Set<ServletContextDetails> contexts = new HashSet<>();
                for (ObjectName name : results) {
                    contexts.add(new ServletContextDetails(
                            (String) mbeanServer.getAttribute(name, baseAttributeName),
                            (String) mbeanServer.getAttribute(name, displayNameAttributeName)));
                }
                return createApplicationAutoName(appServer, contexts);
            }
        } catch (MalformedObjectNameException | InstanceNotFoundException | AttributeNotFoundException | ReflectionException | MBeanException e) {
            // ignore
        }
        return null;
    }

    public static boolean isAgentAttached() {
        return System.getProperty(BootstrapAgent.NR_AGENT_ARGS_SYSTEM_PROPERTY) != null;
    }

    public static ApplicationAutoName getApplicationAutoName(EnvironmentFacade environmentFacade) {
        
        if (isAgentAttached()) {
            // if the app is explicitly named from the attach tool, use that name
            final String appNameOverride =
                    environmentFacade.getenv(AgentArguments.NEW_RELIC_APP_NAME_ENV_VARIABLE);
            if (appNameOverride != null) {
                return new ApplicationAutoName(appNameOverride);
            }

            // favor the docker image name
            String imageName = environmentFacade.getenv(MARATHON_APP_DOCKER_IMAGE);
            if (imageName != null) {
                int index = imageName.indexOf(':');
                return new ApplicationAutoName(
                        index > 0 ? imageName.substring(0, index) : imageName);
            }

            // try to find app server info
            final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
            for (AppServer server : AppServer.values()) {
                ApplicationAutoName details = server.getApplicationServerDetails(mbeanServer);
                if (details != null) {
                    return details;
                }
            }
        }
        return new ApplicationAutoName(null);
    }

    static class ServletContextDetails {
        private final String baseName;
        private final String displayName;

        ServletContextDetails(String baseName, String displayName) {
            this.baseName = baseName;
            this.displayName = displayName == null ? baseName : displayName;
        }

        public String getBaseName() {
            return baseName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return "ServletContextDetails [baseName=" + baseName + ", displayName=" + displayName + "]";
        }
    }
}
