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

package com.github.susom.starr.deid.anonymizers;

public class AnonymizedItemWithReplacement {
  String word;
  Integer start;
  Integer end;
  String type;
  String replacement;
  String foundBy;

  public AnonymizedItemWithReplacement(String word, String anonymizerType) {
    this.word = word;
    this.type = anonymizerType;
  }

  public AnonymizedItemWithReplacement(String word, int start, int end, String replacement, String foundBy, String anonymizerType) {
    this.word = word;
    this.start = start;
    this.end = end;
    this.replacement = replacement;
    this.foundBy = foundBy;
    this.type = anonymizerType;
  }

  /**
   * replace the word with replacement.
   * @param word target word
   * @param anonymizerType phi type
   * @param replacement replacement text
   * @param foundBy for tracking finding method
   */
  public AnonymizedItemWithReplacement(String word, String anonymizerType,
                                       String replacement, String foundBy) {
    this.word = word;
    this.type = anonymizerType;
    this.replacement = replacement;
    this.foundBy = foundBy;
  }

  public String getReplacement() {
    return replacement;
  }

  public void setReplacement(String replacement) {
    this.replacement = replacement;
  }

  public String getFoundBy() {
    return foundBy;
  }

  public void setFoundBy(String foundBy) {
    this.foundBy = foundBy;
  }

  public String getWord() {
    return word;
  }

  public void setWord(String word) {
    this.word = word;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Integer getStart() {
    return start;
  }

  public void setStart(Integer start) {
    this.start = start;
  }

  public Integer getEnd() {
    return end;
  }

  public void setEnd(Integer end) {
    this.end = end;
  }



  public static String applyChange(String input) {




    return null;
  }
}
