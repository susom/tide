package com.github.susom.starr.deid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.privacy.dlp.v2.InspectResult;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DlpTransformTest {

  String text = ">>>>>>>>>>>>> more tests: date test Jan 01, 2018, ssn: 874-98-5739, birth day: 2003-09-18, passport: 56521368, pp2: 56985631 Sapphire Team: Deepa, Tom, Olson, and Mr. Olson,  Mesterhazy, Shenoy, Malunjkar, Somalee Datta, Li, Wencheng Li,  and Balraj <<<<<* * * HISTORICAL DOCUMENT CONVERTED FROM CERNER * * * PATIENT: [patient_name], [patient_name] MRN: [MRN] ACCOUNT #: [MRN] DOB: 05_26_2008[JITTERED] DATE OF SERVICE: 11_20_2009[JITTERED] PREOPERATIVE DIAGNOSIS: Left Wilms tumor. POSTOPERATIVE DIAGNOSIS: Left Wilms tumor with hilar nodal metastasis with rupture and involvement of the left hemidiaphragm. PROCEDURE: Insertion of Broviac catheter; radical left nephrectomy with segmental resection of diaphragm and resection of periaortic nodes. PRIMARY SURGEON: Gary Hartman, MD; Matias Bruzoni, MD ANESTHESIA: General. ANESTHESIOLOGIST: Michael Chen, MD PERTINENT HISTORY AND INDICATIONS: This 17-month-old boy with a large mass in the left kidney with no evidence of lung disease or liver metastasis was prepared for operation. The parents understand the nature of the procedure, risks, and indications and request to be performed. FINDINGS: Large upper pole tumor with involvement of hilar nodes and segmental involvement of the posterior aspect of the diaphragm; rupture of the hilar nodes immediately preceding removal of the tumor during ligation of hilar vessels. PROCEDURE IN DETAIL: With the patient in supine position under general anesthesia the neck, chest and abdomen were prepped and draped in the usual sterile fashion. The left subclavian vein was accessed with a micropuncture technique. The wire was exchanged and a 6.6 French Broviac catheter was tunneled from an anterior chest incision. It was cut to length. The introducer was advanced under fluoroscopic control. The catheter was then advanced and confirmed to be at the cavoatrial junction. It was then sutured in place with 4-0 Vicryl and the subclavian puncture site was closed with a similar stitch. Dry sterile dressing was applied. Attention was then turned to the abdomen. An upper abdominal transverse thoracoabdominal shaped incision was carried through the skin, subcutaneous tissue, and muscle layers were opened the length of the skin incision, but not extending into the chest. The mass was very large and extended from the costal margin to the pubic bone. Dissection was begun by mobilizing the colon and retracting it medially. The spleen was densely adherent and the pancreas and spleen were gradually mobilized off of the tumor and rotated mesially. The inferior pole revealed the remnant normal kidney and the gonadal vessels were divided, as was the ureter. The mass was then gradually mobilized circumferentially. To mobilize the upper pole the involvement in the diaphragm required a segmental resection of the posterior aspect of the diaphragm. Upon completing this, the mass was then able to be mobilized medially and dissection exposed the aorta and the renal artery. There was gross hilar nodal disease in the hilum that made exposure of the hilar vessels difficult and therefore the need for exposing the aorta and tracing the renal artery in that direction. During the retraction of the hilar nodes and the mass, the hilar nodes disintegrated and there was local rupture. The artery and vein were then quickly ligated and the mass in total was removed. Additional specimens of periaortic lymph nodes were then obtained and the renal artery and vein were then suture ligated with 3-0 silk tie suture ligatures. After the completion of that there was no gross residual disease. The defect in the diaphragm was closed with interrupted 2-0 Nurolon figure-of-eight sutures and the pleural cavity was aspirated with a red rubber catheter prior to closing the last suture. The defect in the mesocolon was then closed with 4-0 Vicryl. The perimeter of the original tumor bed was then clipped with titanium clips. The spleen and colon were replaced in normal anatomic position and the small bowel was returned to the abdomen. The muscle layers were then closed with interrupted 2-0 Nurolon figure-of-eight sutures; subcutaneous fascia with 4-0 Vicryl and the skin with 4-0 chromic. ESTIMATED BLOOD LOSS: 50 cc SPONGE AND NEEDLE COUNTS: Correct. Dry sterile dressing is applied. The patient tolerated the procedure well and left the operating room to recovery in good condition. {END} D: 11_20_2009[JITTERED] T: 11_21_2009[JITTERED] I: 11_21_2009[JITTERED] R: 11_28_2009[JITTERED] sb DOC #: 518347 DICT JOB #: 000161006 Electronically signed on 11_28_2009[JITTERED] __________________________________________________________ Gary E. Hartman MD Electronically signed on 11_29_2009[JITTERED] ___________________________________________________________ Matias Bruzoni , MD";

  DeidJob job;

  private static final Logger log = LoggerFactory.getLogger(DlpTransformTest.class);
  @Before
  public void setUp() throws Exception {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    DeidJobs jobs = mapper.readValue(this.getClass().getClassLoader()
      .getResourceAsStream("deid_test_config.yaml"), DeidJobs.class);

    job = jobs.deid_jobs[0];
  }

  @Test
  public void dlpInspectRequest() throws IOException {
    DeidResult.DeidText deidResult = new DeidResult.DeidText(new String[]{"0001"}, text, "");
    InspectResult result = new DlpTransform(job, "som-rit-phi-starr-miner-dev").dlpInspectRequest(text,deidResult,null);
    log.info("TextStage1:");
    log.info(deidResult.getTextStage1());
    log.info("TextStage2:");
    log.info(deidResult.getTextStage2());
  }


  @Test
  public void deidRequest() throws IOException {

    DeidResult.DeidText deidResult = new DeidResult.DeidText(new String[]{"0001"}, text, "");

    new DlpTransform(job, "som-rit-phi-starr-miner-dev").dlpDeidRequest(text, deidResult);
    return;
  }


}
