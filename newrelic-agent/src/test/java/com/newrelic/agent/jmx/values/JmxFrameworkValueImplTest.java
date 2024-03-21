package com.newrelic.agent.jmx.values;

import com.newrelic.agent.jmx.metrics.JMXMetricType;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class JmxFrameworkValueImplTest {
    private static List<JmxFrameworkMetricsClassTestAttributes> classesUnderTest = new ArrayList<>();

    // Loop through all implementations of the JmxFrameworkValue class and assert on the associated class specific values
    // (Defined in the static block below)
    @Test
    public void getFrameworkMetrics_returnsBuiltInMetricsFromConstructorCall()
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        for (JmxFrameworkMetricsClassTestAttributes testAttribute : classesUnderTest) {
            Constructor<?> noArgConstructor = testAttribute.clazz.getConstructor();
            Object obj = noArgConstructor.newInstance();
            JmxFrameworkValues jmxFrameworkValues = (JmxFrameworkValues) obj;

            Assert.assertEquals(testAttribute.prefix, jmxFrameworkValues.getPrefix());
            //The arrays for objectNames, metricNames and types are all the same length
            for (int idx = 0; idx < testAttribute.objectNames.length; idx++) {
                Assert.assertEquals(testAttribute.objectNames[idx], jmxFrameworkValues.getFrameworkMetrics().get(idx).getObjectNameString());
                Assert.assertEquals(testAttribute.metricNames[idx], jmxFrameworkValues.getFrameworkMetrics().get(idx).getObjectMetricName());
                Assert.assertEquals(testAttribute.types[idx], jmxFrameworkValues.getFrameworkMetrics().get(idx).getType());
            }
        }
    }

    static {
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(EmbeddedTomcatDataSourceJmxValues.class, "org.apache.tomcat.jdbc.pool.jmx",
                new String[] { "org.apache.tomcat.jdbc.pool.jmx:name=*,type=ConnectionPool" },
                new String[] { "JmxBuiltIn/DataSources/{name}/" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(EmbeddedTomcatJmxValues.class, "Tomcat",
                new String[] { "*:type=Manager,context=*,host=*,*", "*:type=ThreadPool,name=*" },
                new String[] { "JmxBuiltIn/Session/{context}/", "JmxBuiltIn/ThreadPool/{name}/" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(GlassfishJmxValues.class, "amx",
                new String[] { "amx:type=thread-pool-mon,pp=*,name=*", "amx:type=session-mon,pp=*,name=*", "amx:type=transaction-service-mon,pp=*,name=*" },
                new String[] { "JmxBuiltIn/ThreadPool/{name}/", "JmxBuiltIn/Session/{name}/", "JmxBuiltIn/Transactions/" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(JavaLangJmxMetrics.class, "java.lang",
                new String[] { "java.lang:type=Threading", "java.lang:type=ClassLoading" },
                new String[] { "JmxBuiltIn/Threads/", "JmxBuiltIn/Classes/" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(Jboss7UpJmxValues.class, "jboss.as",
                new String[] { "jboss.as:subsystem=transactions" },
                new String[] { "JmxBuiltIn/Transactions/" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(JettyJmxMetrics.class, "org.eclipse.jetty",
                new String[] { "org.eclipse.jetty.util.thread:type=queuedthreadpool,id=*" },
                new String[] { "JmxBuiltIn/ThreadPool/{id}/" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(KafkaConsumerJmxValues.class, "kafka.consumer",
                new String[] { "\"kafka.consumer\":type=\"ConsumerTopicMetrics\",name=*" },
                new String[] { "JMX/{name}/" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(KafkaProducerJmxValues.class, "kafka.producer",
                new String[] { "\"kafka.producer\":type=\"ProducerTopicMetrics\",name=*" },
                new String[] { "JMX/{name}/" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(ResinJmxValues.class, "resin",
                new String[] { "resin:type=SessionManager,*", "resin:type=ConnectionPool,*", "resin:type=ThreadPool", "resin:type=TransactionManager" },
                new String[] { "JmxBuiltIn/Session/{WebApp}/", "JmxBuiltIn/DataSources/{name}/", "JmxBuiltIn/ThreadPool/Resin/", "JmxBuiltIn/Transactions/" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(LegacySolr7JmxValues.class, "solr7",
                new String[] { "solr:dom1=core,dom2=*,category=CACHE,scope=searcher,name=queryResultCache", "solr:dom1=core,dom2=*,category=CACHE,scope=searcher,name=filterCache",
                        "solr:dom1=core,dom2=*,category=CACHE,scope=searcher,name=documentCache", "solr:dom1=core,dom2=*,category=UPDATE,scope=updateHandler,name=*" },
                new String[] { "JMX/solr/{dom2}/queryResultCache/%/", "JMX/solr/{dom2}/filterCache/%/",
                        "JMX/solr/{dom2}/documentCache/%/", "JMX/solr/{dom2}/updateHandler/%/{name}" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(Solr7JmxValues.class, "solr7",
                new String[] { "solr:dom1=core,*,category=CACHE,scope=searcher,name=queryResultCache", "solr:dom1=core,*,category=CACHE,scope=searcher,name=filterCache",
                        "solr:dom1=core,*,category=CACHE,scope=searcher,name=documentCache", "solr:dom1=core,*,category=UPDATE,scope=updateHandler,name=*" },
                new String[] { "JMX/solr/{for:dom[2:]}/queryResultCache/%/", "JMX/solr/{for:dom[2:]}/filterCache/%/",
                        "JMX/solr/{for:dom[2:]}/documentCache/%/", "JMX/solr/{for:dom[2:]}/updateHandler/%/{name}" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(SolrJmxValues.class, "solr",
                new String[] { "solr*:type=queryResultCache,*", "solr*:type=filterCache,*", "solr*:type=documentCache,*", "solr*:type=updateHandler,*" },
                new String[] { null, null, null, null },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(TomcatJmxValues.class, "Catalina",
                new String[] { "*:type=Manager,context=*,host=*,*", "*:type=Manager,path=*,host=*", "*:type=ThreadPool,name=*", "*:type=DataSource,context=*,host=*,class=javax.sql.DataSource,name=*" },
                new String[] { "JmxBuiltIn/Session/{context}/", "JmxBuiltIn/Session/{path}/", "JmxBuiltIn/ThreadPool/{name}/", "JmxBuiltIn/DataSources/{name}/" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(WeblogicJmxValues.class, "com.bea",
                new String[] { "com.bea:ServerRuntime=*,Name=ThreadPoolRuntime,Type=ThreadPoolRuntime", "com.bea:ServerRuntime=*,Name=*,Type=JDBCDataSourceRuntime",
                        "com.bea:ServerRuntime=*,Name=*,Type=JDBCOracleDataSourceRuntime", "com.bea:ServerRuntime=*,Name=*,ApplicationRuntime=*,Type=EJBPoolRuntime,EJBComponentRuntime=*,*",
                        "com.bea:ServerRuntime=*,Name=*,ApplicationRuntime=*,Type=EJBTransactionRuntime,EJBComponentRuntime=*,*",
                        "com.bea:ServerRuntime=*,Name=*,ApplicationRuntime=*,Type=EJBTransactionRuntime,EJBComponentRuntime=*,*",
                        "com.bea:ServerRuntime=*,Name=*,ApplicationRuntime=*,Type=EJBTransactionRuntime,EJBComponentRuntime=*,*",
                        "com.bea:ServerRuntime=*,Name=JTARuntime,Type=JTARuntime" },
                new String[] { "JmxBuiltIn/ThreadPool/{Name}/", "JmxBuiltIn/DataSources/{Name}/", "JmxBuiltIn/DataSources/{Name}/",
                        "JmxBuiltIn/EJB/Pool/Bean/{ApplicationRuntime}/{EJBComponentRuntime}/{Name}/", "JmxBuiltIn/EJB/Transactions/Application/{ApplicationRuntime}/",
                        "JmxBuiltIn/EJB/Transactions/Module/{ApplicationRuntime}/{EJBComponentRuntime}/",
                        "JmxBuiltIn/EJB/Transactions/Bean/{ApplicationRuntime}/{EJBComponentRuntime}/{Name}/", "JmxBuiltIn/JTA/{Name}/" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN,
                        JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.SUM_ALL_BEANS, JMXMetricType.SUM_ALL_BEANS,
                        JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(WebSphere7JmxValues.class, "WebSphere-7",
                new String[] { "WebSphere:type=ThreadPool,name=*,process=*,platform=*,node=*,*", "WebSphere:j2eeType=JTAResource,type=TransactionService,name=*,process=*,platform=*,node=*,*" },
                new String[] { "JmxBuiltIn/ThreadPool/{name}/", "JmxBuiltIn/JTA/{type}/" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(WebSphereJmxValues.class, "WebSphere",
                new String[] { "WebSphere:type=ThreadPool,name=*,process=*,platform=*,node=*,*", "WebSphere:j2eeType=JTAResource,type=TransactionService,name=*,process=*,platform=*,node=*,*",
                        "WebSphere:type=SessionManager,name=*,process=*,platform=*,node=*,*" },
                new String[] { "JmxBuiltIn/ThreadPool/{name}/", "JmxBuiltIn/JTA/{type}/", "JmxBuiltIn/Session/{name}/" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
        classesUnderTest.add(new JmxFrameworkMetricsClassTestAttributes(WebsphereLibertyJmxValues.class, "liberty",
                new String[] { "WebSphere:type=SessionStats,name=*", "WebSphere:type=ThreadPoolStats,name=*", "WebSphere:type=ConnectionPoolStats,name=*" },
                new String[] { "JmxBuiltIn/Session/{name}/", "JmxBuiltIn/ThreadPool/{name}/", "JmxBuiltIn/DataSources/{name}/" },
                new JMXMetricType[] { JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN, JMXMetricType.INCREMENT_COUNT_PER_BEAN }));
    }

    public static class JmxFrameworkMetricsClassTestAttributes {
        public Class clazz;
        public String prefix;
        public String [] objectNames;
        public String [] metricNames;
        public JMXMetricType [] types;

        public JmxFrameworkMetricsClassTestAttributes(Class clazz, String prefix, String [] objectNames, String [] metricNames, JMXMetricType [] types) {
            this.clazz = clazz;
            this.prefix = prefix;
            this.objectNames = objectNames;
            this.metricNames = metricNames;
            this.types = types;
        }
    }
}
