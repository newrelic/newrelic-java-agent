JMETER_VERSION=5.6.3
JMXMON_VERSION=0.3
 
mkdir deps
cd deps

# Install jmeter
echo "Intalling JMeter"
curl --output jmeter.tgz https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz
tar -xzf jmeter.tgz
rm jmeter.tgz
mv apache-jmeter-${JMETER_VERSION} jmeter
echo "JMeter installed!"

# Install JMX Plugin

echo "Intalling JMX sampler plugin for JMeter"
curl --output jpgc-jmxmon-${JMXMON_VERSION}.zip https://jmeter-plugins.org/files/packages/jpgc-jmxmon-${JMXMON_VERSION}.zip
unzip jpgc-jmxmon-${JMXMON_VERSION}.zip -d jpgc-jmxmon
mv jpgc-jmxmon/lib/*.jar jmeter/lib
mv jpgc-jmxmon/lib/ext/*.jar jmeter/lib/ext

rm jpgc-jmxmon-${JMXMON_VERSION}.zip
rm -rf jpgc-jmxmon
echo "Intalled JMX sampler plugin for JMeter!"