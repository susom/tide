# TiDE Overview

TiDE is a free open-source text deidentification tool that can identify and deid PHI in clinical note text and other free text in medical data. It uses pattern matching, known PHI matching and NER to search for PHI, and use general replacement or hide-in-plain-sight to replace PHI with safe text.


## Safe Harbor 18 identifiers
TiDE can identify the following HIPAA identifiers either by pattern matching or known PHI matching:

```
  Name, Address, dates, phone, fax, Email, SSN, MRN, Health plan beneficiary number, Account number, Certificate or license number, vehicle number, URL, IP, any other characteristic that could uniquely identify the individual
```

TiDE does not process non-text information such as these two identifiers
```
Finger/Voice print, photo
```

#### TiDE Uses / Execution

TiDE can be used in various environments. Below are the prerequisites and instructions for few of the environemnts TiDE is available 

   1. Local System - Standalone
   2. Local System - using a  Docker container
   3. Google Cloud Platform 

   # Install Prerequisite
   1. [Prerequisites All](#Prerequisites-Source)
   2. [Prerequisites Local System - Standalone](#Prerequisites-Local-Standalone)
   3. [Prerequisites Local System - Using Docker Container](#Prerequisites-Container)
   4. [Prerequisites Google Cloud Platform](#Prerequisites-GCP)

   # Using TiDE
   2. [Local System - Standalone](#Using-Local-Standalone)
   3. [Local System - Using Docker Container](#Using-Container)
   4. [Google Cloud Platform](#Using-GCP-Executing-from-within-container-data-is-in-GCP-bucket)

   1. Once prerequisites are met, open a command line and change the directory to the folder where TiDE source has been downloaded, eg. if on local system, source is downloaded at "C:\Dev\tide-source" navigate to the folder
   
   ```
   cmd
   cd C:\Dev\tide-source
   ```

# Using-Local-Standalone
   1. In the command window, execute the following
   
   ```
   mvn clean install -DskipTests
   java -jar ./target/deid-3.0.21-SNAPSHOT-dataflow.jar --deidConfigFile=./src/main/resources/deid_config_omop_genrep_incl_annotator_type.yaml --inputType=text --phiFileName=./phi/phi_person_data_example.csv --personFile=./person_data/person.csv --inputResource=./sample_notes --outputResource=./output
   ```
   2. [Sample Input](#Sample-Input-Local)

   3. [TiDE Output - Local or container](#Output-Local) 
# Using-Container

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
   
   java -jar /opt/deid/target/deid-3.0.21-SNAPSHOT-dataflow.jar --deidConfigFile=/workspaces/src/main/resources/deid_config_omop_genrep_incl_annotator_type.yaml --inputType=text --phiFileName=/workspaces/phi/phi_person_data_example.csv --personFile=/workspaces/person_data/person.csv --inputResource=/workspaces/sample_notes --outputResource=/workspaces/output

   ```
   4. [Sample Input](#Sample-Input-Local)
   5. [TiDE Output - Local or container](#Output-Local)

# Using-GCP-Executing-from-within-container-data-is-in-GCP-bucket
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
   
   ```java
    java -jar -Xmx6g /opt/deid/target/deid-3.0.21-SNAPSHOT-dataflow.jar --deidConfigFile=deid_config_omop_genrep_incl_annotator_type.yaml --inputType=gcp_gcs --inputResource=gs://<INPUT_BUCKET_NAME>/sample_notes_jsonl/notes.json --outputResource=gs://<OUTPUT_BUCKET_NAME> --gcpCredentialsKeyFile=<SERVICE_ACCOUNT_KEY_DOWNLOADED> --textIdFields="id" --textInputFields="note"

   ```
   4. [Sample Input](#Sample-Input-GCP)
   5. [TiDE Output - GCP](#Output-GCP) 

# Sample-Input-Local
<TODO>
Input Arguments:
   1. inputResource (mandatory) eg. inputResource=gs://<INPUT_BUCKET_NAME>/sample_notes_jsonl/notes.json
   This argument specifies the file with notes in jsonl format to be deid.  
   
# Sample-Input-GCP
<TODO>
Input Arguments:
   1. inputResource (mandatory) eg. inputResource=/workspaces/sample_notes
   This argument specifies location of the folder with notes to be deid in text format. All files in this folder will be processed. 

# Output-Local

On execution of previous command, application will process the sample data and generate the output in the "output" folder. TiDE output is generated in the "output" folder. For every execution, application will generate a subfolder in the "output" folder using current timestamp. For latest execution output, use the folder with latest timestamp. In the "current timestamp" folder, there are 3 sets of output:
1. At the root of folder with "current timestamp", one or more files with in jsonl format. 
2. A subfolder "individual". This subfolder contains, one file containing exactly one deid note corresponding to each input note.
3. A subfolder "annotator". This subfolder contains, one file containing exactly one note corresponding to each input note with Doccano visual output decorator. 

# Output-GCP
<TODO>
On execution of previous command, application will process the sample data and generate the output in the GCP bucket specified in the argument "outputResource". TiDE output is in jsonl format. 

### Prerequisites-Source

   1. GitHub (Source Repository)

   TiDE source code is maintained in GitHub. GitHub is a code repository and is used for storing and maintaining TiDE source code.
  
   ***Access GitHub using GitHub Desktop tool***

      1. Download and install GitHub Desktop Client: https://desktop.github.com/
      2. After installation, open the GitHub Desktop program 
      3. Open File > Clone repository
      4. On Clone a repository dialog box, URL tab, in the "Repository URL", enter "https://github.com/susom/tide/" 
      5. In "Local Path", enter a value for local path where you would like to keep the source. Like on my machine the source folder is "C:\Dev\tide-source" 
      6. Click "Clone".
      7. This will download the latest TiDE code on your local system in the location specificed in local path.
      8. Open Local path folder in your choice of IDE like Visual Studio Code.

### Prerequisites-Local-Standalone

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

### Prerequisites-Container

   1. Docker installation on local machine
   
   Docker is an open platform for developing, shipping, and running applications. Docker enables you to separate your applications from your infrastructure so you can deliver software quickly. With Docker, you can manage your infrastructure in the same ways you manage your applications.

   Docker installation is different for different platforms. Here are the links for various operating system:

      1. Mac: https://docs.docker.com/docker-for-mac/install/
      2. Windows: https://docs.docker.com/docker-for-windows/install/
      3. Ubuntu: https://docs.docker.com/engine/install/ubuntu/

   2. Tools Required
      1. Windows: [PowerShell](https://github.com/PowerShell/PowerShell)
      2. Mac: Terminal
      3. Ubuntu: Shell or terminal

### Prerequisites-GCP

   1. Google Cloud Platform (GCP)
   
   Google Cloud Platform (GCP), offered by Google, is a suite of cloud computing services where you can leverage the power of online computing for performing resource intensive job typically not available on on local system.

      1. [Create Google Cloud account](https://cloud.google.com/free). If you meet the [criteria](https://cloud.google.com/free/docs/gcp-free-tier/#free-trial), you can get $300 free Cloud Billing credits to pay for resources.
      2. After creating the account, Using [Google Console](https://console.cloud.google.com/)
         1. [Create Cloud Project](https://cloud.google.com/appengine/docs/standard/nodejs/building-app/creating-project#creating-a-gcp-project)
         2. [Create Service account](https://cloud.google.com/iam/docs/creating-managing-service-accounts) 
            1. Enter name for the Service Account eg. "TiDE service account"
            2. Based on Service Account name, system will automatically generate service account id. You can either use the same name or change the name in the input box below service account name. 
            3. Enter description for the Service Account eg. "This service account will be used to verify TiDE functionality".
            4. Click > Create and continue
            5. [Adding roles to the service account](https://cloud.google.com/iam/docs/granting-changing-revoking-access#grant-single-role)
               1. Cloud Dataflow Service Agent
               2. Storage Admin
               3. BigQuery Admin
            6. Click Continue and Then Click Done
            7. The Service Account creation is complete with required roles.
            8. Under Permissions tab check if your or user is assigned to this service account, if not then click on Grant Access and add the user.
      3. [Generate Key for Service account (Json)](https://cloud.google.com/iam/docs/creating-managing-service-account-keys#iam-service-account-keys-create-console).
         1.  A key will be generated. Download this ley to your local system.
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
https://cloud.google.com/docs/authentication/getting-started


```
export GOOGLE_APPLICATION_CREDENTIALS=<gcp service account credential json file>

```

# Data Preparation for TiDE
<TODO-Add description, further cleanup>

## Directly use BigQuery table as input 

You can directly use BigQuery table that contains the free text column as input if you only need to use NER or general patten matching. If you have known PHI associated with text, you can join free text with known phi, and create a final input table with both text and known phi in each row. 

## Use JSON files on Google Cloud Storage or local disk

If you prefer to work with files, you can export table to Google Cloud Storage bucket as newline delimited JSON files. Then use these GCS files as input.


# Options to Replace PHI 

Options to deid PHI discovered by TiDE:

* Masking (everything except name and location)
* Jittering (date, age) with provided jitter value
* Surrogate name and location
* General Replacement with common patterns of each type of PHIs

# Build the project

Run Maven commands at repo root directory.
```
mvn clean install -DskipTests=true
```

# Run TiDE Pipeline

##  Configure Deid job spec

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

- general: for Phone/Fax, Email, URL, IP address, SSN
- general_number: general order, account number and general-accession
- surrogate_name: surrogate name using NameSurrogate
- surrogate_address: surrogate address using LocationSurrogate
- jitter_date_from_field: Date Anonymizer with jitter value provided in an input field
- jitter_birth_date: Date Anonymizer
- jitter_date_randomly: randomly generate jitter using hash function
- remove_age: Age Anonymizer
- remove_mrn: Mrn Anonymizer
- replace_minimumlengthword_with: find words with minimum word length
- replace_with: find word longer than 2 characters, and not in common vocabulary
- replace_strictly_with: applied strictly regardless word length, and if in common vocabulary


Configuration Example:


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

      - itemName: patient_name
        action: surrogate_name
        actionParamMap: {"format":"L,F","f_zip":"zip","f_gender":"","f_dob":"birth_date"}
        fields:  pat_name, PROXY_NAME

      - itemName: patient_name
        action: surrogate_name
        actionParamMap: {"format":"F","f_zip":"zip","f_gender":"","f_dob":"birth_date"}
        fields:  PAT_FIRST_NAME, preferred_name

      - itemName: patient_mrn
        action: tag
        actionParam: mrn
        fields:  PAT_MRN_ID
        .....

```

## Run Deid jobs

TiDE was created using Apache Beam programming model. So you can run TiDE on many technologies using appropriate runners on https://beam.apache.org/documentation/runners/capability-matrix/

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
|inputType    | type of the input souce. Currently supports Google Cloud Storage, Google BigQuery and local files  | gcp_gcs , gcp_bq , local |
|inputResource | Path of the file to read from | gs://mybucket/path/to/json/files/*.json|
|outputResource | Path of the output files | |
|DeidConfigFile | Name of the Deid configuration. Can use the provided configurations or external config file | deid_config_clarity.yaml |
|dlpProject   | GCP project id, if use GCP DLP service | | 
|googleDlpEnabled | Turn on/off Google DLP | true or false|


### Pipeline runtime parameters for Apache Beam runners
https://beam.apache.org/documentation/runners/dataflow/

### Pipeline runtime parameters for  Google Cloud Dataflow
https://cloud.google.com/dataflow/docs/guides/setting-pipeline-options



## Run TiDE as regular Java application
Deid jobs can be run either with Maven or Java jar.

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
--deidConfigFile=deid_config_clarity.yaml --inputType=gcp_gcs   \
--textIdFields="note_id,note_csn_id" \
--textInputFields="fullnote" \
--inputResource=gs://<input data bucket>/input/HNO_NOTE_TEXT_PHI_MERGED/note-input-*.json \
--outputResource=gs://<input data bucket>/NOTE_DEID_result"
```



An example of reading from local file and produce result at local

```
java -jar deid-3.0.21-SNAPSHOT.jar \
--deidConfigFile=deid_config_clarity.yaml \
--textIdFields="note_id" \
--textInputFields="note_text" \
--inputResource=/Users/wenchengli/dev/NOTE_FULL_PHI_PROV_test1_1000row.json
--outputResource=local_test2_result \

```

## Optionally use Google DLP API to identify the following DLP infoTypes: 
https://cloud.google.com/dlp/docs/infotypes-reference

```
  AGE,DATE,DATE_OF_BIRTH,CREDIT_CARD_NUMBER,US_BANK_ROUTING_MICR,AMERICAN_BANKERS_CUSIP_ID,IBAN_CODE,US_ADOPTION_TAXPAYER_IDENTIFICATION_NUMBER,US_DRIVERS_LICENSE_NUMBER,US_INDIVIDUAL_TAXPAYER_IDENTIFICATION_NUMBER,US_PREPARER_TAXPAYER_IDENTIFICATION_NUMBER,US_PASSPORT,US_SOCIAL_SECURITY_NUMBER,US_EMPLOYER_IDENTIFICATION_NUMBER,US_VEHICLE_IDENTIFICATION_NUMBER,EMAIL_ADDRESS,PERSON_NAME,PHONE_NUMBER,US_HEALTHCARE_NPI,US_DEA_NUMBER,LOCATION,IP_ADDRESS,MAC_ADDRESS,URL
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
java -jar deid-3.0.21-SNAPSHOT.jar \
--gcpCredentialsKeyFile=<google_credential.json> \
--projectId=<google_project_id> \
--deidConfigFile=deid_config_omop_genrep.yaml \
--inputBqTableId=<bigquery_input_text_table_id> \
--outputBqTableId=<bigquery_native_job_output_table_id> \
--idFields=note_id
--inspectFields=note_text

```
