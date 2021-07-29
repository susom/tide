FROM gcr.io/stanford-r/tide:latest AS JAVA_EXECUTER

FROM openkbs/jre-mvn-py3:latest AS PYTHON_EXECUTER

RUN sudo mkdir -p /opt/deid
WORKDIR /opt/deid
COPY requirements.txt /opt/deid/requirements.txt
COPY --from=JAVA_EXECUTER /opt/deid/Result_output /opt/deid/final_output
COPY TiDEOutputTransformer.py /opt/deid/transformer.py
RUN pip install -r requirements.txt

RUN sudo python3 transformer.py
