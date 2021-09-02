FROM alpine:3.14

RUN  apk update \
  && apk upgrade \
  && apk add ca-certificates \
  && update-ca-certificates \
  && apk add --update coreutils && rm -rf /var/cache/apk/*   \ 
  && apk add --update openjdk11 tzdata curl unzip bash \
  && apk add --no-cache nss \
  && rm -rf /var/cache/apk/*

RUN  apk add maven
RUN export PATH=${PATH}:${JAVA_HOME}/bin

RUN apk add libc6-compat
RUN ln -s /lib/libc.musl-x86_64.so.1 /lib/ld-linux-x86-64.so.2

RUN mkdir /opt/deid
WORKDIR /opt/deid
COPY ./ /opt/deid

RUN mvn clean install -DskipTests
#RUN mv target/deid-3.0.21-SNAPSHOT-jar-with-dependencies.jar /opt/deid/deid.jar

#CMD [ "java","-jar","/opt/deid/deid.jar" ]
ENTRYPOINT [ "/bin/sh" ]