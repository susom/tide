/*
 * Copyright 2019 The Board of Trustees of The Leland Stanford Junior University.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.github.susom.starr.deid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions.Builder;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.privacy.dlp.v2.Action;
import com.google.privacy.dlp.v2.Action.SaveFindings;
import com.google.privacy.dlp.v2.BigQueryOptions;
import com.google.privacy.dlp.v2.BigQueryTable;
import com.google.privacy.dlp.v2.CreateDlpJobRequest;
import com.google.privacy.dlp.v2.DlpJob;
import com.google.privacy.dlp.v2.DlpJob.JobState;
import com.google.privacy.dlp.v2.FieldId;
import com.google.privacy.dlp.v2.GetDlpJobRequest;
import com.google.privacy.dlp.v2.InfoType;
import com.google.privacy.dlp.v2.InspectConfig;
import com.google.privacy.dlp.v2.InspectJobConfig;
import com.google.privacy.dlp.v2.Likelihood;
import com.google.privacy.dlp.v2.OutputStorageConfig;
import com.google.privacy.dlp.v2.ProjectName;
import com.google.privacy.dlp.v2.StorageConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DlpNativeRunner {
  public static final String OPTION_NAME_GCP_CREDENTIALS_KEY_FILE = "gcpCredentialsKeyFile";
  public static final String OPTION_NAME_PROJECT_ID               = "projectId";
  public static final String OPTION_CONFIG_FILE                   = "deidConfigFile";
  public static final String OPTION_INPUT_BQ_TABLE                = "inputBqTableId";
  public static final String OPTION_OUTPUT_BQ_TABLE                = "outputBqTableId";
  public static final String OPTION_ID_FIELDS                     = "idFields";
  public static final String OPTION_INSPECT_FIELDS                = "inspectFields";

  private String projectId;
  private String gcpCredentialsKeyFile;
  private String deidConfigFile;
  private String inputBqTable;
  private String outputBqTable;
  private String idFields;
  private String inspectFields;

  private HashSet<String> fieldsToKeep = new HashSet<>();

  private String jobId = UUID.randomUUID().toString();

  final List<InfoType> infoTypes = new ArrayList<>();
  final Likelihood minLikelihood = Likelihood.LIKELY;
  final boolean includeQuote = true;

  private static final Logger log = LoggerFactory.getLogger(DlpNativeRunner.class);

  public static void main(String[] args) throws IOException,InterruptedException {
    new DlpNativeRunner().run(args);
  }

  private void run(String[] args) throws IOException, InterruptedException {

    final OptionParser optionParser = new OptionParser();
    final OptionSpec<String> serviceAccountKeyFileOption =
        optionParser.accepts(OPTION_NAME_GCP_CREDENTIALS_KEY_FILE)
        .withRequiredArg().required().ofType(String.class);
    final OptionSpec<String> projectIdOption = optionParser.accepts(OPTION_NAME_PROJECT_ID)
        .withRequiredArg().ofType(String.class);
    final OptionSpec<String> deidConfigOption = optionParser.accepts(OPTION_CONFIG_FILE)
        .withRequiredArg().ofType(String.class);
    final OptionSpec<String> inputBqTableOption = optionParser.accepts(OPTION_INPUT_BQ_TABLE)
        .withRequiredArg().ofType(String.class);
    final OptionSpec<String> outputBqTableOption = optionParser.accepts(OPTION_OUTPUT_BQ_TABLE)
        .withRequiredArg().ofType(String.class);;
    final OptionSpec<String> idFieldsOption = optionParser.accepts(OPTION_ID_FIELDS)
        .withRequiredArg().ofType(String.class);
    final OptionSpec<String> inspectFieldsOption = optionParser.accepts(OPTION_INSPECT_FIELDS)
        .withRequiredArg().ofType(String.class);

    OptionSet cmdLineOptions = optionParser.parse(args);

    gcpCredentialsKeyFile = cmdLineOptions.valueOf(serviceAccountKeyFileOption);
    GoogleCredentials credentials = null;
    try (FileInputStream serviceAccountStream =
        new FileInputStream(new File(gcpCredentialsKeyFile))) {
      credentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
    } catch (IOException e) {
      log.error("Google Credential is invalid or missing");
    }

    inputBqTable = cmdLineOptions.valueOf(inputBqTableOption);
    String[] inputTableIdParts = inputBqTable.split("\\.|\\:");
    TableId inputTableId = TableId.of(
        inputTableIdParts[0], inputTableIdParts[1], inputTableIdParts[2]);
    Builder bqBuilder = com.google.cloud.bigquery.BigQueryOptions
        .newBuilder()
        .setCredentials(credentials);

    bqBuilder.setProjectId(projectId);

    BigQuery bigQuery = bqBuilder.build().getService();

    final Table inputTable = bigQuery.getTable(inputTableId);

    inspectFields = cmdLineOptions.valueOf(inspectFieldsOption);
    for (String id : inspectFields.split(" |,")) {
      fieldsToKeep.add(id);
    }

    idFields = cmdLineOptions.valueOf(idFieldsOption);
    BigQueryOptions.Builder bigQueryOptionsBuilder = BigQueryOptions.newBuilder();
    for (String id : idFields.split(" |,")) {
      bigQueryOptionsBuilder.addIdentifyingFields(FieldId.newBuilder().setName(id).build());
      fieldsToKeep.add(id);
    }


    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    deidConfigFile = cmdLineOptions.valueOf(deidConfigOption);
    final DeidJobs jobs = mapper.readValue(this.getClass().getClassLoader()
        .getResourceAsStream(deidConfigFile), DeidJobs.class);

    projectId = cmdLineOptions.valueOf(projectIdOption);
    CreateDlpJobRequest.Builder jobBuilder = CreateDlpJobRequest.newBuilder();
    jobBuilder.setParent(ProjectName.of(projectId).toString());
    jobBuilder.setJobId(jobId);

    for (DeidSpec spec : jobs.deidJobs[0].getGoogleDlpInfoTypes()) {
      for (String f : spec.infoTypes) {
        f = f.trim().toUpperCase(Locale.ROOT);
        if (f.length() > 0) {
          infoTypes.add(InfoType.newBuilder().setName(f).build());
          log.info("added infoType: " +  f + " to   " + spec.itemName);
        }
      }
    }

    final InspectConfig inspectConfig =
        InspectConfig.newBuilder()
          .addAllInfoTypes(infoTypes)
          .setMinLikelihood(minLikelihood)
          .setIncludeQuote(includeQuote)
          .build();

    final BigQueryTable inputBigQueryTable =
        BigQueryTable.newBuilder()
          .setProjectId(inputTableIdParts[0])
          .setDatasetId(inputTableIdParts[1])
          .setTableId(inputTableIdParts[2]).build();

    bigQueryOptionsBuilder.setTableReference(inputBigQueryTable);

    FieldList inputFieldList = inputTable.getDefinition().getSchema().getFields();
    for (Field f : inputFieldList) {
      if (!fieldsToKeep.contains(f.getName())) {
        bigQueryOptionsBuilder.addExcludedFields(FieldId.newBuilder().setName(f.getName()).build());
      }
    }

    outputBqTable = cmdLineOptions.valueOf(outputBqTableOption);
    String[] outputTableIdParts = outputBqTable.split("\\.|\\:");

    BigQueryTable outputBigQueryTable =
        BigQueryTable.newBuilder()
          .setProjectId(outputTableIdParts[0])
          .setDatasetId(outputTableIdParts[1])
          .setTableId(outputTableIdParts[2]).build();

    Action.Builder actionBuilder = Action.newBuilder()
        .setSaveFindings(
          SaveFindings.newBuilder()
            .setOutputConfig(
              OutputStorageConfig.newBuilder()
                .setTable(outputBigQueryTable).build()));

    StorageConfig storageConfig =
        StorageConfig.newBuilder()
        .setBigQueryOptions(bigQueryOptionsBuilder.build())
        .build();

    InspectJobConfig inspectJobConfig =
        InspectJobConfig.newBuilder()
          .setInspectConfig(inspectConfig)
          .setStorageConfig(storageConfig)
          .addActions(actionBuilder.build())
          .build();

    try (DlpServiceClient dlpServiceClient = DlpServiceClient.create()) {

      CreateDlpJobRequest createDlpJobRequest = CreateDlpJobRequest.newBuilder()
          .setInspectJob(inspectJobConfig)
          .setParent(ProjectName.of(projectId).toString())
          .build();

      DlpJob job = dlpServiceClient.createDlpJob(createDlpJobRequest);

      log.info("DlpJob submitted:{}", job.getName());

      GetDlpJobRequest getDlpJobRequest =
          GetDlpJobRequest.newBuilder().setName(job.getName()).build();
      DlpJob jobStatus = dlpServiceClient.getDlpJob(getDlpJobRequest);
      int logCnt = 0;
      int resultCode = jobStatus.getStateValue();
      while (resultCode == JobState.PENDING.getNumber()
        || resultCode == JobState.RUNNING.getNumber()
        || resultCode == JobState.JOB_STATE_UNSPECIFIED.getNumber()) {

        logCnt ++;
        Thread.sleep(20000);

        jobStatus = dlpServiceClient.getDlpJob(getDlpJobRequest);
        if (resultCode != jobStatus.getStateValue() || logCnt == 100) {
          logCnt = 0;
          log.info("DlpJob State:{}", jobStatus.getStateValue());
        }
        resultCode = jobStatus.getStateValue();
      }
    }
  }
}
