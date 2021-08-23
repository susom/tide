# TiDE Overview

TiDE is a free open-source text deidentification tool that can identify and deid PHI in clinical note text and other free text in medical data. It uses pattern matching, known PHI matching and NER to search for PHI, and use geenral replacement or hide-in-plain-sight to replace PHI with safe text.


## Safe Harbor 18 identifiers
TiDE can identify the following HIPAA identifiers either by pattern matching or known PHI matching:

```
  Name, Address, dates,phone,fax,Email,SSN,MRN,Health plan beneficiary number,Account number,Certificate or license number,vehicle number,URL,IP, Any other characteristic that could uniquely identify the individual
```

TiDE does not process non-text information such as these two identifiers
```
Finger/Voice print,photo
```

#### How to execute?

***If you need to install the required Dependencies please refer to the "Prerequisites" Section***

1. Once prerequisites are met execute the commands below
2. Open a command line and change the directory to the folder where TiDE source has been downloaded
3. Map the folder where source is located on local machine, for example on my machine it is mapped to "/Users/mayurd/local/tide"

   ```
   docker build . -t tide-program:latest
   docker run -it -v /Users/mayurd/local/tide:/workspaces tide-program:latest
   ```

4. Above command will switch to Shell of the TiDE image.
5. Execute the command below.

##### 1. Executing jar when all necessary files are in your local (Laptop or Desktop)

   ```java
   
   java -jar /opt/deid/target/deid-3.0.21-SNAPSHOT-dataflow.jar --deidConfigFile=/workspaces/src/main/resources/deid_config_omop_genrep_incl_annotator_type.yaml --inputType=text --phiFileName=/workspaces/phi/phi_person_data_example.csv --personFile=/workspaces/person_data/person.csv --inputResource=/workspaces/sample_notes --outputResource=/workspaces/output

   ```
On execution of previous command, application will process the sample data and generate the output in the mounted "workspaces" folder. The output for TiDE is generated in the "output" folder specified in the mounted location. Current execution output is in the subfolder with current timestamp in the "output" folder. Individual deid-note is in "individual" foler. Doccano output is generated in the "annotator" folder.

##### 2. Executing from within container, data is in GCP bucket

   ```java
    java -jar -Xmx6g /opt/deid/target/deid-3.0.21-SNAPSHOT-dataflow.jar --deidConfigFile=deid_config_omop_genrep_incl_annotator_type.yaml --inputType=gcp_gcs --inputResource=gs://<INPUT_BUCKET_NAME>/sample_notes_jsonl/notes.json --outputResource=gs://<OUTPUT_BUCKET_NAME> --gcpCredentialsKeyFile=<SERVICE_ACCOUNT_KEY_DOWNLOADED> --textIdFields="id" --textInputFields="note"
  ```  


#### Prerequisites

