FROM centos:7.2.1511
MAINTAINER Oleksiy Dyagilev Oleksiy_Dyagilev@gigaspaces.com

# upgrade system
RUN yum -y update
RUN yum clean all
RUN yum -y install curl wget unzip

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

# download and upack InsightEdge
RUN mkdir -p /opt
ADD gigaspaces-insightedge-*.zip /opt/gigaspaces-insightedge.zip
RUN cd /opt \
    && unzip gigaspaces-insightedge.zip

#RUN mkdir -p /opt \
#    && cd /opt \
#    && wget https://www.dropbox.com/s/4e78nzm9pu7h1kf/gigaspaces-insightedge-0.4.0-SNAPSHOT.zip?dl=1 -O gigaspaces-insightedge-0.4.0-SNAPSHOT.zip \
#    && unzip gigaspaces-insightedge-0.4.0-SNAPSHOT.zip

ADD bootstrap.sh /etc/bootstrap.sh
RUN chown root:root /etc/bootstrap.sh
RUN chmod 700 /etc/bootstrap.sh

# start InsightEdge
CMD ["/etc/bootstrap.sh", "-d"]

EXPOSE 8090
