# TiDE

![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![Google Cloud](https://img.shields.io/badge/GoogleCloud-%234285F4.svg?style=for-the-badge&logo=google-cloud&logoColor=white)
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=java&logoColor=white)
![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)
![Google Drive](https://img.shields.io/badge/Google%20Drive-4285F4?style=for-the-badge&logo=googledrive&logoColor=white)
![Alpine Linux](https://img.shields.io/badge/Alpine_Linux-%230D597F.svg?style=for-the-badge&logo=alpine-linux&logoColor=white)
![Mac OS](https://img.shields.io/badge/mac%20os-000000?style=for-the-badge&logo=macos&logoColor=F0F0F0)
![Windows](https://img.shields.io/badge/Windows-0078D6?style=for-the-badge&logo=windows&logoColor=white)

[![Maintenance](https://img.shields.io/badge/Maintained%3F-yes-green.svg)](https://GitHub.com/Naereen/StrapDown.js/graphs/commit-activity)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Overview

TiDE is a free open-source text deidentification tool that can identify and deid PHI in clinical note text and other free text in medical data. It uses pattern matching, known PHI matching and NER to search for PHI, and use general replacement or hide-in-plain-sight to replace PHI with safe text. For more information about the TiDE algorithm, please refer to the Section 6 of the manuscript ["A new paradigm for accelerating clinical data science at Stanford Medicine"](https://arxiv.org/abs/2003.10534)

## Safe Harbor 18 identifiers

TiDE can identify the following HIPAA identifiers either by a) name entity recognition or pattern matching or b) known PHI matching:

```
  Name, Address, dates, phone, fax, Email, SSN, MRN, Health plan beneficiary number, Account number, Certificate or license number, vehicle number, URL, IP, any other characteristic that could uniquely identify the individual
```

TiDE does not process non-text information such as these two identifiers

```
Finger/Voice print, photo
```

Note that certain identifiers may a have pattern that is unique to the organization's EHR system and may fail our pattern matching or Name Entity Recognition. Please review the TiDE output of your organization's clinical notes carefully.

## TiDE Uses / Execution

TiDE can be used in various environments. Below are the prerequisites and instructions for several environments TiDE is available

   1. Local System - Standalone
   2. Local System - using a  Docker container
   3. Google Cloud Platform

### Install Prerequisite

   1. [Prerequisites All](#Prerequisites-Source)
   2. [Prerequisites Local System - Standalone](#Prerequisites-Local-Standalone)
   3. [Prerequisites Local System - Using Docker Container](#Prerequisites-Container)
   4. [Prerequisites Google Cloud Platform](#Prerequisites-GCP)

### Using TiDE

   The following examples are based on executing TiDE on windows system

   Once prerequisites are met, open a command line and change the directory to the folder where TiDE source has been downloaded, e.g. if on local system, source is downloaded at "C:\Dev\tide-source" navigate to the folder

   ```
   cmd
   cd C:\Dev\tide-source
   ```

   1. [Local System - Standalone](#Using-Local-Standalone)
   2. [Local System - Using Docker Container](#Using-Container)
   3. [Google Cloud Platform](#Using-GCP-Executing-from-within-container-data-is-in-GCP-bucket)

### Using Local Standalone

   1. In the command window, execute the following

      1. If notes file is prepared in text format:

      ```
      mvn clean install -DskipTests
      java -jar ./target/deid-3.0.31-SNAPSHOT-dataflow.jar --deidConfigFile=./src/main/resources/deid_config_omop_genrep.yaml --annotatorConfigFile=./src/main/resources/annotator_config.yaml --inputType=text --phiFileName=./phi/phi_person_data_example.csv --personFile=./person_data/person.csv --inputResource=./sample_notes --outputResource=./output
      ```

      2. If notes file is prepared in jsonl format:

      ```
      mvn clean install -DskipTests
      java -jar ./target/deid-3.0.31-SNAPSHOT-dataflow.jar --deidConfigFile=./src/main/resources/deid_config_omop_genrep.yaml --annotatorConfigFile=./src/main/resources/annotator_config.yaml --inputType=local --inputResource=./sample_notes_jsonl/notes.json --outputResource=./output --textIdFields="note_id" --textInputFields="note_text"
      ```

   2. [Sample Input](#Sample-Input-Local)

   3. [TiDE Output - Local or container](#Output-Local)

### Using Container

   1. In the command window, execute the following

   ```
   docker build . -t tide-program:latest
   ```

   2. Update the following command if the source location is different from (C:\Dev\tide-source). This command will map the local source and output folder with container. Execute the following

   ```
   docker run -it -v /mnt/c/Dev/tide-source:/workspaces tide-program:latest
   ```

   3. Above command will switch the command line prompt to Shell of the TiDE image. Execute the following in the Container Shell

   ```java

   java -jar /opt/deid/target/deid-3.0.31-SNAPSHOT-dataflow.jar --deidConfigFile=./src/main/resources/deid_config_omop_genrep.yaml --annotatorConfigFile=./src/main/resources/annotator_config.yaml --inputType=text --phiFileName=/workspaces/phi/phi_person_data_example.csv --personFile=/workspaces/person_data/person.csv --inputResource=/workspaces/sample_notes --outputResource=/workspaces/output

   ```

   4. [Sample Input](#Sample-Input-Local)
   5. [TiDE Output - Local or container](#Output-Local)

### Using GCP Executing from within container data is in GCP bucket

   1. In the command window, execute the following

   ```
   docker build . -t tide-program:latest
   ```

   2. Update the following command if the source location is different from (C:\Dev\tide-source). This command will map the local source and output folder with container. Execute the following

   ```
   docker run -it -v /mnt/c/Dev/tide-source:/workspaces tide-program:latest
   ```

   3. Above command will switch the command line prompt to Shell of the TiDE image. Execute the following in the Container Shell

   
   ```java
    java -jar -Xmx6g /opt/deid/target/deid-3.0.31-SNAPSHOT-dataflow.jar --deidConfigFile=./src/main/resources/deid_config_omop_genrep.yaml --annotatorConfigFile=./src/main/resources/annotator_config.yaml --inputType=gcp_gcs --inputResource=gs://<INPUT_BUCKET_NAME>/sample_notes_jsonl/notes.json --outputResource=gs://<OUTPUT_BUCKET_NAME> --gcpCredentialsKeyFile=<SERVICE_ACCOUNT_KEY_DOWNLOADED> --textIdFields="id" --textInputFields="note"
   ```

   4. [Sample Input](#Sample-Input-GCP)
   5. [TiDE Output - GCP](#Output-GCP)

### Sample Input Local

   Sample Notes:
      For inputType="text": [sample notes folder](sample_notes)
      For inputType="local": [sample notes jsonl folder](sample_notes_jsonl)

   Input Arguments:

   1. inputResource (mandatory) e.g. inputResource=/workspaces/sample_notes
   When used with
      1. "inputType=text", this argument specifies location of the folder with notes to be deid in text format. All files in this folder will be processed.
      2. "inputType="local", this argument specifies the file with notes to be deid in newline delimited JSON files (jsonl) format.You can directly use jsonl that contains id and free text column as input if you only need to use NER or general patten matching. If you have known PHI associated with text, you need to have phi information embedded in the jsonl file.

### Sample Input GCP

   Sample Notes:
      Please refer to ([sample notes folder](sample_notes_jsonl))

   Input Arguments:

   1. inputResource (mandatory) e.g. inputResource=gs://<INPUT_BUCKET_NAME>/sample_notes_jsonl/notes.json
   This argument specifies the file with notes to be deid in newline delimited JSON files (jsonl) format.

### Output Local

   On execution of previous command, application will start processing the input notes and display messages like below

   ```
      21:24:43,972 INFO  [main]     com.github.susom.starr.deid.Main.run(Main.java:67) Current Settings:
      appName: Main
      deidConfigFile: /workspaces/src/main/resources/deid_config_omop_genrep.yaml
      annotatorConfigFile: /workspaces/src/main/resources/annotator_config.yaml
      gcpCredentialsKeyFile:
      inputResource: /workspaces/sample_notes
      inputType: text
      optionsId: 0
      outputResource: /workspaces/output
      personFile: /workspaces/person_data/person.csv
      phiFileName: /workspaces/phi/phi_person_data_example.csv
      runner: class org.apache.beam.runners.direct.DirectRunner
      stableUniqueNames: WARNING
      ®
      21:24:43,980 INFO  [main]     com.github.susom.starr.deid.Main.run(Main.java:76) reading configuration from file /workspaces/src/main/resources/deid_config_omop_genrep.yaml®
      21:24:44,069 INFO  [main]     com.github.susom.starr.deid.Main.run(Main.java:83) received configuration for note_deid_20190812®
      21:24:46,329 INFO  [direct-runner-worker]     edu.stanford.nlp.util.logging.SLF4JHandler.print(SLF4JHandler.java:88) Adding annotator tokenize®
      ..............
      ..............
      ..............
      21:24:58,919 INFO  [direct-runner-worker]     org.apache.beam.sdk.io.FileBasedSink$WriteOperation.removeTemporaryFiles(FileBasedSink.java:805) Will remove known temporary file /workspaces/output/1629926684106/.temp-beam-fb9dbd2c-f17c-4d4a-a99c-8034e4d2fef9/f1ccd490-eb10-4cee-a5a2-96c82f221c74®
   ```

   On completion of execution of previous command, TiDE output will be available in the "output" folder. For every execution, application will create a subfolder in the "output" folder using "current timestamp in long format". For latest execution output, use the folder with latest timestamp. This folder will have 3 sets of output:

   1. At the root of "current timestamp in long format" folder, one or more files in newline delimited JSON files (jsonl) format containing original note, deid note, and findings [Sample jsonl Output](/output/1629926684106)
   2. A subfolder "individual" containing deid notes. This folder will have one file corresponding to each input note [Sample Individual Output](/output/1629926684106/individual)
   3. A subfolder "annotator" containing output in Doccano format. This folder will have one file corresponding to each input note [Sample Annotator Output](/output/1629926684106/annotator)

### Output GCP

   On completion of execution of previous command, TiDE output will be available in the GCP bucket specified in the "outputResource" argument. TiDE output is in newline delimited JSON files (jsonl) format.

## Prerequisites Source

   1. GitHub (Source Repository)

   TiDE source code is maintained in GitHub. GitHub is a code repository and is used for storing and maintaining TiDE source code.
  
   ***Access GitHub using GitHub Desktop tool***

   1. Download and install [GitHub Desktop Client](https://desktop.github.com/)
   2. After installation, open the GitHub Desktop program 
   3. Open File > Clone repository
   4. On Clone a repository dialog box, URL tab, in the "Repository URL", enter "https://github.com/susom/tide/"
   5. In "Local Path", enter a value for local path where you would like to keep the source. Like on my machine the source folder is "C:\Dev\tide-source"
   6. Click "Clone".
   7. This will download the latest TiDE code on your local system in the location specified in local path.
   8. Open Local path folder in your choice of IDE like Visual Studio Code.

## Prerequisites Local Standalone

   1. Java
      1. Install Java on your system. Here are the links for various operating system:
         1. [Windows](https://devwithus.com/install-java-windows-10/)
         2. [Linux](https://www.guru99.com/how-to-install-java-on-ubuntu.html)
         3. [Mac](https://mkyong.com/java/how-to-install-java-on-mac-osx/)
   2. Maven
      1. Install Maven on your system. Here are the links for various operating system:
         1. [Windows](https://mkyong.com/maven/how-to-install-maven-in-windows/)
         2. [Linux](https://linuxize.com/post/how-to-install-apache-maven-on-ubuntu-20-04/)
         3. [Mac](https://mkyong.com/maven/install-maven-on-mac-osx/)

## Prerequisites Container

   1. Docker installation on local machine

   Docker is an open platform for developing, shipping, and running applications. Docker enables you to separate your applications from your infrastructure so you can deliver software quickly. With Docker, you can manage your infrastructure in the same ways you manage your applications.

   Docker installation is different for different platforms. Here are the links for various operating system:

   1. [Mac](https://docs.docker.com/docker-for-mac/install/)
   2. [Windows](https://docs.docker.com/docker-for-windows/install/)
   3. [Ubuntu](https://docs.docker.com/engine/install/ubuntu/)

   Tools Required

   1. Mac: Terminal
   2. Windows: [PowerShell](https://github.com/PowerShell/PowerShell)
   3. Ubuntu: Shell or terminal

## Prerequisites GCP

   1. Google Cloud Platform (GCP)

   Google Cloud Platform (GCP), offered by Google, is a suite of cloud computing services where you can leverage the power of online computing for performing resource intensive job typically not available on on local system.

   1. [Create Google Cloud account](https://cloud.google.com/free). If you meet the [criteria](https://cloud.google.com/free/docs/gcp-free-tier/#free-trial), you may get Cloud Billing credits to pay for resources from Google (Currently Google is offering 90-day, $300 Free Trial).
   2. After creating the account, Using [Google Console](https://console.cloud.google.com/)
      1. [Create Cloud Project](https://cloud.google.com/appengine/docs/standard/nodejs/building-app/creating-project#creating-a-gcp-project)
      2. [Create Service account](https://cloud.google.com/iam/docs/creating-managing-service-accounts) 
         1. Enter name for the Service Account e.g. "TiDE service account"
         2. Based on Service Account name, system will automatically generate service account id. You can either use the same name or change the name in the input box below service account name. 
         3. Enter description for the Service Account e.g. "This service account will be used to verify TiDE functionality".
         4. Click > Create and continue
         5. [Adding roles to the service account](https://cloud.google.com/iam/docs/granting-changing-revoking-access#grant-single-role)
            1. Cloud Dataflow Service Agent
            2. Storage Admin
            3. BigQuery Admin
         6. Click Continue and Then Click Done
         7. The Service Account creation is complete with required roles.
         8. Under Permissions tab check if your or user is assigned to this service account, if not then click on Grant Access and add the user.
   3. [Generate Key for Service account (Json)](https://cloud.google.com/iam/docs/creating-managing-service-account-keys#iam-service-account-keys-create-console).
      1. A key will be generated. Download this ley to your local system.
   4. [Add Billing to GCP account](https://cloud.google.com/billing/docs/how-to/manage-billing-account#create_a_new_billing_account)
      1. On Google Console (https://console.cloud.google.com/). Click on the Navigation menu on the left, and then Hover on Billing
      2. Click on Manage Billing Accounts > Add Billing Account.
      3. Fill all the required details and attach the project to the billing.
   5. Configure Storage for the GCP project [Create Storage Buckets](https://cloud.google.com/storage/docs/creating-buckets)
      1. Open [Google Console](https://console.cloud.google.com/). Click on the Navigation menu on the left, and then Hover on Cloud Storage.
      2. Click Cloud storage > Browser.
      3. Two buckets are required. One for input data and another for output data, steps for creation of both buckets are same.
      4. Click on create bucket link 
         1. Give name to your bucket. click continue
         2. Select Location type - REGIONAL > Select region from dropdown > Continue
         3. Select default storage class Standard > Continue
         4. Select control access as Uniform, make sure the checkbox for **Enforce public access prevention on this bucket** is checked > continue
         5. Under Advance setting select Encryption type > google-managed encryption key.
         6. Click Create.

You need to configure Google Cloud credential if run TiDE on Dataflow.
<https://cloud.google.com/docs/authentication/getting-started>

```
export GOOGLE_APPLICATION_CREDENTIALS=<gcp service account credential json file>
```

## Data Preparation for TiDE

TiDE can process data in various formats such as

1. "text" note (one per file) along with known phi file (csv format) and note-phi relationship file (csv format)
2. newline delimited JSON files (jsonl) file with phi information along with the note
3. "BigQuery" table

## Use text files on local disk

TiDE supports text files for input notes to be deid'ed. Each file should have exactly one note. The file name is used as note id. The known PHI information for the persons can be supplied in a phi file in csv format. The phi file should have a person id. A file with relationship between person id and note should be supplied in csv file. TiDE supports multiple notes for same person.  

## Directly use BigQuery table as input

You can directly use BigQuery table that contains the free text column as input if you only need to use NER or general patten matching. If you have known PHI associated with text, you can join free text with known phi, and create a final input table with both text and known phi in each row.

## Use JSON files on Google Cloud Storage or local disk

TiDE supports newline delimited JSON files (jsonl) files. Export table to Google Cloud Storage bucket as newline delimited JSON files. Then use these GCS files as input.

## Options to Replace PHI

Options to deid PHI discovered by TiDE:

* Masking (everything except name and location)
* Jittering (date, age) with provided jitter value
* Surrogate name and location
* General Replacement with common patterns of each type of PHIs

## Run TiDE Pipeline

### Configure Deid job spec

TiDE has some embedded job specifications ([resource folder](src/main/resources)) that fit for most common use cases.

If need to customize the configuration, create a new config yaml file, and use the file path as argument value of --deidConfigFile when run the tool.

## Deid Config YAML file

Sample configuration to switch on/off features

```yaml
    analytic: false
    googleDlpEnabled: false
    nerEnabled: true
    annotatorOutputEnabled: true

```

Multiple deid actions can be grouped into same PHI category by using same __itemName__. Grouping is useful for deid quality analytics if analytic is enabled.

Configure General Regex pattern matching or find known PHI of the patient associated with the text.

* general: for Phone/Fax, Email, URL, IP address, SSN
* general_number: general order, account number and general-accession
* surrogate_name: surrogate name using NameSurrogate
* surrogate_address: surrogate address using LocationSurrogate
* jitter_date_from_field: Date Anonymizer with jitter value provided in an input field
* jitter_birth_date: Date Anonymizer
* jitter_date_randomly: randomly generate jitter using hash function
* remove_age: Age Anonymizer
* remove_mrn: Mrn Anonymizer
* replace_minimumlengthword_with: find words with minimum word length
* replace_with: find word longer than 2 characters, and not in common vocabulary
* replace_strictly_with: applied strictly regardless word length, and if in common vocabulary

Configuration Example:

   Sample configuration file(s):
      Please refer to ([deid configuration file](src/main/resources/deid_config_omop_genrep.yaml))
      Please refer to ([annotator configuration file](src/main/resources/annotator_config.yaml))

```yaml
name: note_deid_20190812
deidJobs:
  - jobName: stanford_deid_v3
    version: v3.0
    textFields: note_text
    textIdFields: note_id
    analytic: false
    ......
    
    spec:
      - itemName: phi_date
        action: jitter_date_from_field
        actionParam: 10/10/2100
        fields: 'JITTER'

      - itemName: mrn
        action: remove_mrn
        actionParam: 99999999
        fields: ''

      - itemName: patient_mrn
        action: replace_minimumlengthword_with
        actionParam: 99999999 3
        fields: PAT_MRN_ID

      - itemName: other_id
        action: replace_minimumlengthword_with
        actionParam: 999999999 3
        fields: pat_id, birth_wrist_band, epic_pat_id, PRIM_CVG_ID, PRIM_EPP_ID, EMPLOYER_ID
        .....

```

## Run Deid jobs

TiDE was created using Apache Beam programming model. So you can run TiDE on many technologies using appropriate runners on <https://beam.apache.org/documentation/runners/capability-matrix/>

## Pipeline Runtime Configurations

Three types of parameters are needed for running TiDE:

1. for TiDE itself, for example, specify deid configuration
2. for Apache Beam runners
3. for running TiDE on Google Cloud Dataflow

### Pipeline runtime parameters for TiDE

|parameter|description| sample value |
|--|--|--|
|textInputFields |field name in input that contains free text | note_text|
|textIdFields |field name in input that contains row id(s) | note_id,note_csn_id|
| runner | type of the Apache Beam runner | DirectRunner, DataflowRunner |
|inputType    | type of the input souce. Currently supports Google Cloud Storage, Google BigQuery and local files  | gcp_gcs, gcp_bq, local, text |
|inputResource | Path of the file to read from | gs://mybucket/path/to/json/files/*.json|
|outputResource | Path of the output files | |
|DeidConfigFile | Name of the Deid configuration. Can use the provided configurations or external config file | deid_config_omop_genrep.yaml |
|AnnotatorConfigFile | Name of the Annotator configuration. Can use the provided configurations or external config file | annotator_config.yaml |
|dlpProject   | GCP project id, if use GCP DLP service | |
|googleDlpEnabled | Turn on/off Google DLP | true or false|
|phiFileName | Known PHI file | /workspaces/phi/phi_person_data_example.csv|
|personFile | Relationship between known PHI and notes | /workspaces/person_data/person.csv|

#### Known PHI File Format

   Sample PHI file:
      Please refer to ([PHI file](phi/phi_person_data_example.csv))

There should be max of one record per person (person_id) in this file

```
person_id,MRN,JITTER,STUDY_ID,ANON_ID,pat_id,pat_name,add_line_1,city,zip,home_phone,email_address,birth_date,sex_c,ssn,epic_pat_id,PAT_MRN_ID,PAT_LAST_NAME,PAT_FIRST_NAME,EMPLOYER_ID,cur_pcp_prov_id,PROV_NAME,father_name,father_addr_ln_1,father_city,father_zip,father_cell_phone,mother_name,mother_cell_phone,emerg_pat_rel_c,accession_num
```

#### Person File Format

   Sample person file:
      Please refer to ([relationship file](person_data/person.csv))

There should be max of one record per note (note_id) in this file

```
note_id,person_id
```

### Pipeline runtime parameters

1. [Apache Beam runners](https://beam.apache.org/documentation/runners/dataflow/)
2. [Google Cloud Dataflow](https://cloud.google.com/dataflow/docs/guides/setting-pipeline-options)

## Run TiDE as regular Java application

Here is an example for reading from Google Cloud Storage and storing result to Google Cloud Storage:

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
--deidConfigFile=deid_config_omop_genrep.yaml --annotatorConfigFile=annotator_config.yaml --inputType=gcp_gcs   \
--textIdFields="note_id,note_csn_id" \
--textInputFields="fullnote" \
--inputResource=gs://<input data bucket>/input/HNO_NOTE_TEXT_PHI_MERGED/note-input-*.json \
--outputResource=gs://<input data bucket>/NOTE_DEID_result"
```

## Optionally use Google DLP API to identify the following DLP infoTypes
<https://cloud.google.com/dlp/docs/infotypes-reference>

```
  AGE, DATE, DATE_OF_BIRTH, CREDIT_CARD_NUMBER, US_BANK_ROUTING_MICR, AMERICAN_BANKERS_CUSIP_ID, IBAN_CODE, US_ADOPTION_TAXPAYER_IDENTIFICATION_NUMBER, US_DRIVERS_LICENSE_NUMBER, US_INDIVIDUAL_TAXPAYER_IDENTIFICATION_NUMBER, US_PREPARER_TAXPAYER_IDENTIFICATION_NUMBER, US_PASSPORT, US_SOCIAL_SECURITY_NUMBER, US_EMPLOYER_IDENTIFICATION_NUMBER, US_VEHICLE_IDENTIFICATION_NUMBER, EMAIL_ADDRESS, PERSON_NAME, PHONE_NUMBER, US_HEALTHCARE_NPI, US_DEA_NUMBER, LOCATION, IP_ADDRESS, MAC_ADDRESS, URL
```

## Use Google DLP

DLP can be integrated with two ways. One way is directly enable DLP in TiDE deid transform, which will call Google DLP API individually for each text row. The second way is to use Google DLP Native job to find PHIs independently from TiDE and merge findings of each parallel result into final deied-text.

### Option one: enable DLP API Request in TiDE

Enabled Google DLP in TiDE config YAML file

```commandline
deidJobs:
  - jobName: stanford_deid_v3
    ...
    googleDlpEnabled: true
```

### Option two: Run DLP Native Job to find PHI

```
java -jar deid-3.0.31-SNAPSHOT.jar \
--gcpCredentialsKeyFile=<google_credential.json> \
--projectId=<google_project_id> \
--deidConfigFile=deid_config_omop_genrep.yaml \
--annotatorConfigFile=annotator_config.yaml \
--inputBqTableId=<bigquery_input_text_table_id> \
--outputBqTableId=<bigquery_native_job_output_table_id> \
--idFields=note_id
--inspectFields=note_text

```
