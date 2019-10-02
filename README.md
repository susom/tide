# TiDE 


## PHI and InfoTypes

This deid tool try to identify and handles Safe Harbor 18 identifiers

      Name, Address, dates,phone,fax,Email,SSN,MRN,Health plan beneficiary number,Account number,Certificate or license number,vehicle number,URL,IP,Finger/Voice print,photo,Any other characteristic that could uniquely identify the individual


Optionally Google DLP API identify the following DLP infoTypes: 

    AGE,DATE,DATE_OF_BIRTH,CREDIT_CARD_NUMBER,US_BANK_ROUTING_MICR,AMERICAN_BANKERS_CUSIP_ID,IBAN_CODE,US_ADOPTION_TAXPAYER_IDENTIFICATION_NUMBER,US_DRIVERS_LICENSE_NUMBER,US_INDIVIDUAL_TAXPAYER_IDENTIFICATION_NUMBER,US_PREPARER_TAXPAYER_IDENTIFICATION_NUMBER,US_PASSPORT,US_SOCIAL_SECURITY_NUMBER,US_EMPLOYER_IDENTIFICATION_NUMBER,US_VEHICLE_IDENTIFICATION_NUMBER,EMAIL_ADDRESS,PERSON_NAME,PHONE_NUMBER,US_HEALTHCARE_NPI,US_DEA_NUMBER,LOCATION,IP_ADDRESS,MAC_ADDRESS,URL



## Prepare data to be deid-ed

* Step one: 

First join both text table and knwon phi table to get data table with both text and known phi in each row. Please refer to the scripts under dev-ops which contains queries to prepare input data for note, impression, narrative, flowsheet etc. 

* Step two:

Export table to Google Cloud Storage bucket as newline delimited JSON file. 


### Modification options: 
 
* Masking (everything except name and location)
* Jittering (date, age) with provided jitter value
* Surrogate name and location
* General Replacement with common patterns of each type of PHIs



### TiDE identify PHIs in three ways. 

1. General pattern matching with regex searches for known patterns. For example url, email, date and US address.
2. Based on known PHI, TiDE removed them from the text.
3. use NLP or DLP tools to find entities such as name, location
 

###  Configure Deid job spec

To customize the configuration, create a new config file, and use the file path as argument value of --deidConfigFile when run the utility.

Deid spec can be grouped into PHI categories. 

Configure General Regex pattern matching, or find known PHI of the patient associated with the text. 

- general : for Phone/Fax, Email, URL, IP address, SSN
- general_number: general order and account number
- surrogate_name : surrogate name using NameSurrogate
- surrogate_address : surrogate address using LocationSurrogate
- jitter_date_from_field : Date Anonymizer with jitter value provided in an input field
- jitter_birth_date : Date Anonymizer
- jitter_date_randomly: randomly generate jitter using hash function
- remove_age : Age Anonymizer
- remove_mrn : Mrn Anonymizer
- replace_minimumlengthword_with : find words with minimum word length
- replace_with : find word longer than 2 characters, and not in common vocabulary
- replace_strictly_with : applied strictly regardless word length, and if in common vocabulary


```yaml

name: note_deid_20180831
deidJobs:
  - jobName: stanford_deid_v1_strict
    version: v1
    spec:
      - itemName: phi_date
        action: jitter
        actionParam: 10
        fields: birth_date

      - itemName: other_date
        action: keep
        fields: '*'

      - itemName: patient_mrn
        action: tag
        actionParam: mrn
        fields:  PAT_MRN_ID
        .....

```


### Set up environment

Configure Google credential file location

```
export GOOGLE_APPLICATION_CREDENTIALS=<gcp service account credential json file>

```


### Run Deid jobs


Deid jobs can be run either with Maven or Java jar.

Read from Google Cloud Storage and store result to BigQuery:

```
mvn -Pdataflow-runner compile exec:java -Dexec.mainClass=com.github.susom.starr.deid.Main \
-Dexec.args="--project=<Google Project ID> \
--dlpProject=<Google Project ID for DLP API calls> \
--serviceAccount=<Service Account> \
--stagingLocation=gs://<dataflow staging bucket>/staging \
--gcpTempLocation=gs://<dataflow staging bucket>/temp \
--tempLocation=gs://<dataflow staging bucket>/temp \
--region=us-west1 --workerMachineType=n1-standard-8 --maxNumWorkers=20 --diskSizeGb=100 \
--runner=DataflowRunner \
--deidConfigFile=deid_config_clarity.yaml --inputType=gcp_gcs   \
--textIdFields="note_id,note_csn_id" \
--textInputFields="fullnote" \
--inputResource=gs://<input data bucket>/input/HNO_NOTE_TEXT_PHI_MERGED/note-input-*.json \
--outputResource=gs://<input data bucket>/NOTE_DEID_result"
```



Read from local file
```

java -jar deid-3.0.9-SNAPSHOT.jar \
--deidConfigFile=deid_config_clarity.yaml \
--textIdFields="note_id" \
--textInputFields="note_text" \
--inputResource=/Users/wenchengli/dev/servers/clarity/lpch \/NOTE_FULL_PHI_PROV_test1_1000row.json
--outputResource=local_test2_result \

```
## Use Google DLP 

DLP can be integrated with two ways. One way is directly enable DLP in TiDE deid transform, which will call Google DLP API individually for each text row. The second way is to use Google DLP Native job to find PHIs independently from TiDe and merge findings of each parallel result into final deied-text. 

### Option one: enable DLP API Request in TiDE

Enabled Google DLP in TiDE config YAML file

```commandline
deidJobs:
  - jobName: stanford_deid_v3
    ...
    googleDlpEnabled: true
```

### Option two: Run DLP Native Job in parallel and merge findings later

#### Start DLP Native Job

``` 
java -jar deid-3.0.9-SNAPSHOT.jar \
--gcpCredentialsKeyFile=<google_credential.json> \
--projectId=<google_project_id> \
--deidConfigFile=deid_config_omop_genrep.yaml \
--inputBqTableId=<bigquery_input_text_table_id> \
--outputBqTableId=<bigquery_native_job_output_table_id> \
--idFields=note_id
--inspectFields=note_text


```

#### Final deid text generation with TiDE and DLP Native findings 

Run Bigquery query in 

./dev-ops/bigquery-sql/deid-merge-findings.sql



### build the project

Run Maven at deid module root.

```
mvn clean install -DskipTests=true
```
