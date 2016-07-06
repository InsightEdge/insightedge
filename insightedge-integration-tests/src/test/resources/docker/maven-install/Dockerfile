#
# Runs InsightEdge in a demo mode
#

FROM centos:7.2.1511
MAINTAINER Danylo Hurin danylo@gigaspaces.com

# upgrade system
RUN yum -y update
RUN yum clean all
RUN yum -y install curl wget unzip

# setup user
RUN useradd ie-user -G wheel -s /bin/bash -m
RUN echo "ie-user:ie-user" | chpasswd
RUN echo "%wheel ALL=(ALL) ALL" >> /etc/sudoers

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

RUN printf "export JAVA_HOME=/usr/jdk1.8.0_31 \n" >> /home/ie-user/.bashrc
RUN printf "export PATH=$PATH:$JAVA_HOME/bin \n" >> /home/ie-user/.bashrc

# maven
#RUN wget http://mirror.cc.columbia.edu/pub/software/apache/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz
#RUN tar xzf apache-maven-3.3.9-bin.tar.gz -C /usr/local
#RUN cd /usr/local
#RUN ln -s apache-maven-3.3.9 maven
#
#RUN printf "export M2_HOME=/usr/local/maven \n" >> /home/ie-user/.bashrc
#RUN printf "export PATH=$PATH:$M2_HOME/bin \n" >> /home/ie-user/.bashrc
#ENV M2_HOME=/usr/local/maven
#ENV PATH=$PATH:$M2_HOME/bin
RUN yum -y install maven
RUN mvn --version

# sbt
RUN curl https://bintray.com/sbt/rpm/rpm | tee /etc/yum.repos.d/bintray-sbt-rpm.repo
RUN yum -y install sbt

# git
RUN yum -y install git
RUN git --version
#RUN cd /home/ie-user
#RUN git clone https://github.com/InsightEdge/insightedge-examples.git

# add InsightEdge distr
RUN mkdir -p /opt
ADD gigaspaces-insightedge-*.zip /opt/gigaspaces-insightedge.zip
RUN cd /opt \
    && unzip gigaspaces-insightedge.zip \
    && mv gigaspaces-insightedge-* gigaspaces-insightedge

ADD bootstrap.sh /etc/bootstrap.sh
RUN chown root:root /etc/bootstrap.sh
RUN chmod 700 /etc/bootstrap.sh

ADD maven-test.sh /home/ie-user/maven-test.sh
RUN chmod +x /home/ie-user/maven-test.sh
RUN chown ie-user /home/ie-user/maven-test.sh

# Keep image running
CMD ["/etc/bootstrap.sh", "-d"]
