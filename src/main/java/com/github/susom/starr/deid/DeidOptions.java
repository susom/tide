package com.github.susom.starr.deid;

import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.options.ValueProvider;

@SuppressWarnings("java:S125")
public interface DeidOptions extends PipelineOptions {
    @Description("Set GCP credentials key file if using DataflowRunner.")
    @Default.String("")
    String getGcpCredentialsKeyFile();

    void setGcpCredentialsKeyFile(String value);

    /*
      Tide Open source change - following is temporarily commented
      void setGcpCredential(Credentials value);
    */

    @Description("Namen of the Deid configuration, the default is deid_test_config.yaml")
    @Default.String("deid_config_clarity.yaml")
    String getDeidConfigFile();

    void setDeidConfigFile(String value);

    /*
      @Description("input file type, optional")
      @Default.String("TXT")
      ValueProvider<String> getInputFileType();
      void setInputFileType(ValueProvider<String> value);
    */

    @Description("Path of the file to read from")
    @Validation.Required
    ValueProvider<String> getInputResource();

    void setInputResource(ValueProvider<String> value);

    @Description("Path of the file to save to")
    @Validation.Required
    ValueProvider<String> getOutputResource();

    void setOutputResource(ValueProvider<String> value);

    @Description("type of the input resouce. gcp_gcs | gcp_bq | local | txt")
    @Default.String("gcp_gcs")
    String getInputType();

    void setInputType(String value);

    @Description("The Google project id that DLP API will be called, optional.")
    @Default.String("")
    String getDlpProject();

    void setDlpProject(String value);

    @Description("Turn on/off Google DLP")
    String getGoogleDlpEnabled();

    void setGoogleDlpEnabled(String value);

    @Description("override text field, optional.")
    ValueProvider<String> getTextInputFields();

    void setTextInputFields(ValueProvider<String> value);

    @Description("override text-id field, optional.")
    ValueProvider<String> getTextIdFields();

    void setTextIdFields(ValueProvider<String> value);

    @Description("phi-info-resource, typically line delimited json file, one line per note, note filename should identify the record in this file, optional.")
    ValueProvider<String> getPhiInfoResource();

    void setPhiInfoResource(ValueProvider<String> value);

    @Description("Path of the PHI file to read from")
    @Validation.Required
    ValueProvider<String> getPhiFileName();

    void setPhiFileName(ValueProvider<String> value);

    void setOrignalOutputPath(String value);
    String getOrignalOutputPath();

}
