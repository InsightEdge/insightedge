#
# Runs InsightEdge in a demo mode
#

FROM centos:7.2.1511
MAINTAINER Kobi Kisos kobi@gigaspaces.com

# upgrade system
RUN yum -y update
RUN yum clean all
RUN yum -y install curl wget unzip
RUN yum -y install net-tools

# java
ENV JAVA_HOME /usr/jdk1.8.0_31
ENV PATH $PATH:$JAVA_HOME/bin
RUN curl -sL --retry 3 --insecure \
  --header "Cookie: oraclelicense=accept-securebackup-cookie;" \
  "http://download.oracle.com/otn-pub/java/jdk/8u31-b13/server-jre-8u31-linux-x64.tar.gz" \
  | gunzip \
  | tar x -C /usr/ \
  && ln -s $JAVA_HOME /usr/java \
  && rm -rf $JAVA_HOME/man

# add InsightEdge distr
RUN mkdir -p /opt/insightedge

RUN mkdir -p /tmp/spark-events


ENV XAP_NIC_ADDRESS "#local:ip#"
ENV XAP_OPTIONS "-Dcom.gs.transport_protocol.lrmi.bind-port=10000-10100 -Dcom.gigaspaces.start.httpPort=9104 -Dcom.gigaspaces.system.registryPort=7102 -Dcom.gs.deploy=/deploy -Dcom.gs.work=/work"

# ssh
EXPOSE 22
# spark
EXPOSE 8090 8080 7077 18080
# datagrid (some might be redundant, not sure)
EXPOSE 9104
EXPOSE 7102
EXPOSE 4174
EXPOSE 7000-7010
EXPOSE 10000-10100