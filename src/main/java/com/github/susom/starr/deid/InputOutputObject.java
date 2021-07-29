package com.github.susom.starr.deid;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class InputOutputObject {
@SerializedName("FINDING_CNT_note")
  @Expose
  private Integer fINDINGCNTNote;
  @SerializedName("note")
  @Expose
  private String note;
  @SerializedName("FINDING_note")
  @Expose
  private String fINDINGNote;
  @SerializedName("TEXT_DEID_note")
  @Expose
  private String tEXTDEIDNote;
  @SerializedName("id")
  @Expose
  private String id;

  /**
   * No args constructor for use in serialization
   *
   */
  public InputOutputObject() {
  }

  /**
   *
   * @param id
   * @param note
   */
  public InputOutputObject(String id, String note) {
    super();
    this.id = id;
    this.note = note;
  }

  /**
   *
   * @param fINDINGCNTTranscription
   * @param transcription
   * @param fINDINGTranscription
   * @param tEXTDEIDTranscription
   * @param id
   */
  public InputOutputObject(Integer fINDINGCNTNote, String note, String fINDINGNote, String tEXTDEIDNote, String id) {
    super();
    this.fINDINGCNTNote = fINDINGCNTNote;
    this.note = note;
    this.fINDINGNote = fINDINGNote;
    this.tEXTDEIDNote = tEXTDEIDNote;
    this.id = id;
  }

  public Integer getFINDINGCNTNote() {
    return fINDINGCNTNote;
  }

  public void setFINDINGCNTNote(Integer fINDINGCNTNote) {
    this.fINDINGCNTNote = fINDINGCNTNote;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getFINDINGNote() {
    return fINDINGNote;
  }

  public void setFINDINGNote(String fINDINGNote) {
    this.fINDINGNote = fINDINGNote;
  }

  public String getTEXTDEIDNote() {
    return tEXTDEIDNote;
  }

  public void setTEXTDEIDNote(String tEXTDEIDNote) {
    this.tEXTDEIDNote = tEXTDEIDNote;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
