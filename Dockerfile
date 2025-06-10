FROM maven:3.9-eclipse-temurin-17-focal
# Do not replace Maven with a generic JRE or JDK since TiDE can be called by Maven

RUN apt-get update || (apt-get clean && apt-get update) && \
    apt-get install -y zip unzip curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
    
RUN mkdir /opt/deid
WORKDIR /opt/deid
COPY ./ /opt/deid

RUN mvn clean install -DskipTests
#RUN mv target/deid-3.0.30-SNAPSHOT-jar-with-dependencies.jar /opt/deid/deid.jar

#CMD [ "java","-jar","/opt/deid/deid.jar" ]
ENTRYPOINT [ "/bin/sh" ]