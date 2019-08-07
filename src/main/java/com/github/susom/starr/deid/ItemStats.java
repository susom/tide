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

class ItemStats implements Serializable {
  private String anonymizerType;
  private String dataSrc;
  private String word;
  private long count;

  public String getAnonymizerType() {
    return anonymizerType;
  }

  public void setAnonymizerType(String anonymizerType) {
    this.anonymizerType = anonymizerType;
  }

  public String getDataSrc() {
    return dataSrc;
  }

  public void setDataSrc(String dataSrc) {
    this.dataSrc = dataSrc;
  }

  public String getWord() {
    return word;
  }

  public void setWord(String word) {
    this.word = word;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }
}
