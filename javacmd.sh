# !/bin/sh
java -jar /opt/deid/tide.jar --deidConfigFile=deid_config_omop_genrep.yaml --inputType="txt" --phiFileName=/opt/deid/sample_notes/phi_sample.json --inputResource=/opt/deid/sample_notes --outputResource=/opt/deid/final_output
#--textIdFields="id" --textInputFields="transcription"
#--phiInfoResource=""
#mvn exec:java -Dexec.mainClass="com.github.susom.starr.deid.Main" -Dexec.args="--deidConfigFile=deid_config_omop_genrep.yaml --inputType="txt" --inputResource=./sample_notes --phiFileName=./phi/phi_sample.json --outputResource=local_deid   