1. GitHub (Source Repository)

   TiDE source code is maiantained in GitHub. This requires a GitHub account (free). GitHub is a code repository and is used for storing and maintaining TiDE source code. GitHub preserves commit history of the pushed changes. Multiple people can work on the same repository, when shared and create their own branches from main branch, in order to make their own changes without impacting anyone else code.

   1. Register for GitHub account : https://github.com/signup?source=login
  
  ***Access GitHub using GitHub Desktop tool***

   1. Download and install github Desktop Client: https://desktop.github.com/
   2. After installation, open the GitHub Desktop program and login to github account from the GitHub desktop Window
   3. Once loggedin into github desktop you can see 2 sections in GitHub Desktop
      1. Left section will have options like 
         1. Create a tutorial repository
         2. Clone a repository from the internet
         3. Create a new repository on your hard drive
         4. Add an existing repository from your hard disk.
      2. Right Section will have list of repositories, if you have any.

  ***Access GitHub using CLI***

   1. Install git on your local machine : https://git-scm.com/downloads
   2. Once downloaded open your shell(Git bash for Windows) run the commands below to set username and email for your git account.
      1. $ git config --global user.name "<GitHub_User_Name>" 
      2. $ git config --global user.email GitHub_Email_ID
   3. Generate SSH from your local, this will be used by GitHub account to validate your local system whenever your push or pull code from your code repo from your account. in your shell/Terminal type the command below and hit enter till you see the Gibrish image.
      1. $ ssh-keygen -t ed25519 
      2. once the Gibrish image appears navigate to your root folder and list .ssh folder items 
         1.  location of ssh folder /home/<your_current_account_name>/.ssh/
         2.  you will see id.ed25519 and id.ed25519_pub
         3.  id.rsa needs to be kepty safe and id.ed25519_pub is provided to the host system.
         4.  COPY the id.ed25519_pub content and open your GitHub
             1. In the upper-right corner of any page, click your profile photo, then click Settings.
             2. In the user settings sidebar, click SSH and GPG keys. 
             3. Click New SSH key or Add SSH key. 
             4. In the "Title" field, add a descriptive label for the new key. For example, if you're using a personal Mac, you might call this key "XYZ".
             5. Paste your key into the "Key" field. 
             6. Click Add SSH key. 
             7. If prompted, confirm your GitHub password.
   4. Few Tips with GitHub on local
      1. Add the code to GitHub repo
         1. Create a repo on GitHub
            1. In the upper-right corner of any page, click your + button adjacent to profile photo, then click New Repository.
            2. Fill all the necessary details, you will be asked on repo type
               1. Private: Only you can see, if you are logged-in
               2. Public: Anyone can see
      2. If you have code in local machine and want to push on the new repo just created in step one.
         1. Open Shell/Terminal and navigate to the working code folder.
         2. Initialize the local directory as a Git repository.
            1. $ git init -b main
         3. Add the files in your new local repository. This stages them for the first commit. 
            1. $ git add . # Adds the files in the local repository and stages them for commit. To unstage a file, use 'git reset HEAD YOUR-FILE'.
         4. Commit the files that you've staged in your local repository. 
            1. $ git commit -m "First commit" //Commits the tracked changes and prepares them to be pushed to a remote repository. To remove this commit and modify the file, use 'git reset --soft HEAD~1' and commit and add the file again.
         5. At the top of your GitHub repository's Quick Setup page, click to copy the remote repository URL. 
         6. In Terminal, add the URL for the remote repository where your local repository will be pushed. 
            1. $ git remote add origin <GITHUB_REMOTE_URL>  //Sets the new remote
            2. git remote -v  //Verifies the new remote URL
         7. Push the changes in your local repository to GitHub.
            1. $ git push origin main  //Pushes the changes in your local repository up to the remote repository you specified as the origin
      3. Pull project to local
         1. Navigate to your repository on GitHub 
         2. On right hand side you will the green color Code button > click > clickon ssh > Copy the link.
         3. Now Open your shell/Terminal and navigate to the folder where you want to copy the repo to work and hit the command below
            1. Git clone <Repo_link_copied_from_GitHub>
         4. Authentication will not be required as your ssh key is already there with GitHub, in case of HTTPS type link, authentication will be asked.
      4. Push the code to repo
         1. Once you have made the changes to the necessary file, hit the commands below
            1. $ git add .
            2. $ git commit -m 'initial project version'
            3. $ git remote add origin URL  
            4. $ git push -u origin master
         2. You can verify your changes once you naviagte to your github repo
      5. Few handy commands
         1. $ git remote -v // Lists a Git project's remotes.
         2. $ git pull // Get latest Data from the actual branch 
         3. $ git branch -a // Show all available branches  
         4. $ git branch branch-name // create a branch  
         5. $ git checkout branch-name // Switch to a branch  
         6. $ git rm // remove file  
         7. $ git rm -f // to remove it when it’s already in the index  
         8. $ git rm --cached // keep on hard drive but remove from being tracked  
         9. $ git rm --cached -r // remove a folder keep on hard drive but remove from being tracked 
         10. $ git branch -d branch-name // delete the branch  
         11. If you want to put your changes temporarely aside and pull new stuff use stash:
             1.  $ git stash
             2.  $ git pull
             3.  $ git stash pop
         12. To remove a stash completely:
             1.  $ git stash drop
    

2. Docker installation on local machine
   
   Docker is an open platform for developing, shipping, and running applications. Docker enables you to separate your applications from your infrastructure so you can deliver software quickly. With Docker, you can manage your infrastructure in the same ways you manage your applications.

   Docker installation is diffrent for diffrent platform, choose as per yor OS

   1. Mac : https://docs.docker.com/docker-for-mac/install/
   2. Windows: https://docs.docker.com/docker-for-windows/install/
   3. Ubuntu: https://docs.docker.com/engine/install/ubuntu/

3. Dockerfile in Base code folder
   Dockerfile is a set of instructions that is needed to create a Docker images, which will run as a container and will hold all the components to execute your application.
   You will find this file in the base directory of the application code.

