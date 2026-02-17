## SimRail Information Tools (SIT) Backend

![Build Status](https://github.com/simrailtools/backend/actions/workflows/ci.yml/badge.svg)

The SIT backend collects and provides data about everything happening in [SimRail](https://simrail.eu/en) in realtime.
This includes updates to servers, dispatch posts and journeys as well as extra features such as predicted vehicle
sequences and map data.

The collector service (single instance) bundles all the data from the upstream SimRail apis and

1. stores persistent data (such as journey information) into a [PostgreSQL](https://postgresql.org) database
2. sends out realtime updates of journey, server and dispatch post data via [nats](https://nats.io).
3. predicts data in realtime, such as the times for upcoming journey events

The api services (one or more instances) provide the stored information in form of a REST api. Realtime information can
be accessed by api users using
the [nats websocket protocol](https://docs.nats.io/reference/reference-protocols/nats-protocol).

A public api instance is hosted at [apis.simrail.tools](https://apis.simrail.tools), a frontend leveraging the api data
can be found on [simrail.tools](https://simrail.tools) (source code for the web frontend is available
on [GitHub](https://github.com/simrailtools/frontend) too!).

### Getting Started (Development)

Local development on the application is fairly easy.

1. start all necessary docker containers (defined in `.dev/docker-compose.yml`) using `make up_dev_d`
2. run the collector and/or api application from your IDE (ensure that the `common` and `dev` spring profiles are
   active, these already contain all the connection information for the databases)

Everything is secured using the same password and credentials to ease local development. The username is always `stb`,
the password being `stb_very_secu1e_passw0rd`.

The following services are started in docker:

| Service                          | URL                                               |
|----------------------------------|---------------------------------------------------|
| Prometheus                       | http://127.0.0.1:51810                            |
| Grafana (Dashboards for Metrics) | http://127.0.0.1:51811                            |
| PostgreSQL                       | 127.0.0.1:51800                                   |
| Valkey                           | 127.0.0.1:51801                                   |
| Nats                             | 127.0.0.1:51802 (TCP) 127.0.0.1:51803 (WebSocket) |

Note that metric scraping only works correctly if the collector runs on port 8080 and the api on port 8081. Also, the
database schema is initialized by the collector service only (the initial schema and migrations are located in the
`.flyway` directory, (as the name suggests) this project uses [Flyway](https://flywaydb.org) for database initialization
and migration).

### Updating the base data

The base data (data that is not provided by upstream apis directly) is located in JSON files in the `.data` directory.
The data can be edited or supplemented there. Note that a bunch of tests exists in the common module for the data, make
sure to update them accordingly to your changes (and ensure that they still pass). If you found an error in the data,
feel free to correct it. Please check if a small unit test can be added to prevent future regressions in the data.

### License

This project is released under the terms of the MIT license. See [license.txt](license.txt)
or https://opensource.org/licenses/MIT.
