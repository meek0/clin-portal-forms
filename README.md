# clin-portal-forms

This API provides a bridge between the front-end and FHIR Server for **Prescriptions** creation and validation using FHIR Client library.

## Security

The API checks RPT token in request header `Authorization` and public fields inside like: expiration, audience, issuer ... If valid, the token is then forward to FHIR Server which will validate the authenticity and access rights of the user with the token provider.

## Health check

The API has 3 public endpoints available for k8s

```
/actuator/health
/actuator/health/liveness
/actuator/health/readyness
```

## FHIR Client library

Return of experience using FHIR Client library instead of OpenFeign to perform the queries between the API and FHIR Server.

### Advantage
- All the FHIR model is available, same as the one used in FHIR Server
- FHIR Client is easy to setup with the server url
- Lot of different ways to create queries, from the generic, to custom using interfaces and builder like
- Be able to ask FHIR Server for resource validation

### Performances considerations

- You can't perform `CompletableFuture.supplyAsync(() -> fhirClient.xxxx(...), es);` on FHIR client because `FhirAuthInterceptor` is bound to current thread and won't find the request `Authorization`
- **Use Bundle GET to reduce data exchange between the API and FHIR when possible, one query to get all the information**
- Define you custom FHIR queries in IClinFhirClient, be aware they can't be async and if cumulated they will reduce the API response time

### Documentation
- [Official FHIR Client Doc.](https://hapifhir.io/hapi-fhir/docs/client/introduction.html)
- [Example of custom queries](https://github.com/hapifhir/hapi-fhir/blob/master/hapi-fhir-structures-r4/src/test/java/ca/uhn/fhir/rest/client/ITestClient.java)
