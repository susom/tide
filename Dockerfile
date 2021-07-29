FROM gcr.io/stanford-r/tide:latest AS JAVA_EXECUTER

# RUN mkdir -p /opt/deid
# WORKDIR /opt/deid
# COPY ./ /opt/deid

# RUN mvn clean compile assembly:single

# RUN mv target/tide-0.1.0-SNAPSHOT-jar-with-dependencies.jar /opt/deid/tide.jar

# RUN chmod a+x /opt/deid/tide.jar
# RUN chmod a+x /opt/deid/javacmd.sh
# RUN sh /opt/deid/javacmd.sh

FROM openkbs/jre-mvn-py3:latest AS PYTHON_EXECUTER

RUN sudo mkdir -p /opt/deid
RUN sudo mkdir -p /opt/deid/original
WORKDIR /opt/deid
COPY requirements.txt /opt/deid/requirements.txt
COPY --from=JAVA_EXECUTER /opt/deid/final_output? /opt/deid/final_output
COPY TiDEOutputTransformer.py /opt/deid/transformer.py
RUN sudo mv final_output/1*/* original/
RUN pip install -r requirements.txt

RUN sudo python3 transformer.py
