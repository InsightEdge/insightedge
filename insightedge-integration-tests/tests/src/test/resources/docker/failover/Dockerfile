#
# Runs InsightEdge in a demo mode
#

FROM centos:7.2.1511
MAINTAINER Kobi Kisos kobi@gigaspaces.com

ARG STORAGE_SERVER="imc-srv01"
RUN echo "Using STORAGE_SERVER=${STORAGE_SERVER}"
RUN if [[ "${STORAGE_SERVER}" == "" ]]; then echo "STORAGE_SERVER can't be empty"; exit 1; fi

# upgrade system
RUN yum -y update
RUN yum clean all
RUN yum -y install curl wget unzip
RUN yum -y install net-tools

# java
ENV ZIPPED_JDK=jdk-8u131-linux-x64.tar.gz
ENV TAR_JDK=jdk-8u131-linux-x64.tar

ENV JAVA_TARGET=jdk1.8.0_131
ENV JAVA_HOME /usr/$JAVA_TARGET
ENV PATH $PATH:$JAVA_HOME/bin

RUN wget http://${STORAGE_SERVER}/javas/$ZIPPED_JDK
RUN gunzip $ZIPPED_JDK && tar -xvf $TAR_JDK -C /usr/
RUN ln -s $JAVA_HOME /usr/java && rm -rf $JAVA_HOME/man

# add InsightEdge distr
RUN mkdir -p /opt/insightedge

RUN mkdir -p /tmp/spark-events
ENV EXT_JAVA_OPTIONS "-Dcom.gs.transport_protocol.lrmi.bind-port=10000-10100 -Dcom.gigaspaces.start.httpPort=9104 -Dcom.gigaspaces.system.registryPort=7102 -Dcom.gs.deploy=/deploy -Dcom.gs.work=/work"

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
