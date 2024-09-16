package bio.ferlab.clin.portal.forms.mappers;

import bio.ferlab.clin.portal.forms.JsonTestUtils;
import bio.ferlab.clin.portal.forms.models.submit.Request;
import org.junit.jupiter.api.Test;

class QlinMeMapperTest {

  private final QlinMeMapper mapper = new QlinMeMapper();

  @Test
  void mapToAnalysisCreateRequest_01() {
    var uiRequest = JsonTestUtils.loadJsonResource("qlin-me/ui_request_01.json", Request.class);
    JsonTestUtils.assertJson("qlin-me/mapped_request_01.json", mapper.mapToAnalysisCreateRequest(uiRequest));
  }

}
