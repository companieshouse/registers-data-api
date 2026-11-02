# Registers Data API

## Overview

Registers Data API is a Spring Boot service that stores and retrieves company
register data in MongoDB. It receives register updates from
[registers-delta-consumer](https://github.com/companieshouse/registers-delta-consumer)
and exposes company register data through a REST API.

Updates and deletions notify
[chs-kafka-api](https://github.com/companieshouse/chs-kafka-api) through its
`POST /private/resource-changed` endpoint.

Read requests support API key and OAuth2 identities. Write requests are restricted
to API keys with the `internal-app` privilege.

## Table of Contents

- [Overview](#overview)
- [Development Requirements](#development-requirements)
- [Building and Testing](#building-and-testing)
- [Developer Testing with Docker](#developer-testing-with-docker)
- [Running Locally without Docker CHS](#running-locally-without-docker-chs)
- [Environment Variables](#environment-variables)
- [API Routes](#api-routes)
- [Developer Technical Notes](#developer-technical-notes)

## Development Requirements

- [Java 21](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html).
- [Maven](https://maven.apache.org/download.cgi).
- [Git](https://git-scm.com/downloads) and Make.
- Access to the Companies House Maven dependencies, including the parent POM and
  internal SDKs.
- Docker and the
  [Docker CHS Development](https://github.com/companieshouse/docker-chs-development)
  environment for local end-to-end testing.

MongoDB is required for register persistence. Successful write operations also
require access to `chs-kafka-api` and the SDK configuration described below.

## Building and Testing

Build the service:

```sh
make clean build
```

The build target skips tests and copies the executable JAR to
`registers-data-api.jar`. To build and run the configured Maven verification
lifecycle together:

```sh
mvn verify
```

The existing Make targets can also be run separately:

| Command | Purpose |
|---|---|
| `make test-unit` | Run unit tests. |
| `make test-integration` | Run the configured integration-test lifecycle. |
| `make test` | Run the integration and unit test targets. |
| `make package version=<version>` | Package a versioned ZIP containing the application JAR. |
| `make sonar` | Run Sonar analysis using the configured credentials. |

The integration-test target does not by itself prove end-to-end coverage: check
the Failsafe output for tests actually executed. Use the Docker CHS environment
to exercise MongoDB and resource-change notifications.

## Developer Testing with Docker

First follow the setup instructions in
[Docker CHS Development](https://github.com/companieshouse/docker-chs-development).
Use Java 21 in the terminal used to build this service.

From the Docker CHS Development root:

```sh
chs-dev services enable registers-data-api
chs-dev development enable registers-data-api
chs-dev up
```

The service's
[Docker CHS configuration](https://github.com/companieshouse/docker-chs-development/blob/master/services/modules/delta/registers-data-api.docker-compose.yaml)
declares MongoDB, ERIC and `authentication-service` as dependencies. Enable
`chs-kafka-api` as well when testing updates and deletions:

```sh
chs-dev services enable chs-kafka-api
chs-dev up
```

View logs and reload the service after changes using the Docker CHS helpers:

```sh
docker_chs logs -f registers-data-api
docker_chs reload registers-data-api
```

Use the gateway route
`http://api.chs.local/company/{company_number}/registers` to test with an
authenticated client, such as Companies House Enterprise Postman. Health is
available at `/registers-data-api/healthcheck` on the application itself, matching
the Docker CHS health probe; gateway routing may differ.

## Running Locally without Docker CHS

Provide a MongoDB instance, configure the environment variables below, and run:

```sh
MONGODB_URL=mongodb://localhost:27017 mvn spring-boot:run
```

The application listens on port `8080` by default. Check application health:

```sh
curl http://localhost:8080/registers-data-api/healthcheck
```

This command only checks application health, not MongoDB connectivity or the
ability to publish resource-change notifications. The MongoDB health indicator
is explicitly disabled.

For write operations, also configure the internal SDK and `chs-kafka-api`.
Requests normally reach the service through ERIC; do not expose a standalone
instance that trusts caller-supplied ERIC identity headers to an untrusted network.

## Environment Variables

The following application settings are defined in
[`application.properties`](src/main/resources/application.properties):

| Variable | Purpose | Default |
|---|---|---|
| `MONGODB_URL` | MongoDB connection URL, without a database path. The application appends `/company_registers?retryWrites=false`. | `mongodb://mongo:27017` |
| `CHS_KAFKA_API_URL` | Base URL used for resource-change requests to `chs-kafka-api`. Set a complete URL for write operations. | `localhost` |
| `CHS_API_KEY` | Supplies the `chs.kafka.api.key` property. Outbound calls use the internal SDK, so also configure its credentials below. | Development placeholder; not a production credential |
| `OTEL_LOG_ENABLED` | Enables the configured OpenTelemetry support. | `false` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | Base OTLP HTTP endpoint for logs, traces and metrics. | `http://localhost:4318` |

The Docker CHS service configuration also supplies the following settings for
shared libraries and local development. Use the central configuration for
environment-specific values; do not commit credentials.

| Variable | Purpose |
|---|---|
| `CHS_INTERNAL_API_KEY` | API key used by the internal SDK for private API calls. |
| `API_URL` | API base URL supplied to the SDK. |
| `PAYMENTS_API_URL` | Payments API base URL supplied to the SDK. |
| `DOCUMENT_API_URL` | Document API base URL supplied to the SDK. |
| `DOCUMENT_API_LOCAL_URL` | Local document API base URL supplied to the SDK. |
| `LOGLEVEL` | Structured logging level; Docker CHS uses `debug`. |
| `HUMAN_LOG` | Human-readable logging setting used in local development. |
| `OTEL_SERVICE_NAME` | Service name for telemetry, normally `registers-data-api`. |

`SERVER_PORT` can override Spring Boot's default port when running directly.
The packaged container start script fixes the port at `8080`.

## API Routes

### Resource-Based Routes

| Method | Path | Purpose | Normal responses |
|---|---|---|---|
| `GET` | `/company/{company_number}/registers` | Retrieve the company's register data as a `CompanyRegister`. | `200` with data; `404` if absent. |
| `PUT` | `/company/{company_number}/registers` | Create or update register data from an `InternalRegisters` request. | `200`; `409` if the update is older than stored data. |
| `DELETE` | `/company/{company_number}/registers` | Notify resource deletion and remove the stored document. | `200`; `404` if absent. |

Malformed input handled by the exception advice returns `400`. Database or
resource-change service failures handled by the service return `503`.

Request and response models are supplied by the Companies House SDKs. See
[`RegistersController`](src/main/java/uk/gov/companieshouse/registers/controller/RegistersController.java)
for the implemented routes and
[`private-api-sdk-java`](https://github.com/companieshouse/private-api-sdk-java)
for the internal register models.

### Other Routes

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/registers-data-api/healthcheck` | Spring Boot Actuator health endpoint; does not require ERIC identity headers. |

### Authentication and Request Headers

ERIC supplies the identity headers after authenticating the caller. The
application applies the following checks:

| Header | Requirement |
|---|---|
| `ERIC-Identity` | A non-blank identity is required for register requests. |
| `ERIC-Identity-Type` | Must be `key` or `oauth2` for reads. Writes require `key`. |
| `ERIC-Authorised-Key-Privileges` | Comma-separated privileges; writes require the exact `internal-app` entry. |

Missing or invalid identity information returns `401`. An identity without the
required write privileges returns `403`. Browser CORS support is limited to GET.

## Developer Technical Notes

### Persistence and Update Ordering

The database and collection are both named `company_registers`. Documents are
looked up using the company number.

PUT compares the incoming `internal_data.delta_at` with the stored delta
timestamp. Older updates are rejected. Equal timestamps are accepted so a retry
can repeat resource-change notification.

The service saves an update before notifying `chs-kafka-api`. If notification
fails, the document may already have been persisted even though the request
returns an error. DELETE sends the notification before removing the document;
the document is retained if notification fails.

### JSON and MongoDB Conversion

Custom MongoDB converters configure date serialisation, null omission and
empty-field handling. When changing models or Jackson dependencies, check
round-trips of existing documents and HTTP payloads as well as deleted-data
payloads sent in resource-change notifications.

### Resource-Change Notifications

Notifications use resource kind `registers` and resource URI
`company/{company_number}/registers`. Deletion events include the deleted register
data. The request ID is propagated to the SDK call and notification context.

### Dependency Security

See [Jackson and Guava vulnerability remediation](DEPENDENCY-VULNERABILITY-REMEDIATION.md)
for the dependency upgrades, compatibility considerations and deployment checks
associated with the reported security advisories.
