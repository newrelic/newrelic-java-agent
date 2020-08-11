FROM centos:7

RUN yum update -y
RUN yum install -y git java-1.8.0-openjdk-devel

ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.252.b09-2.el7_8.x86_64

WORKDIR /root

RUN git clone https://github.com/spring-projects/spring-petclinic.git
WORKDIR /root/spring-petclinic
RUN ./mvnw package

CMD ${JAVA_HOME}/bin/java -Djava.security.egd=file:/dev/./urandom -jar target/*.jar
