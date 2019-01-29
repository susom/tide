package com.github.susom.starr.deid;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.susom.starr.Utility;
import com.github.susom.starr.deid.DeidTransform.DeidFn;
import com.github.susom.starr.deid.LocationSurrogate.Address;
import edu.stanford.irt.core.facade.AnonymizedItem;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocationSurrogateTest {
  String[] textArray = new String[]{
//    "Pearly is a 36 year old Caucasian female from Redwood City, California who is being seen in this clinic for outpatient management of significant psychological challenges that include depression, anxiety and intermittent borderline personality features that exacerbate her experience of chronic pain (i.e, fibromyalgia; irritable bowel symptomatology; and headaches). MEDICATIONS: Prescriptions:  - baclofen (baclofen 10 mg oral tablet), 1 1/2 tab po tid prn pain as needed for muscle pain, Li MD, MPH, Meredith R   - clonazePAM (clonazepam 0.5 mg oral tablet), 1/2 tab by mouth 2 times a day 1/28 decreased to 1/2 tab (0.25 mg) po bid, Mike [ Palo Alto, CA 98739             ] topical (lidocaine 5% topical ointment), Apply to affected areas 4 times daily as needed for pain., Jose MD, MPH, Meredith R   - lidocaine topical (lidocaine 2% topical gel with applicator), apply to the affected area as needed, Kate MD, [ 123 Porter Ave, Palo Alto, CA 89093           ] (ondansetron 8 mg oral tablet, disintegrating), 1 tab by mouth 3 times a day as needed for for nausea/vomiting, John MD, Mary ",
    "Dear Dr. Aure,    I had the pleasure of seeing your patient Quentin Augostini in the cardiology clinic today.    LOCATION OF OUTPATIENT CONSULTATION: San Ramon pediatric cardiology office. 730 Ezra RD)    730 Ezra Rd  San Jose CA  94304-1503 and San Ramon in US"
  };

  DeidJob job;


  @Before
  public void setUp() throws Exception {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    DeidJobs jobs = mapper.readValue(this.getClass().getClassLoader()
      .getResourceAsStream("deid_test_config.yaml"), DeidJobs.class);

    job = jobs.deid_jobs[0];
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void scrub() throws IOException, ClassNotFoundException, SQLException {


    List<AnonymizedItem > items = new ArrayList<>();
    Address[] knownAddr = new Address[]{ new Address("Porter", "room 2", "123", "Ave", "Palo Alto","89093", "CA"), new Address("Stanford","building Y", "456", "Ave", "Palo Alto", "98739", "CA")};

    for(String text : textArray){

      List<AnonymizedItem> foundLocationItems = new ArrayList<>();
      List<AnonymizedItem> foundNameItems  = new ArrayList<>();

      DeidTransform.findEntiesWithNer(text, foundNameItems, foundLocationItems);

      LocationSurrogate locationSurrogate = new LocationSurrogate(knownAddr, "location",
        foundLocationItems, true);
      String result = locationSurrogate.scrub(text, items);
      System.out.println("INPUT:" + text);
      System.out.println("OUTPUT:" + result);

    }

  }

  @Test
  public void getSimpleLocationRegex() {
    Address[] knownAddr = new Address[]{ new Address("Porter", "room 2", "123", "Ave", "Palo Alto","89093", "CA"), new Address("Stanford","building Y", "", "Ave", "Palo+Alto", "98739", "CA")};

    for(String text : textArray){
      for(Address address : knownAddr){
        String patternStr= LocationSurrogate.getSimpleLocationRegex(address);
        System.out.println("patternStr:" + patternStr);

        System.out.println("SIMPLE REGEX INPUT:" + text);
        System.out.println("SIMPLE REGEX OUTPUT:" + text.replaceAll(patternStr, "[replaced]"));

      }

    }


  }
}
