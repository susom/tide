
DEID
=========

Identify PHI values for each patient
---------

The De-identification method here is using PHI value as regular expression search terms to remove phi from note.  Please see the Clarity PHI is this document. https://drive.google.com/open?id=1XftVH6SCqtoRyD2TwvBnlsnzPUI5yFN_ayAdNR0o_7E

The PHI worksheet is for collecting search terms rather than for ruling out columns.


Configure Deid job spec
----

To customize the configuration, create a new config file, and use the file path as argument value of --deidConfigFile when run the utility.

Deid spec can be grouped into PHI categories.

```yaml

name: note_deid_20180831
deid_jobs:
  - job_name: stanford_deid_v1_strict
    version: v1
    spec:
      - item_name: phi_date
        action: jitter
        action_param: 10
        fields: birth_date

      - item_name: other_date
        action: keep
        fields: '*'

      - item_name: patient_mrn
        action: tag
        action_param: mrn
        fields:  PAT_MRN_ID
        .....

```


### set up environment

Configure Google credential file location

```
export GOOGLE_APPLICATION_CREDENTIALS=<gcp service account credential json file>

```


Run Deid jobs
-----

Read from Google Cloud Storage and store result to BigQuery:

```

java -jar db-to-bigquery-1.0-SNAPSHOT.jar \
--deidConfigFile=deid_config_clarity.yaml \
--inputResource=gs://rit-starr-clarity-note-phi-9bbc7c5f \/NOTE_FULL_PHI_PROV_LPCH_test2_1000row.json \
--outputResource=gs://rit-starr-clarity-note-phi-9bbc7c5f/NOTE_FULL_PHI_PROV_LPCH_test2_result

```



Read from local file
```

java -jar db-to-bigquery-1.0-SNAPSHOT.jar \
--deidConfigFile=deid_config_clarity.yaml \
--inputResource=/Users/wenchengli/dev/servers/clarity/lpch \/NOTE_FULL_PHI_PROV_test1_1000row.json
--outputResource=local_test2_result \

```
