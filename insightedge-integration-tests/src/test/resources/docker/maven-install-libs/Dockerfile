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
#RUN useradd ie-user -G wheel -s /bin/bash -m
#RUN echo "ie-user:ie-user" | chpasswd
#RUN echo "%wheel ALL=(ALL) ALL" >> /etc/sudoers

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

RUN printf "export JAVA_HOME=/usr/jdk1.8.0_31 \n" >> /root/.bashrc
RUN printf "export PATH=$PATH:$JAVA_HOME/bin \n" >> /root/.bashrc
#RUN printf "export JAVA_HOME=/usr/jdk1.8.0_31 \n" >> /home/ie-user/.bashrc
#RUN printf "export PATH=$PATH:$JAVA_HOME/bin \n" >> /home/ie-user/.bashrc

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

# scala
#ENV SCALA_VERSION=2.11.8
ENV SCALA_VERSION 2.10.5 # for sbt
#RUN wget http://www.scala-lang.org/files/archive/scala-$SCALA_VERSION.tgz
#RUN tar xvf scala-$SCALA_VERSION.tgz
#RUN mv scala-$SCALA_VERSION /usr/lib
#RUN ln -s /usr/lib/scala-$SCALA_VERSION /usr/lib/scala
#ENV SCALA_HOME /usr/lib/scala
#ENV PATH $PATH:$SCALA_HOME/bin
#RUN printf "export SCALA_HOME=/usr/lib/scala" >> /home/ie-user/.bashrc
#RUN printf "export PATH=$PATH:$SCALA_HOME/bin" >> /home/ie-user/.bashrc
RUN wget http://www.scala-lang.org/files/archive/scala-2.10.5.tgz
RUN tar xvf scala-2.10.5.tgz
RUN mv scala-2.10.5 /usr/lib
RUN ln -s /usr/lib/scala-2.10.5 /usr/lib/scala
ENV SCALA_HOME /usr/lib/scala
ENV PATH $PATH:/usr/lib/scala/bin
RUN printf "export SCALA_HOME=/usr/lib/scala" >> /root/.bashrc
RUN printf "export PATH=$PATH:$SCALA_HOME/bin" >> /root/.bashrc
#RUN printf "export SCALA_HOME=/usr/lib/scala" >> /home/ie-user/.bashrc
#RUN printf "export PATH=$PATH:$SCALA_HOME/bin" >> /home/ie-user/.bashrc


#RUN rm scala-$SCALA_VERSION.tgz

# sbt
ENV SBT_VERSION 0.13.9-1 # '-1' is YUM suffix
ENV SBT_OPTS "-Xms512M -Xmx4096M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512M"
RUN printf "export SBT_OPTS='-Xms512M -Xmx4096M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512M'" >> /root/.bashrc
#RUN printf "export SBT_OPTS='-Xms512M -Xmx4096M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512M'" > /home/ie-user/.bashrc
RUN curl https://bintray.com/sbt/rpm/rpm | tee /etc/yum.repos.d/bintray-sbt-rpm.repo
#RUN yum -y install sbt-$SBT_VERSION
RUN yum -y install sbt-0.13.9-1
#RUN su ie-user -c 'sbt -mem 4000 sbtVersion'
RUN sbt -mem 4000 sbtVersion

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

#ADD maven-install-libs-test.sh /home/ie-user/maven-install-libs-test.sh
#RUN chmod +x /home/ie-user/maven-install-libs-test.sh
#RUN chown ie-user /home/ie-user/maven-install-libs-test.sh

ADD maven-install-libs-test.sh /etc/maven-install-libs-test.sh
RUN chmod +x /etc/maven-install-libs-test.sh
RUN chown root:root /etc/maven-install-libs-test.sh


# Keep image running
CMD ["/etc/bootstrap.sh", "-d"]
