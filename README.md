# clin-portal-forms

This API provides a bridge between the front-end and FHIR Server for **Prescriptions** creation and validation using FHIR Client library.

# DevTools

It's recommended to enable **Devtools (auto reload) for IntelliJ** in settings.

- Compiler > Build project automatically
- Advanced Settings > Allow auto-make to start even if app is running

Modifying any source files, including the ones in `templates/` will trigger a reload.

# Security

The API checks **RPT token** in request header `Authorization` public certificate and fields inside like: expiration, audience, issuer ... 

If valid, the token is then forward to FHIR Server which will validate the authenticity again and access rights of the user.

Note: the RPT token needs to contain an attribute **fhir_practitioner_id** equals the FHIR id of the current practitioner.

# Health check

The API has 3 public endpoints available for k8s.

```
/actuator/health
/actuator/health/liveness
/actuator/health/readyness
```

# Status

When enabled gives details about the running API.
```
/actuator/status
```
*Requires a clin-system token*

# FHIR Client library

Return of experience using FHIR Client library instead of OpenFeign to perform the queries between the API and FHIR Server.

## Advantage
- All the FHIR model is available, same as the one used in FHIR Server
- FHIR Client is easy to setup with the server url + provide some feature such as retry
- Lot of different ways to create queries, from the generic, to custom using interfaces and builder like
- Be able to ask FHIR Server for resource validation

## Performances considerations

- **Use Bundle to reduce data exchange between the API and FHIR when possible, one query to get/post/put all the data**
- Some FHIR data such as CodeSystem/ValueSet can be huge, using a cache will help reduce data exchange

## Documentation
- [Official FHIR Client Doc.](https://hapifhir.io/hapi-fhir/docs/client/introduction.html)
- [Example of custom queries](https://github.com/hapifhir/hapi-fhir/blob/master/hapi-fhir-structures-r4/src/test/java/ca/uhn/fhir/rest/client/ITestClient.java)

# Examples

Find bellow some endpoints call examples

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
                        "label": "Saisir un texte"
                    }
                },
                {
                    "name": "Biopsie musculaire",
                    "value": "BMUS",
                    "extra": {
                        "type": "multi_select",
                        "label": "Selectionner un ou plusieurs élements",
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
  "analysis": {
    "panel_code": "MMG",
    "is_reflex": false,
    "resident_supervisor": "PRR00102",
    "comment": "general comment"
  },
  "patient": {
    "ep": "CHUSJ",
    "ramq": "RAMQ12341236",
    "mrn": "MRN001",
    "first_name": "Creation",
    "last_name": "Prescription",
    "birth_date": "1990-07-20",
    "gender": "male"
  },
  "clinical_signs": {
    "signs": [
      {
        "value": "HP:0001319",
        "is_observed": true,
        "age_code": "HP:0410280"
      },
      {
        "value": "HP:0002194",
        "is_observed": false
      }
    ],
    "comment": "comment observation"
  },
  "paraclinical_exams": {
    "exams": [
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
        "values": ["HP:0030006", "HP:0030000"]
      }
    ],
    "comment": "comment investigation"
  },
  "history_and_diagnosis": {
    "diagnostic_hypothesis": "comment indication",
    "ethnicity": "LAT-AM",
    "inbreeding": true,
    "health_conditions": [
      {
        "condition": "a health condition",
        "parental_link": "MATMEM"
      }
    ]
  }
}
```
### New born body example

For new borns, `mrn` and `ramq` can be empty in `patient` but `mother_ramq` is required in `additional_info`.

```json
"patient": {
    "ep": "CHUSJ",
    ...
    // no mrn or ramq needed
    ...
    "additional_info": {
      "is_new_born": true,
      "mother_ramq": "RAMQ12341236"
    }
}
```

### Foetus body example

For foetus, `mrn` and `ramq` are the one from the mother.

```json
"patient": {
    "ep": "CHUSJ",
    "ramq": "RAMQ12341236",
    "mrn": "MRN001",
    "additional_info": {
      "is_prenatal_diagnosis": true,
      "foetus_gender": [male|female|unknown],
      "gestational_age": [ddm|dpa|deceased],
      "gestational_date": "2022-08-12"  // required if gestational_age is ddm | dpa
    }
}
```

### Trio body example

Two new sections `mother` and `father` at the same level than `patient`

```json
"patient": {
   ...
}
"mother": {
    "parent_enter_moment": "now",
    "ep": "CHUSJ",
    "no_mrn": true,
    "ramq": "XXXXXXXXX",
    "last_name": "Foo",
    "first_name": "BAR",
    "birth_date": "1990-01-01",
    "gender": "male",
    "parent_clinical_status": "affected",
    "signs": [
        {
        "value": "HP:0000002",
        "is_observed": true,
        "name": "Abnormality of body height",
        "age_code": "HP:0410280"
        }
    ]
},
"father": { 
    "parent_enter_moment": [later|never], 
    "parent_no_info_reason": "optional later/never reason" 
}
```
*Note: if `parent_clinical_status: not_affected|unknown` then `signs` isn't required*

### Response

`200 OK`
```json
{
  "id": "created_prescription_id"
}
```
## GET /search/patient/`ep`?`ramq`=foo&`mrn`=foo

### Parameters
|Name|Required|Type| Description                  |
|---|---|---|------------------------------|
|`ep`|true|String| Two `ep` can have the same `mrn` | 
|`ramq`|false|String| required if `mrn` is null    | 
|`mrn`|false|String| required if `ramq` is null   |
### Response
```json
{
    "first_name": "firstName",
    "last_name": "LastName",
    "gender": "male",
    "ep": "CHUSJ",
    "birth_date": "1090-07-20",
    "ramq": "RAMQ12341236",
    "mrn": "MRNTEST005"
}
```
## GET /search/prescription?`id`=foo&`ramq`=foo

### Parameters
| Name   |Required|Type| Description                |
|--------|---|---|----------------------------|
| `id`   |false|String| required if `ramq` is null | 
| `ramq` |false|String| required if `id` is null   |
### Response
```json
[
  {
    "id": "prescription_id",
    "ep": "bar",
    "panel_code": "MMG",
    "prescriber_id": "role_id",
    "prescriber_name": "Dre test test",
    "date": "2022-08-02",
    "patient_id": "foo",
    "patient_name": "Creation Prescription",
    "patient_ramq": "XXXX12345689",
    "mother_ramq": "RAMQ12341236"
  },
  {
    ...
  }
]
```
## GET /autocomplete/supervisor/`ep`/`prefix`

### Parameters
|Name|Required|Type|Description|
|---|---|---|---|
|`ep`|true|String|| 
|`prefix`|true|String|to match supervisor `id firstName lastName`| 
### Response
```json
[
    {
        "id": "PRR00003",
        "name": "Dre test test"
    },
    {
        "id": "PRR00031",
        "name": "Dre foo bar"
    }
]
```
## POST /assignment

Assign practitioner roles to a ServiceRequest of analysis type profile.

### Body

Backend validations:
- User roles include `clin_genetician`
- `analysis_id` is an existing ServiceRequest of analysis type profile.
- `assignments` are all practitioner roles known by FHIR and are `Geneticist` with code `15941008`
- `assignments` will fully replace previous assignments and therefore can be *empty*

```json
{
    "analysis_id": "445076", // the service request ID
    "assignments": [ // the practitioner roles IDs
        "PRR00001", 
        "PRR00002"
    ]
}
```
If successfully updated the response should be the same as the request body (built from FHIR response):
### Response
`200 OK`
```json
{
    "analysis_id": "445076",
    "assignments": [
        "PRR00001",
        "PRR00002"
    ]
}
```