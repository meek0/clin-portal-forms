package bio.ferlab.clin.portal.forms.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LabelsServiceTest {
  
  @Autowired
  private LabelsService service;
  
  @Test
  void context() {
    assertNotNull(service);
  }

  @Test
  void getLabel() {
    assertEquals("foo", service.get("CODE1", "fr"));
    assertEquals("bar", service.get("CODE2", "en"));// fr by default
    assertNull(service.get("CODE3", "fr"));
    assertNull(service.get("unknown", "fr"));
    assertNull(service.get("", "fr"));
    assertNull(service.get(null, null));
  }

}