# clin-portal-forms

This API provides a bridge between the front-end and FHIR Server for **Prescriptions** creation and validation using FHIR Client library.

# Endpoints

Note: the RPT token needs to contain an attribute **fhir_practitioner_id** equals the FHIR id of the current user practitioner.

## GET /form/`panel-code`?`lang`=fr

### Parameters
|Name|Required|Type|Description|
|---|---|---|---|
|`panel-code`|true|String|possible values: [RHAB, MITN, MYOC, MMG, MYAC, HYPM, RGDI, DYSM]| 
|`lang`|false|String|| 
### Response

```json
{
    "config": {
        "prescribing_institutions": [
            {
                "name": "CHUS",
                "value": "CHUS"
            }
        ],
        "clinical_signs": {
            "default_list": [
                {
                    "name": "Neonatal hypotonia",
                    "value": "HP:0001319"
                },
                ...
            ],
            "onset_age": [
                {
                    "name": "Apparition pédiatrique (<= 15 ans)",
                    "value": "HP:0410280"
                },
                ...
            ]
        },
        "paraclinical_exams": {
            "default_list": [
                {
                    "name": "Créatine kinase sérique",
                    "value": "CKIN",
                    "extra": {
                        "type": "string",
                        "options": null
                    }
                },
                {
                    "name": "Biopsie musculaire",
                    "value": "BMUS",
                    "extra": {
                        "type": "multi_select",
                        "options": [
                            {
                                "name": "Expression anormale des protéines des fibres musculaires",
                                "value": "HP:0030089"
                            },
                            ...
                        ]
                    }
                }
                ...
            ]
        },
        "history_and_diagnosis": {
            "parental_links": [
                {
                    "name": "Membre de la branche maternelle",
                    "value": "MATMEM"
                },
                {
                    "name": "Membre de la branche paternelle",
                    "value": "PATMEM"
                }
            ],
            "ethnicities": [
                {
                    "name": "Canadien Francais",
                    "value": "CA-FR"
                },
                ...
            ]
        }
    }
}
```
## POST /form

### Body
```json
{
    "analyse": {
        "panel_code": "MMG",
        "is_reflex": false,
        "observation": "free comment observation",
        "investigation": "free comment investigation",
        "indication": "free comment indication",
        "resident_supervisor": "PRR00102",
        "comment": "free general comment"
    },
    "patient": {
        "ep": "CHUSJ",
        "ramq": "RAMQTEST",
        "mrn": "MRNTEST",
        "first_name": "Creation",
        "last_name": "Prescription",
        "birth_date": "20/07/1990",
        "gender": "male",
        "ethnicity": "LAT-AM"
    },
    "clinical_signs": [
        {
            "value": "HP:0001319",
            "is_observed": true,
            "age_code": "HP:0410280"
        },
        {
            "value": "HP:0002194",
            "is_observed": false,
            "age_code": "HP:0003593"
        }
    ],
    "paraclinical_exams": [
        {
            "code": "CKIN",
            "interpretation": "abnormal",
            "value": "comment text"
        },
        {
            "code": "GCNR",
            "interpretation": "normal"
        },
        {
            "code": "EMG",
            "interpretation": "abnormal",
            "values": [
                "HP:0030006",
                "HP:0030000"
            ]
        }
    ]
}
```
### Response

`201 created`


# Security

The API checks RPT token in request header `Authorization` public certificate and fields inside like: expiration, audience, issuer ... 

If valid, the token is then forward to FHIR Server which will validate the authenticity again and access rights of the user.

# Health check

The API has 3 public endpoints available for k8s

```
/actuator/health
/actuator/health/liveness
/actuator/health/readyness
```

# FHIR Client library

Return of experience using FHIR Client library instead of OpenFeign to perform the queries between the API and FHIR Server.

## Advantage
- All the FHIR model is available, same as the one used in FHIR Server
- FHIR Client is easy to setup with the server url + provide some feature such as retry
- Lot of different ways to create queries, from the generic, to custom using interfaces and builder like
- Be able to ask FHIR Server for resource validation

## Performances considerations

- **Use Bundle to reduce data exchange between the API and FHIR when possible, one query to get/post/put all the data**
- Some FHIR data such as CodeSystem/ValueSet can be huge, using a cache could help reduce data exchange

## Documentation
- [Official FHIR Client Doc.](https://hapifhir.io/hapi-fhir/docs/client/introduction.html)
- [Example of custom queries](https://github.com/hapifhir/hapi-fhir/blob/master/hapi-fhir-structures-r4/src/test/java/ca/uhn/fhir/rest/client/ITestClient.java)
