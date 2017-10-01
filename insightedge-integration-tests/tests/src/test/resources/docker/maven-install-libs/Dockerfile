#
# Runs InsightEdge in a demo mode
#

FROM centos:7.4.1708
MAINTAINER Danylo Hurin danylo@gigaspaces.com

# upgrade system
RUN yum -y update
RUN yum clean all
RUN yum -y install curl wget unzip

# setup user
RUN useradd ie-user -G wheel -s /bin/bash -m
RUN echo "ie-user:ie-user" | chpasswd
RUN echo "%wheel ALL=(ALL) ALL" >> /etc/sudoers

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

RUN alternatives --install /usr/bin/java java $JAVA_HOME/bin/java 1
RUN alternatives --set java $JAVA_HOME/bin/java

# maven
ENV MVN_VERSION=3.3.9
RUN wget http://www.eu.apache.org/dist/maven/maven-3/$MVN_VERSION/binaries/apache-maven-$MVN_VERSION-bin.tar.gz
RUN tar xzf apache-maven-$MVN_VERSION-bin.tar.gz
RUN mkdir /usr/local/maven
RUN mv apache-maven-$MVN_VERSION/ /usr/local/maven/
RUN alternatives --install /usr/bin/mvn mvn /usr/local/maven/apache-maven-$MVN_VERSION/bin/mvn 1

# sbt
# '-1' is for yum
ENV SBT_VERSION "0.13.9-1"
RUN curl https://bintray.com/sbt/rpm/rpm | tee /etc/yum.repos.d/bintray-sbt-rpm.repo
RUN yum -y install sbt-$SBT_VERSION
RUN runuser -l ie-user -c 'sbt -mem 4096 sbtVersion'

# git
RUN yum -y install git
RUN git --version

# add InsightEdge distr
RUN mkdir -p /opt
ADD gigaspaces-insightedge-*.zip /opt/gigaspaces-insightedge.zip
RUN cd /opt \
    && unzip gigaspaces-insightedge.zip \
    && mv gigaspaces-insightedge-* gigaspaces-insightedge

ADD bootstrap.sh /etc/bootstrap.sh
RUN chown root:root /etc/bootstrap.sh
RUN chmod 700 /etc/bootstrap.sh

ADD install-libs-test.sh /home/ie-user/install-libs-test.sh
RUN chmod +x /home/ie-user/install-libs-test.sh
RUN chown ie-user /home/ie-user/install-libs-test.sh

# Keep image running
CMD ["/etc/bootstrap.sh", "-d"]
