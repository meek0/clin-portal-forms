package bio.ferlab.clin.portal.forms.clients;

import bio.ferlab.clin.portal.forms.configurations.QlinMeConfiguration;
import bio.ferlab.clin.portal.forms.mappers.QlinMeMapper;
import bio.ferlab.clin.portal.forms.models.analysis.AnalysisCreateResponse;
import bio.ferlab.clin.portal.forms.models.submit.Request;
import bio.ferlab.clin.portal.forms.models.submit.Response;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class QlinMeClient {

  private final RestClient client;
  private final ObjectMapper objectMapper;
  private final QlinMeMapper mapper;

  public QlinMeClient(QlinMeConfiguration configuration, QlinMeMapper mapper) {
    this.client = RestClient.create(configuration.getUrl());
    this.objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    this.mapper = mapper;
  }

  public ResponseEntity<?> create(String authorization, Request request) throws JsonProcessingException {
    var mappedRequest = mapper.mapToAnalysisCreateRequest(request);
    log.debug(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappedRequest));
    return client.post()
          .uri("/api/v1/analysis")
          .header(HttpHeaders.AUTHORIZATION, authorization)
          .header(HttpHeaders.CONTENT_TYPE, "application/json")
          .body(objectMapper.writeValueAsBytes(mappedRequest))
          .exchange((req, res) -> {
            if (res.getStatusCode().value() == 201) {
              // extract ID from response
              var analysis = objectMapper.readValue(res.getBody(), AnalysisCreateResponse.class);
              return ResponseEntity.status(res.getStatusCode()).body(new Response(analysis.analysisId()));
            } else {
              // anything else we forward the response as is
              var body = String.join( "\n", IOUtils.readLines(res.getBody()));
              return ResponseEntity.status(res.getStatusCode()).header(HttpHeaders.CONTENT_TYPE, "application/json").body(body);
            }
          });
  }
}
