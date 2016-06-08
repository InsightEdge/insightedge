#
# A cluster node container for InsightEdge installation
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

ADD ie-user.pem.pub /home/ie-user/ie-user.pem.pub
ADD ie-user.pem /home/ie-user/ie-user.pem
RUN mkdir /home/ie-user/.ssh
RUN chmod 700 /home/ie-user/.ssh
RUN cat /home/ie-user/ie-user.pem.pub >> /home/ie-user/.ssh/authorized_keys
RUN chmod 600 /home/ie-user/.ssh/authorized_keys
RUN chown ie-user /home/ie-user/.ssh
RUN chown ie-user /home/ie-user/.ssh/authorized_keys
RUN chown ie-user /home/ie-user/ie-user.pem

# add env variables for ie-user
RUN printf "export JAVA_HOME=/usr/jdk1.8.0_31 \n" >> /home/ie-user/.bashrc
RUN printf "export PATH=$PATH:$JAVA_HOME/bin \n" >> /home/ie-user/.bashrc
RUN printf 'export XAP_NIC_ADDRESS="#local:ip#" \n' >> /home/ie-user/.bashrc
RUN printf 'export NIC_ADDR="#local:ip#" \n' >> /home/ie-user/.bashrc

# Script to be run by 'client' container
ADD remote_install.sh /home/ie-user/remote_install.sh
RUN chmod +x /home/ie-user/remote_install.sh
RUN chown ie-user /home/ie-user/remote_install.sh

# ssh
EXPOSE 22
# spark
EXPOSE 8090 8080 7077
# datagrid (some might be redundant, not sure)
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