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

import java.io.Serializable;
import org.apache.beam.sdk.options.ValueProvider;

public class DeidJob implements Serializable {
  String jobName;
  String version;
  ValueProvider<String> textFields;
  ValueProvider<String> textIdFields;
  boolean analytic = true;
  DeidSpec[] spec;
  DeidSpec[] googleDlpInfoTypes;
  DeidSpec.DateJitterMode dateJitter;
  Integer dateJitterRange;
  String dateJitterSeedField;

  boolean googleDlpEnabled;
  boolean nerEnabled;

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

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
    return spec == null ? new DeidSpec[0] : spec;
  }

  public void setSpec(DeidSpec[] spec) {
    this.spec = spec;
  }

  public ValueProvider<String> getTextFields() {
    return textFields;
  }

  public void setTextFields(ValueProvider<String> textFields) {
    this.textFields = textFields;
  }

  public ValueProvider<String> getTextIdFields() {
    return textIdFields;
  }

  public void setTextIdFields(ValueProvider<String> textIdFields) {
    this.textIdFields = textIdFields;
  }

  public DeidSpec.DateJitterMode getDateJitter() {
    return dateJitter;
  }

  public void setDateJitter(DeidSpec.DateJitterMode dateJitter) {
    this.dateJitter = dateJitter;
  }

  public Integer getDateJitterRange() {
    return dateJitterRange;
  }

  public void setDateJitterRange(Integer dateJitterRange) {
    this.dateJitterRange = dateJitterRange;
  }

  public String getDateJitterSeedField() {
    return dateJitterSeedField;
  }

  public void setDateJitterSeedField(String dateJitterSeedField) {
    this.dateJitterSeedField = dateJitterSeedField;
  }
}
