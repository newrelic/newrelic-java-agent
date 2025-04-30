JMETER_VERSION=5.6.3
JMXMON_VERSION=0.3
JPGCGGL_VERSION=2.0
 
mkdir deps
cd deps

# Install jmeter
echo "Intalling JMeter"
curl --output jmeter.tgz https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz
tar -xzf jmeter.tgz
rm jmeter.tgz
mv apache-jmeter-${JMETER_VERSION} jmeter
echo "JMeter installed!"

# Install JMX Plugins

echo "Intalling JMX sampler plugin for JMeter"
curl --output jpgc-jmxmon-${JMXMON_VERSION}.zip https://jmeter-plugins.org/files/packages/jpgc-jmxmon-${JMXMON_VERSION}.zip
unzip jpgc-jmxmon-${JMXMON_VERSION}.zip -d jpgc-jmxmon
mv jpgc-jmxmon/lib/*.jar jmeter/lib
mv jpgc-jmxmon/lib/ext/*.jar jmeter/lib/ext
rm jpgc-jmxmon-${JMXMON_VERSION}.zip
rm -rf jpgc-jmxmon
echo "Intalled JMX sampler plugin for JMeter!"

echo "Intalling Graphs Generator Listener plugin for JMeter"
curl --output jpgc-ggl-${JPGCGGL_VERSION}.zip https://jmeter-plugins.org/files/packages/jpgc-ggl-${JPGCGGL_VERSION}.zip
unzip jpgc-ggl-${JPGCGGL_VERSION}.zip -d jpgc-ggl
mv jpgc-ggl/lib/ext/jmeter-plugins-graphs-ggl-${JPGCGGL_VERSION}.jar jmeter/lib/ext
rm jpgc-ggl-${JPGCGGL_VERSION}.zip
rm -rf jpgc-ggl
echo "Intalled Graphs Generator Listener plugin for JMeter!"