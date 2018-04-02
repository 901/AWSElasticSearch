## Elastic Search <-> API Gateway Demo in Java

### Overview
This is the lambda function code to integrate an ElasticSearch Cluster (AWS) with an API Gateway. The goal is to be able to query a database of animals created by GBIF (Global Biodiversity Informatics Facility) through a public API. Users should be able to make queries on the API and retrieve valuable information about the research.

### Technical Specs
This project is broken into 3 main parts:

**ElasticSearch database &larr; AWS Lambda functions &larr; &rarr; API Gateway**

#### AWS Elastic ElasticSearch Component
This project is running on the Free Tier of ElasticSearch, hosting several documents. The main document (which this code targets) is the GBIF database. The cluster is **access-controlled** through IAM User management: Only a specified IAM user can query the cluster with GET requests so as to limit possible damage from anonymous parties or unsigned API queries. Ideally, there would be 3 dedicated Master Nodes and 2 Worker Nodes so as to reduce the latency between queries, but this is not possible on the free tier. Also, we can find the necessary storage for the cluster using the following formula:

`Storage Requirement = Source Data * (1 + Number of Replicas) * 1.45`


#### AWS Lambda Function
In order to communicate between API Gateway and AWS ElasticSearch, we need to use AWS Lambda Functions. Using Java, and the Eclipse AWS Toolkit, its easy to generate AWS Lambda functions and upload them to the console. So, we create a Lambda Handler function which accepts the query string as input, and returns an AWS Response as a json object which contains the response from the ES Query.

The lambda function works as such:

1. Handler Function catches input (type: LinkedHashMap) and context of Lambda event
2. Parse the input using Google GSON Library, create a JsonObject of the `querystring`.
3. Generate a payload which consists of our ElasticSearch query to match the necessary terms and queries (found in `querystring`)
4. Create an AWS Request Object, set the content headers, body and parameters.
5. Sign the request with credentials using AWS4 Signing
6. Generate a response object by executing the request on the server.
7. Send response back (JSON-formatted) to invoker.

#### AWS API Gateway
This part is relatively straightforward - once the lambda is created, we simply connect the Lambda Integration to a new resource and GET method, then set method request parameters. Also, it's important to set the integration response to application/json so the output is formatted as a json. A test should result in a successful query.

The API is deployed and can be invoked publicly from here.

### Example Query and Output
Query:

`
Request: /gbif?kingdom=animalia&country=belgium&sex=male
`

