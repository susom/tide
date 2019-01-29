package com.github.susom.starr.deid;

import java.io.Serializable;

public class ItemStats implements Serializable {
  String anonymizerType;
  String dataSrc;
  String word;
  long count;

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
