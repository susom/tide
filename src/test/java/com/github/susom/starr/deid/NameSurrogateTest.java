package com.github.susom.starr.deid;

import com.github.susom.starr.deid.NameSurrogate.NameDictionay;
import edu.stanford.irt.core.facade.AnonymizedItem;
import java.io.IOException;
import java.sql.SQLException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NameSurrogateTest {

  String[] names = new String[]{"Jose","Posada","Arras","ФЭЭЭЭЭ","诚诚诚","Joe","Chris","Posada","Mary","Ed","Emma","Christopher","O'BRIEN"};
  NameDictionay[] dic = new NameDictionay[]{NameDictionay.Firstname,NameDictionay.Lastname,NameDictionay.Lastname,NameDictionay.Lastname,NameDictionay.Lastname,NameDictionay.Firstname,NameDictionay.Firstname,NameDictionay.Lastname,NameDictionay.Firstname,NameDictionay.Firstname,NameDictionay.Firstname,NameDictionay.Firstname,NameDictionay.Firstname};
  String type = "name";

  private static final Logger log = LoggerFactory.getLogger(NameSurrogateTest.class);
  @Test
  public void scrub() throws IOException, SQLException {
    String text = "\t Posada,\tJose,\tMary,\t Joe,\t Emma,\t Chris,\t Christopher,\t ФЭЭЭЭЭ,\t Ed,\t 诚诚诚,\t Hector Sausage-Hausen, O'BRIEN and Mathias d'Arras visited hospital on 11/6/2018. O'BRIEN is the key person";
    List<AnonymizedItem > items = new ArrayList<>();

    NameSurrogate ns = new NameSurrogate(names, "phi-name", dic);

    String result = ns.scrub(text, items);

//    items.forEach(i->{log.info(i.getWord());});
    log.info("scrub input:"+text);
    log.info("scrub output:"+result);
  }

  @Test
  public void getFirstNameSurrogateTest() throws IOException, SQLException {
    NameSurrogate ns = new NameSurrogate(null, "phi-name", dic);
    String name = "Jose";
    String out = ns.getFirstNameSurrogate(name);
    log.info("Surrogate input: "+ name+" => output:"+out);
  }

  @Test
  public void getLastNameSurrogateTest() throws IOException, SQLException {
    NameSurrogate ns = new NameSurrogate(null, "phi-name", dic);
    String name = "Li";
    String out = ns.getLastNameSurrogate(name);
    log.info("Surrogate input: "+ name+" => output:"+out);
  }
}