```
{
  "took": 6,
  "timed_out": false,
  "_shards": {
    "total": 5,
    "successful": 5,
    "skipped": 0,
    "failed": 0
  },
  "hits": {
    "total": 871,
    "max_score": 5.0037503,
    "hits": [
      {
        "_index": "gbif",
        "_type": "record",
        "_id": "21",
        "_score": 5.0037503,
        "_source": {
          "key": 925334504,
          "datasetKey": "83e20573-f7dd-4852-9159-21566e1e691e",
          "publishingOrgKey": "1cd669d0-80ea-11de-a9d0-f1765f95f18b",
          "publishingCountry": "BE",
          "protocol": "DWC_ARCHIVE",
          "lastCrawled": "2014-07-17T19:24:15.353+0000",
          "lastParsed": "2014-07-17T16:28:03.304+0000",
          "extensions": "null",
          "basisOfRecord": "MACHINE_OBSERVATION",
          "sex": "MALE",
          "lifeStage": "ADULT",
          "taxonKey": 2481139,
          "kingdomKey": 1,
          "phylumKey": 44,
          "classKey": 212,
          "orderKey": 7192402,
          "familyKey": 9316,
          "genusKey": 2481126,
          "speciesKey": 2481139,
          "scientificName": "Larus argentatus Pontoppidan, 1763",
          "kingdom": "Animalia",
          "phylum": "Chordata",
          "order": "Charadriiformes",
          "family": "Laridae",
          "genus": "Larus",
          "species": "Larus argentatus",
          "genericName": "Larus",
          "specificEpithet": "argentatus",
          "taxonRank": "SPECIES",
          "decimalLongitude": 2.8549,
          "decimalLatitude": 50.9899,
          "elevation": 0,
          "year": 2014,
          "month": 1,
          "day": 25,
          "eventDate": "2014-01-25T18:55:22.000+0000",
          "issues": "COORDINATE_ROUNDED,COUNTRY_DERIVED_FROM_COORDINATES,MODIFIED_DATE_UNLIKELY",
          "modified": "2014-07-17T09:47:54.000+0000",
          "lastInterpreted": "2014-07-17T16:53:20.232+0000",
          "identifiers": "null",
          "facts": "null",
          "relations": "null",
          "geodeticDatum": "WGS84",
          "class": "Aves",
          "countryCode": "BE",
          "country": "Belgium",
          "informationWithheld": "see metadata",
          "georeferencedDate": "2014-01-25T19:55:22Z",
          "georeferenceVerificationStatus": "unverified",
          "nomenclaturalCode": "ICZN",
          "individualID": "H903169",
          "rights": "http://creativecommons.org/publicdomain/zero/1.0/",
          "rightsHolder": "INBO",
          "ownerInstitutionCode": "INBO",
          "type": "Event",
          "georeferenceProtocol": "doi:10.1080/13658810412331280211",
          "occurrenceID": "182685",
          "georeferenceSources": "GPS",
          "vernacularName": "Herring Gull",
          "gbifID": "925334504",
          "samplingEffort": "secondsSinceLastOccurrence=896",
          "samplingProtocol": "doi:10.1007/s10336-012-0908-1",
          "institutionCode": "INBO",
          "datasetID": "http://dataset.inbo.be/bird-tracking-gull-occurrences",
          "dynamicProperties": "device_info_serial=799",
          "datasetName": "Bird tracking - GPS tracking of Lesser Black-backed Gull and Herring Gull breeding at the Belgian coast",
          "minimumDistanceAboveSurfaceInMeters": "0",
          "language": "en",
          "identifier": "182685"
        }
      },
      {
        "_index": "gbif",
        "_type": "record",
        "_id": "17",
        "_score": 4.6759005,
        "_source": {
          "key": 925333910,
          "datasetKey": "83e20573-f7dd-4852-9159-21566e1e691e",
          "publishingOrgKey": "1cd669d0-80ea-11de-a9d0-f1765f95f18b",
          "publishingCountry": "BE",
          "protocol": "DWC_ARCHIVE",
          "lastCrawled": "2014-07-17T19:24:15.662+0000",
          "lastParsed": "2014-07-17T16:28:02.580+0000",
          "extensions": "null",
          "basisOfRecord": "MACHINE_OBSERVATION",
          "sex": "MALE",
          "lifeStage": "ADULT",
          "taxonKey": 2481139,
          "kingdomKey": 1,
          "phylumKey": 44,
          "classKey": 212,
          "orderKey": 7192402,
          "familyKey": 9316,
          "genusKey": 2481126,
          "speciesKey": 2481139,
          "scientificName": "Larus argentatus Pontoppidan, 1763",
          "kingdom": "Animalia",
          "phylum": "Chordata",
          "order": "Charadriiformes",
          "family": "Laridae",
          "genus": "Larus",
          "species": "Larus argentatus",
          "genericName": "Larus",
          "specificEpithet": "argentatus",
          "taxonRank": "SPECIES",
          "decimalLongitude": 2.8567,
          "decimalLatitude": 51.0246,
          "elevation": 0,
          "year": 2014,
          "month": 1,
          "day": 13,
          "eventDate": "2014-01-13T22:27:22.000+0000",
          "issues": "COORDINATE_ROUNDED,COUNTRY_DERIVED_FROM_COORDINATES,MODIFIED_DATE_UNLIKELY",
          "modified": "2014-07-17T09:47:54.000+0000",
          "lastInterpreted": "2014-07-17T16:53:14.973+0000",
          "identifiers": "null",
          "facts": "null",
          "relations": "null",
          "geodeticDatum": "WGS84",
          "class": "Aves",
          "countryCode": "BE",
          "country": "Belgium",
          "informationWithheld": "see metadata",
          "georeferencedDate": "2014-01-13T23:27:22Z",
          "georeferenceVerificationStatus": "unverified",
          "nomenclaturalCode": "ICZN",
          "individualID": "H903169",
          "rights": "http://creativecommons.org/publicdomain/zero/1.0/",
          "rightsHolder": "INBO",
          "ownerInstitutionCode": "INBO",
          "type": "Event",
          "georeferenceProtocol": "doi:10.1080/13658810412331280211",
          "occurrenceID": "182171",
          "georeferenceSources": "GPS",
          "vernacularName": "Herring Gull",
          "gbifID": "925333910",
          "samplingEffort": "secondsSinceLastOccurrence=1787",
          "samplingProtocol": "doi:10.1007/s10336-012-0908-1",
          "institutionCode": "INBO",
          "datasetID": "http://dataset.inbo.be/bird-tracking-gull-occurrences",
          "dynamicProperties": "device_info_serial=799",
          "datasetName": "Bird tracking - GPS tracking of Lesser Black-backed Gull and Herring Gull breeding at the Belgian coast",
          "minimumDistanceAboveSurfaceInMeters": "5",
          "language": "en",
          "identifier": "182171"
        }
      },
      {
        "_index": "gbif",
        "_type": "record",
        "_id": "23",
        "_score": 4.6759005,
        "_source": {
          "key": 925334735,
          "datasetKey": "83e20573-f7dd-4852-9159-21566e1e691e",
          "publishingOrgKey": "1cd669d0-80ea-11de-a9d0-f1765f95f18b",
          "publishingCountry": "BE",
          "protocol": "DWC_ARCHIVE",
          "lastCrawled": "2014-07-17T19:24:15.552+0000",
          "lastParsed": "2014-07-17T16:28:03.580+0000",
          "extensions": "null",
          "basisOfRecord": "MACHINE_OBSERVATION",
          "sex": "MALE",
          "lifeStage": "ADULT",
          "taxonKey": 2481139,
          "kingdomKey": 1,
          "phylumKey": 44,
          "classKey": 212,
          "orderKey": 7192402,
          "familyKey": 9316,
          "genusKey": 2481126,
          "speciesKey": 2481139,
          "scientificName": "Larus argentatus Pontoppidan, 1763",
          "kingdom": "Animalia",
          "phylum": "Chordata",
          "order": "Charadriiformes",
          "family": "Laridae",
          "genus": "Larus",
          "species": "Larus argentatus",
          "genericName": "Larus",
          "specificEpithet": "argentatus",
          "taxonRank": "SPECIES",
          "decimalLongitude": 2.9361,
          "decimalLatitude": 51.0863,
          "elevation": 0,
          "year": 2014,
          "month": 1,
          "day": 28,
          "eventDate": "2014-01-28T09:56:11.000+0000",
          "issues": "COORDINATE_ROUNDED,COUNTRY_DERIVED_FROM_COORDINATES,MODIFIED_DATE_UNLIKELY",
          "modified": "2014-07-17T09:47:54.000+0000",
          "lastInterpreted": "2014-07-17T16:53:21.468+0000",
          "identifiers": "null",
          "facts": "null",
          "relations": "null",
          "geodeticDatum": "WGS84",
          "class": "Aves",
          "countryCode": "BE",
          "country": "Belgium",
          "informationWithheld": "see metadata",
          "georeferencedDate": "2014-01-28T10:56:11Z",
          "georeferenceVerificationStatus": "unverified",
          "nomenclaturalCode": "ICZN",
          "individualID": "H903169",
          "rights": "http://creativecommons.org/publicdomain/zero/1.0/",
          "rightsHolder": "INBO",
          "ownerInstitutionCode": "INBO",
          "type": "Event",
          "georeferenceProtocol": "doi:10.1080/13658810412331280211",
          "occurrenceID": "182901",
          "georeferenceSources": "GPS",
          "vernacularName": "Herring Gull",
          "gbifID": "925334735",
          "samplingEffort": "secondsSinceLastOccurrence=896",
          "samplingProtocol": "doi:10.1007/s10336-012-0908-1",
          "institutionCode": "INBO",
          "datasetID": "http://dataset.inbo.be/bird-tracking-gull-occurrences",
          "dynamicProperties": "device_info_serial=799",
          "datasetName": "Bird tracking - GPS tracking of Lesser Black-backed Gull and Herring Gull breeding at the Belgian coast",
          "minimumDistanceAboveSurfaceInMeters": "-3",
          "language": "en",
          "identifier": "182901"
        }
      }
      .... [TRUNCATED]
```
We can use any number of the following paramters to query this database:

- Key
- Sex (strict match "Male" or "Female")
- Scientific Name
- Kingdom
- Phylum
- Class
- Order
- Family
- Genus
- Species
- Country
- Vernacular Name
- Year
- Rights Holder

### Usage

[Public API URL](https://82794poka2.execute-api.us-east-2.amazonaws.com/prod/gbif)

Append `?` and Key-value pairs to query the API. For example:

`[URL]?sex=male&kingdom=Animalia&country=Belgium`

Returns a JSON formatted object that can be parsed for data easily. 
