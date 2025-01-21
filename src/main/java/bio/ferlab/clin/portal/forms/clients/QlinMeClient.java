package bio.ferlab.clin.portal.forms.clients;

import bio.ferlab.clin.portal.forms.configurations.QlinMeConfiguration;
import bio.ferlab.clin.portal.forms.mappers.QlinMeMapper;
import bio.ferlab.clin.portal.forms.models.analysis.AnalysisCreateResponse;
import bio.ferlab.clin.portal.forms.models.submit.Request;
import bio.ferlab.clin.portal.forms.models.submit.Response;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class QlinMeClient {

  private final HttpClient client;
  private final ObjectMapper objectMapper;
  private final QlinMeMapper mapper;
  private final QlinMeConfiguration configuration;

  public QlinMeClient(QlinMeConfiguration configuration, QlinMeMapper mapper) {
    log.info("QlinMeClient: {}", configuration);

    var requestConfig = RequestConfig.custom()
      .setConnectTimeout(configuration.getTimeout())
      .setConnectionRequestTimeout(configuration.getTimeout())
      .setSocketTimeout(configuration.getTimeout())
      .build();

    this.client = HttpClientBuilder.create()
      .setDefaultRequestConfig(requestConfig)
      .build();

    this.objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

    this.mapper = mapper;
    this.configuration = configuration;
  }

  public ResponseEntity<?> create(String authorization, Request request, boolean isDraft) {
    HttpResponse response = null;
    try {
      var mappedRequest = mapper.mapToAnalysisCreateRequest(request);
      log.debug(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappedRequest));

      var qlinRequest = new HttpPost(configuration.getUrl()+ "/api/v1/analysis" + (isDraft ? "?draft" : ""));
      qlinRequest.addHeader(HttpHeaders.AUTHORIZATION, authorization);
      qlinRequest.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
      qlinRequest.setEntity(new ByteArrayEntity(objectMapper.writeValueAsBytes(mappedRequest)));
      response = client.execute(qlinRequest);

      var body = EntityUtils.toString(response.getEntity());
      var status = response.getStatusLine().getStatusCode();
      if (status == 201) {
        var analysis = objectMapper.readValue(body, AnalysisCreateResponse.class);
        List<Response.Patient> patients = analysis.patients().stream()
          .map(patient -> new Response.Patient(patient.patientId(), patient.familyMember()))
          .toList();
        return ResponseEntity.ok(new Response(analysis.analysisId(), patients));
      } else {
        return ResponseEntity.status(status)
          .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()).body(body);
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      Optional.ofNullable(response).map(HttpResponse::getEntity).ifPresent(EntityUtils::consumeQuietly);
    }
  }

  public ResponseEntity<?> update(String authorization, Request request, String prescriptionId, boolean isDraft) {
    HttpResponse response = null;
    try {
      if(prescriptionId == null) {
        return ResponseEntity.status(400).body("analysisId is required");
      }
      var mappedRequest = mapper.mapToAnalysisCreateRequest(request);
      log.debug(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappedRequest));

      var qlinRequest = new HttpPut(configuration.getUrl()+ "/api/v1/analysis/" + prescriptionId + (isDraft ? "?draft" : ""));
      qlinRequest.addHeader(HttpHeaders.AUTHORIZATION, authorization);
      qlinRequest.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
      qlinRequest.setEntity(new ByteArrayEntity(objectMapper.writeValueAsBytes(mappedRequest)));
      response = client.execute(qlinRequest);

      var body = EntityUtils.toString(response.getEntity());
      var status = response.getStatusLine().getStatusCode();
      if (status == 201) {
        var analysis = objectMapper.readValue(body, AnalysisCreateResponse.class);
        List<Response.Patient> patients = analysis.patients().stream()
          .map(patient -> new Response.Patient(patient.patientId(), patient.familyMember()))
          .toList();
        return ResponseEntity.ok(new Response(analysis.analysisId(), patients));
      } else {
        return ResponseEntity.status(status)
          .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()).body(body);
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      Optional.ofNullable(response).map(HttpResponse::getEntity).ifPresent(EntityUtils::consumeQuietly);
    }
  }
}
