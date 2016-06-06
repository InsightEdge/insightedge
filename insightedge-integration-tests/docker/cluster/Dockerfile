#
# A target container for InsightEdge installation
# Starts sshd with a 'pem' auth. for 'ie-user'
#

FROM centos:7.2.1511
MAINTAINER Oleksiy Dyagilev oleksiy.dyagilev@gigaspaces.com

RUN yum -y update
RUN yum clean all
RUN yum install -y initscripts openssh openssh-server openssh-clients sudo passwd sed curl wget unzip telnet net-tools
RUN sshd-keygen
RUN sed -i "s/#UsePrivilegeSeparation.*/UsePrivilegeSeparation no/g" /etc/ssh/sshd_config && sed -i "s/UsePAM.*/UsePAM no/g" /etc/ssh/sshd_config

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


# ssh auth with pem file
USER ie-user

ADD ie-user.pem.pub /home/ie-user/ie-user.pem.pub
#RUN cd /home/ie-user && ssh-keygen -q -N "" -b 1024 -f ie-user.pem -t dsa
RUN mkdir /home/ie-user/.ssh
RUN chmod 700 /home/ie-user/.ssh
RUN cat /home/ie-user/ie-user.pem.pub >> /home/ie-user/.ssh/authorized_keys
RUN chmod 600 /home/ie-user/.ssh/authorized_keys
RUN chown ie-user /home/ie-user/.ssh
RUN chown ie-user /home/ie-user/.ssh/authorized_keys

# add env variables for ie-user
RUN printf "export JAVA_HOME=/usr/jdk1.8.0_31 \nexport PATH=$PATH:$JAVA_HOME/bin" >> /home/ie-user/.bashrc


USER root

# ssh
EXPOSE 22
# spark
EXPOSE 8090 8080 7077
# datagrid
#EXPOSE 10000-10100
EXPOSE 9104
EXPOSE 7102
EXPOSE 4174
EXPOSE 7000-7010
EXPOSE 7055
EXPOSE 2222
EXPOSE 3333
EXPOSE 2181
EXPOSE 3306

CMD    ["/usr/sbin/sshd", "-D"]