4. Google Cloud Platform (GCP)
   GCP is a cloud platform where you can leverage the power of computaion for performing job which can outrun your local system's resources.

   1. To create a GCP account (initially GCP give you $300 free Credits): https://cloud.google.com/free
   2. Once done, Open the console Navigation menu (![navigation](/resources/navigation.PNG)) on the left, and then Hover on IAM & Admin > Service Accounts.
   3. Click on Create Service account option on the top bar of the window.
      1. Give the name to the service Account
      2. Give description in the 3rd field.
      3. Click > Create and continue
      4. Click on selete roles and add 3 roles one by one (Cloud Dataflow Service Agent, Storage Admin, BigQuery Admin) 
      5. Click continue
      6. Then click done
      7. Once that is done you will see the Service account created by you.
      8. Click on the 3 dots under action column and click on the manage keys option.
      9. Click on ADD KEY > Create new KEY > Choose Key type - Json > Click create.
      10. A key will be generated and downloaded to your local.
      11. Under Permissions tab check if your or user is assigned to this service account, if not then click on Grant Access and add the user.
   4.  To create a billing account(necessary)
       1. Open the console Navigation menu (![navigation](/resources/navigation.PNG)) on the left,and selete Billing
       2. Click on Manage Billing Accounts > Add Billing Account.
       3. Fill all the required details and attach the project to the billing.
   5. Again click on console Navigation menu (![navigation](/resources/navigation.PNG)) on the left, and under storage > click Cloud storage > Browser.
      1. We need 2 buckets one for input data and another for output data, steps for creation of both buckets are same.
      2. Click on create bucket link 
         1. Give name to your bucket. click continue
         2. Select Location type - REGIONAL > Select region from dropdown > Continue
         3. Select default storage class Standard > Continue
         4. Select control access as Uniform, Make sure the checkbox for **Enforce public access preventation on this bucket** is checked > continue
         5. Under Advance setting select Encryption type > google-managed encryption key.
         6. Click Create.
5. Download PowerShell: https://github.com/PowerShell/PowerShell

# Data Preparation for TiDE

## Directly use BigQuery table as input 

You can directly use BigQuery table that contains the free text column as input if you only need to use NER or general patten matching. If you have known PHI associated with text, you can join free text with knwon phi, and create a final input table with both text and known phi in each row. 

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
```

Multiple deid actions can be grouped into same PHI category by using same __itemName__. Grouping is useful for deid quality analytics if analytic is enabled. 

Configure General Regex pattern matching, or find known PHI of the patient associated with the text. 

- general : for Phone/Fax, Email, URL, IP address, SSN
- general_number: general order, account number and general-accession
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


## Set up runtime environment

You need to configure Google Cloud credential if run TiDE on Dataflow.
https://cloud.google.com/docs/authentication/getting-started


```
export GOOGLE_APPLICATION_CREDENTIALS=<gcp service account credential json file>

```


## Run Deid jobs

TiDE was created using Apache Beam programming model. So you can run TiDE on many technolgies using appropriate runners on https://beam.apache.org/documentation/runners/capability-matrix/

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

DLP can be integrated with two ways. One way is directly enable DLP in TiDE deid transform, which will call Google DLP API individually for each text row. The second way is to use Google DLP Native job to find PHIs independently from TiDe and merge findings of each parallel result into final deied-text. 

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

### Developer Working Tips

   1. If you have code in local machine and want to push on your github account
      1. Open shell/terminal and navigate to your code folder, type git init.
      2. Now, open GitHub desktop, Click on the "Add an existing repository from your hard disk." option on the left section of your github desktop
      3. Choose your local code folder.
      4. Click add repository.
      5. A new window will appear which will have sections
         1. Left section 
            1. Where all of your changes will appear(all files will appear in case of initial upload)
            2. under that sumamry section, where you needto add your comments
            3. Under that description and then the Commit to master button
         2. Top bar section
            1. first your name of repository will come
            2. second will be your name of the remote branch
            3. Publish/push to repository.
      6. Now that you ready to push the inital commit, do it in the sequence below
         1. Select the files you want to send to your repo
         2. Add summary and description
         3. Click commit to master. 
         4. Click on the publish repository
         5. A new window will appear
            1. Select GitHub.com
            2. Name: name of the repo
            3. Description: Description of the repo
            4. if you want to keep repo private on github you can check the "Keep this code private" option
            5. Select org, if any
            6. click publish repository
            7. Once the process is completed you can check the repo on your github account with the name you have mentioned during the process above.
   2. Push new changes to existing remote repo
      1.  Click on the Current repository option on the left section
      2.  Click on the Add button adjacent filter textbox.
      3.  Again choose the code folder you are working on and select add repository.
