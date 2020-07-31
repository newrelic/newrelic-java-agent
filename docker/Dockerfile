FROM centos:7 AS builder

# We use this to do builds in grand central

RUN yum update -y
RUN yum install -y git
RUN yum install -y java-1.7.0-openjdk-devel java-1.8.0-openjdk-devel java-11-openjdk-devel

# Needed for RabbitMQ 3.6.9 testing

RUN yum install -y epel-release wget yum-plugin-versionlock
RUN wget https://packages.erlang-solutions.com/erlang-solutions-1.0-1.noarch.rpm
RUN rpm -Uvh erlang-solutions-1.0-1.noarch.rpm

RUN yum versionlock erlang-19.3-1.el7.centos
RUN yum install -y erlang-19.3-1.el7.centos
