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
import java.util.HashMap;

import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.MapCoder;

@DefaultCoder(MapCoder.class)
class DeidResult extends HashMap implements Serializable {

  private final String[] idFields;
  private String[] textFields;

  /**
   * constructor for DeidResult.
   * @param idFields array of id field names
   * @param idValues array of id values, in the same order as idFields
   * @param textFields array of text field names
   */
  public DeidResult(String[] idFields, Object[] idValues, String[] textFields) {
    super();
    this.idFields = idFields;
    this.textFields = textFields;

    if (idFields.length != idValues.length) {
      throw new IllegalStateException("id field and value counts do not match");
    }

    for (int i = 0; i < idFields.length; i++) {
      String idField = idFields[i];
      super.put(idField, idValues[i]);
    }

  }

  public void addData(String key, Object value) {
    super.put(key,value);
  }

  /**
   * all data are store as string at this moment.
   * @param key data map key
   * @return string value mapped to the provided key
   * @throws IllegalAccessException when data stored is not string, which should not happen
   */
  public String getDataAsString(String key) throws IllegalAccessException {
    Object v = super.get(key);

    if (v == null) {
      return null;
    }

    if (v instanceof String) {
      return (String) v;
    } else {
      throw new java.lang.IllegalAccessException("requested value is not String type");
    }

  }

  public String[] getTextFields() {
    return textFields;
  }

  public void setTextFields(String[] textFields) {
    this.textFields = textFields;
  }

}
