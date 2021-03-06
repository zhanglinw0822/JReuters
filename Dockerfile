# Pull base image  
FROM 192.168.1.112:5000/centos:6.8
  
MAINTAINER zhanglin "zhanglin@puxtech.com"  
  
# update source  
#RUN echo "deb http://archive.ubuntu.com/ubuntu precise main universe"> /etc/apt/sources.list  
#RUN yum update  

# Install zip  
RUN yum -y install zip

# Install curl  
RUN yum -y install curl  
  
# Install JDK 8  
RUN cd /tmp &&  curl -L 'http://download.oracle.com/otn-pub/java/jdk/8u91-b14/jdk-8u91-linux-x64.tar.gz' -H 'Cookie: oraclelicense=accept-securebackup-cookie; gpw_e24=Dockerfile' | tar -xz  
RUN mkdir -p /usr/lib/jvm  
RUN mv /tmp/jdk1.8.0_91/ /usr/lib/jvm/java-8-oracle/  
  
# Set Oracle JDK 8 as default Java  
RUN update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-8-oracle/bin/java 300     
RUN update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/java-8-oracle/bin/javac 300     
  
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle/  
  
# Install tomcat7  
RUN cd /tmp && curl -L 'http://archive.apache.org/dist/tomcat/tomcat-7/v7.0.8/bin/apache-tomcat-7.0.8.tar.gz' | tar -xz  
RUN mv /tmp/apache-tomcat-7.0.8/ /opt/tomcat7/  
  
ENV CATALINA_HOME /opt/tomcat7  
ENV PATH $PATH:$CATALINA_HOME/bin  
  
#ADD tomcat7.sh /etc/init.d/tomcat7  
#RUN chmod 755 /etc/init.d/tomcat7  

ADD JReuters/ /opt/app/
RUN unzip /opt/app/JReuters*.zip -d /opt/app/JReuters
RUN chmod 755 /opt/app/JReuters/start.sh
