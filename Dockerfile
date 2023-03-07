FROM maven:3.9.0-eclipse-temurin-17-focal
# Do not replace Maven with a generic JRE or JDK since TiDE can be called by Maven

RUN apt update && apt -y install zip unzip curl
RUN mkdir /opt/deid
WORKDIR /opt/deid
COPY ./ /opt/deid

RUN mvn clean install -DskipTests
RUN mv target/deid-*dataflow.jar /opt/deid/deid.jar

CMD [ "java","-jar","/opt/deid/deid.jar" ]
