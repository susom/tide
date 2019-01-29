package com.github.susom.starr.deid;

import edu.stanford.irt.core.facade.AnonymizedItem;

public class AnonymizedItemWithReplacement extends AnonymizedItem {
  String replacement;
  String foundBy;

  public AnonymizedItemWithReplacement(String word, String anonymizerType) {
    super(word,anonymizerType);
  }

  public AnonymizedItemWithReplacement(String word, String anonymizerType, String replacement, String foundBy) {
    super(word,anonymizerType);
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
}
