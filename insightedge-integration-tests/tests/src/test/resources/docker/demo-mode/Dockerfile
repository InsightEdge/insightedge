#
# Runs InsightEdge in a demo mode
#

FROM centos:7.2.1511
MAINTAINER Oleksiy Dyagilev oleksiy.dyagilev@gigaspaces.com

# upgrade system
RUN yum -y update
RUN yum clean all
RUN yum -y install curl wget unzip

ENV HERCULES_IP 192.168.10.50

# java
ENV ZIPPED_JDK=jdk-8u131-linux-x64.tar.gz
ENV TAR_JDK=jdk-8u131-linux-x64.tar

ENV JAVA_TARGET=jdk1.8.0_131
ENV JAVA_HOME /usr/$JAVA_TARGET
ENV PATH $PATH:$JAVA_HOME/bin

RUN wget http://$HERCULES_IP/javas/$ZIPPED_JDK
RUN gunzip $ZIPPED_JDK && tar -xvf $TAR_JDK -C /usr/
RUN ln -s $JAVA_HOME /usr/java && rm -rf $JAVA_HOME/man

# add InsightEdge distr
RUN mkdir -p /opt/gigaspaces-insightedge
RUN mkdir -p /tmp/spark-events

ADD bootstrap.sh /etc/bootstrap.sh
RUN chown root:root /etc/bootstrap.sh
RUN chmod 700 /etc/bootstrap.sh

# start InsightEdge
CMD ["/etc/bootstrap.sh", "-d"]

EXPOSE 8090
