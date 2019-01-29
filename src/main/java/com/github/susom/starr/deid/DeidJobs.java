package com.github.susom.starr.deid;


import java.io.Serializable;

/**
 * handling job configuration
 * @author wenchengl
 */

public class DeidJobs implements Serializable {
  DeidJob[] deid_jobs;
  String name;

  public DeidJob[] getDeid_jobs() {
    return deid_jobs;
  }

  public void setDeid_jobs(DeidJob[] deid_jobs) {
    this.deid_jobs = deid_jobs;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}


class DeidJob implements Serializable{
  String job_name;
  String version;
  String text_field;
  String text_id_field;
  boolean analytic;
  DeidSpec[] spec;
  DeidSpec[] googleDlpInfoTypes;
  DateJitterMode date_jitter;
  Integer date_jitter_range;
  String date_jitter_seed_field;

  boolean googleDlpEnabled;
  boolean nerEnabled;

  public DeidSpec[] getGoogleDlpInfoTypes() {
    return googleDlpInfoTypes;
  }

  public void setGoogleDlpInfoTypes(DeidSpec[] googleDlpInfoTypes) {
    this.googleDlpInfoTypes = googleDlpInfoTypes;
  }

  public boolean isGoogleDlpEnabled() {
    return googleDlpEnabled;
  }

  public void setGoogleDlpEnabled(boolean googleDlpEnabled) {
    this.googleDlpEnabled = googleDlpEnabled;
  }

  public boolean isNerEnabled() {
    return nerEnabled;
  }

  public void setNerEnabled(boolean nerEnabled) {
    this.nerEnabled = nerEnabled;
  }

  public String getJob_name() {
    return job_name;
  }

  public void setJob_name(String job_name) {
    this.job_name = job_name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public boolean isAnalytic() {
    return analytic;
  }

  public void setAnalytic(boolean analytic) {
    this.analytic = analytic;
  }

  public DeidSpec[] getSpec() {
    return spec;
  }

  public void setSpec(DeidSpec[] spec) {
    this.spec = spec;
  }

  public DateJitterMode getDate_jitter() {
    return date_jitter;
  }

  public void setDate_jitter(DateJitterMode date_jitter) {
    this.date_jitter = date_jitter;
  }

  public String getText_field() {
    return text_field;
  }

  public void setText_field(String text_field) {
    this.text_field = text_field;
  }

  public String getText_id_field() {
    return text_id_field;
  }

  public void setText_id_field(String text_id_field) {
    this.text_id_field = text_id_field;
  }

  public Integer getDate_jitter_range() {
    return date_jitter_range;
  }

  public void setDate_jitter_range(Integer date_jitter_range) {
    this.date_jitter_range = date_jitter_range;
  }

  public String getDate_jitter_seed_field() {
    return date_jitter_seed_field;
  }

  public void setDate_jitter_seed_field(String date_jitter_seed_field) {
    this.date_jitter_seed_field = date_jitter_seed_field;
  }
}


enum ResourceType {
  gcp_bq,
  gcp_gcs,
  db_sql,
  local
}

