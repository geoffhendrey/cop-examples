# Meteodata solution

This example demonstrates versability of Cisco Observability Platform in a sense
that there is basically no data source nor a domain which couldn't be modelled
using Flexible Metedata model and brought in by either Zodiac function transforming
the data or by direct Open Telemetry call.

The goal of this solution is to allow users to specify for which locations the data
should be retrieved by adding an object to knowledge store and then verify that this
data is being retrieved by querying the platform.

Source of the weather data is [open-meteo.com](https://open-meteo.com), which provides
free current and historical weather data, without a need for a registration, API token
or anything pre-requisite other than being able to execute a HTTP request and parse
JSON response

## Knowledge model

As any other configuration on Cisco Observability Platform, the list of locations for
which the weather data will be collected will be stored in the [Knowledge Store](https://developer.cisco.com/docs/cisco-observability-platform/#!knowledge-store-introduction/introduction).

For that, the Meteodata solution defines a type `meteodata:meteoLocation`

https://github.com/geoffhendrey/cop-examples/blob/main/example/meteodata/package/types/meteo_location.json

`meteoLocation` type is pretty simple, it defines `name`, `latitude` and `longitude`,
while all the attributes are required.

Once the solution is subscribed, the object of that type can be created by using [fsoc](https://github.com/cisco-open/fsoc)

`fsoc knowledge create --type=meteodata:meteoLocation --object-file=./prague.json --layer-type=TENANT`

where `prague.json` would contain

```json
{
    "name": "Prague, CZ",
    "latitude": 50.08804,
    "longitude": 14.42076
}
```

## Entity model

Each configured location is represented by an [entity](https://developer.cisco.com/docs/cisco-observability-platform/#!entities)
defined as

https://github.com/geoffhendrey/cop-examples/blob/main/example/meteodata/package/objects/model/entities/location.json

The entity itself copies the knowledge object referenced above, adds elevation and defines
list of [metric](https://developer.cisco.com/docs/cisco-observability-platform/#!metrics-model) types to be reported to that particular entity.

It's the complete set of data provided by the datasource with 15 minute granularity:

```json
    "metricTypes": [
"meteodata:apparent_temperature",
"meteodata:cloud_cover",
"meteodata:is_day",
"meteodata:precipitation",
"meteodata:pressure_msl",
"meteodata:rain",
"meteodata:relative_humidity_2m",
"meteodata:showers",
"meteodata:snowfall",
"meteodata:surface_pressure",
"meteodata:temperature_2m",
"meteodata:weather_code",
"meteodata:wind_direction_10m",
"meteodata:wind_gusts_10m",
"meteodata:wind_speed_10m"
]
```

## Zodiac function

The active component which periodically collects data from the source is implemented
as a Zodiac function. The algorithm could be summed as

- retrieve meteodata:meteoLocation object from Knowledge store for subscribed tenant
- extract latitudes and longitudes, call [open-meteo.com](open-meteo.com) and request current data
- parse the response, convert it to open telemetry metric packet and sent it to the [platform](https://developer.cisco.com/docs/cisco-observability-platform/#!data-ingestion-introduction)

Zodiac function is implemented as a Micronaut application in Java (version 21), sources are present in
directory `open-meteo-zodiac`.

There is little to be stated about the implementation itself, maybe only few facts:

- Platform service hosts / URIs are supplied as environment variables, which are converted
  to the Micronaut configuration in `src/main/resources/application.yaml`
- Zodiac cron will trigger `POST /` request with appropriate headers, notably `layer-id` and
  `layer-type`, where the first mentioned provides a context of a tenant - that's important,
  since different tenants could have different `meteodata:meteoLocation` Knowledge objects
  stored, meaning this context needs to be propagated to all calls done on behalf of the
  tenant, including call to platform ingestion.
- [Open Telemetry Java SDK](https://opentelemetry.io/docs/languages/java/) is used to report metrics, but it's not the "usual" high-level API.
  In this case, since the function needs to report to multiple dynamic OTEL Resources, slight
  lower level API is used - the function directly assembles OTEL packet and reports it to
  platform ingestion endpoint. See [OTLP gRPC Exporter](https://javadoc.io/doc/io.opentelemetry/opentelemetry-exporter-otlp-metrics/latest/io/opentelemetry/exporter/otlp/metrics/OtlpGrpcMetricExporter.html) for more details.

## What's left to define

There are some other objects defined in the solution, which are required to achieve
given task. One of those are `iam:Permission` and `iam:RoleToPermissionMapping`. Those
define access for object of introduced knowledge type `meteodata:meteoLocation`, since
the default access control is to "deny" any access. Here, the solution defines quite
an opposite - anyone who can log in into the platform can modify objects of that type.

Zodiac function definition is also not only a reference to a docker image, it also contains
definition of a cron trigger and also egress host declaration. Without that being present,
the Zodiac function wouldn't be able to call any resource outside in the Internet.

Last but not least is the resource mapping configuration, which makes sure that ingested
weather metrics are going to be mapped to proper entity.

## Validation

To validate that the solution is working as it should, query coudl be issued (the initial
data grab could take up to 15 minutes)

As with anything else, we can use fsoc

```
% fsoc uql "fetch attributes(location.name), metrics(meteodata:is_day) from entities(meteodata:location) options metricNullFill(false)"
 location_name | metrics                                           
               | source    | metrics                               
               |           | timestamp                     | value 
===================================================================
 San Jose, CA  | meteodata | 2024-02-08 06:00:00 +0000 UTC | 0     
               |           | 2024-02-08 06:15:00 +0000 UTC | 0     
               |           | 2024-02-08 06:30:00 +0000 UTC | 0     
               |           | 2024-02-08 06:45:00 +0000 UTC | 0     
---------------+-----------+-------------------------------+-------
 Tokyo, JP     | meteodata | 2024-02-08 06:00:00 +0000 UTC | 1     
               |           | 2024-02-08 06:15:00 +0000 UTC | 1     
               |           | 2024-02-08 06:30:00 +0000 UTC | 1     
               |           | 2024-02-08 06:45:00 +0000 UTC | 1     
---------------+-----------+-------------------------------+-------
 Prague, CZ    | meteodata | 2024-02-08 06:00:00 +0000 UTC | 0     
               |           | 2024-02-08 06:15:00 +0000 UTC | 0     
               |           | 2024-02-08 06:30:00 +0000 UTC | 1     
               |           | 2024-02-08 06:45:00 +0000 UTC | 1     
---------------+-----------+-------------------------------+-------
```

There are also Cisco Observability Platform tools like Schema Browser, Query Builder
and Metric Explorer, which do work quite well when discovering data for which the
Observe UI is not available.

## Debugging

As with anything which involves any amount of source code, there might be bugs and
zodiac function included as part of this solution is not an exception. During the time
of writing this example there wasn't a good support for accessing logs emitted by the
function added in a solution, but that doesn't stop us from writing it by ourselves,
we are working with monitoring platform which can handle logs.

The only thing to do here is to report them directly to the tenants, similarly as with
the open-meteo metrics.

As any logging mechanism, there should be also a configuration, which would say what should
be logged and also be able to turn it off.

For that, another Knowledge type has been introduced: `meteodata:meteoConfig`

https://github.com/geoffhendrey/cop-examples/blob/main/example/meteodata/package/types/meteo_config.json

This is designed to have only a single object, defined as 

https://github.com/geoffhendrey/cop-examples/blob/main/example/meteodata/package/objects/meteodata/meteoConfig/meteoConfig.json

which also sets the `logLevel` property to `OFF`, meaning logging is not forwarded to the platform by default.

Tenants subscribed to this solution can create a "fragment", basically a patch on a `TENANT` layer, using which
the `logLevel` property could be changed to either `INFO` or `DEBUG`, based on the needed level of insight.

Logs could be then retrieved conveniently by `fsoc` using following command:

```
fsoc logs 'entities(meteodata:service)'
```

## Solution file structure

```
.
├── README.md
├── manifest.json
├── meteoConfigPatch
│   └── meteoConfigPatch.json
├── meteoLocation-object-examples
│   ├── north_pole.json
│   ├── prague.json
│   ├── san-jose.json
│   ├── south_pole.json
│   └── tokyo.json
├── objects
│   ├── functions
│   │   ├── egressHosts
│   │   │   └── egress.json
│   │   ├── function
│   │   │   └── meteodata.json
│   │   └── subscriptionCronConfig
│   │       └── meteodata-cron.json
│   ├── iam
│   │   ├── permission
│   │   │   └── meteo-location-permission.json
│   │   └── roleToPermissionMapping
│   │       └── meteo-permission-mapping.json
│   ├── meteodata
│   │   └── meteoConfig
│   │       └── meteoConfig.json
│   └── model
│       ├── entities
│       │   ├── location.json
│       │   └── service.json
│       ├── events
│       ├── metrics
│       │   ├── apparent_temperature.json
│       │   ├── cloud_cover.json
│       │   ├── is_day.json
│       │   ├── precipitation.json
│       │   ├── pressure_msl.json
│       │   ├── rain.json
│       │   ├── relative_humidity_2m.json
│       │   ├── showers.json
│       │   ├── snowfall.json
│       │   ├── surface_pressure.json
│       │   ├── temperature_2m.json
│       │   ├── weather_code.json
│       │   ├── wind_direction_10m.json
│       │   ├── wind_gusts_10m.json
│       │   └── wind_speed_10m.json
│       ├── namespaces
│       │   └── meteodata.json
│       └── resource-mappings
│           ├── location-resourceMapping.json
│           └── service-resourceMapping.json
└── types
    ├── meteo_config.json
    └── meteo_location.json

```

The majority of a solution has been created with `fsoc`, adding reference commands
which do replicate the structure. Some of the objects were added by hand, since
`fsoc` doesn't support them, also all generated files have been modified to match
the intended shape - `fsoc` provides an initial template, not the final result.

```
fsoc solution init meteodata

fsoc solution extend --add-knowledge meteo_location

fsoc solution extend --add-entity location

fsoc solution extend --add-metric temperature_2m
fsoc solution extend --add-metric apparent_temperature
fsoc solution extend --add-metric relative_humidity_2m
fsoc solution extend --add-metric is_day
fsoc solution extend --add-metric precipitation
fsoc solution extend --add-metric rain
fsoc solution extend --add-metric showers
fsoc solution extend --add-metric snowfall
fsoc solution extend --add-metric weather_code
fsoc solution extend --add-metric cloud_cover
fsoc solution extend --add-metric pressure_msl
fsoc solution extend --add-metric surface_pressure
fsoc solution extend --add-metric wind_speed_10m
fsoc solution extend --add-metric wind_direction_10m
fsoc solution extend --add-metric wind_gusts_10m

fsoc solution validate --stable
fsoc solution push --stable

fsoc solution subscribe meteodata

fsoc knowledge get-type --type meteodata:meteoLocation

fsoc knowledge create --type=meteodata:meteoLocation --object-file=./meteoLocation-object-examples/prague.json --layer-type=TENANT
fsoc knowledge create --type=meteodata:meteoLocation --object-file=./meteoLocation-object-examples/san-jose.json --layer-type=TENANT
fsoc knowledge create --type=meteodata:meteoLocation --object-file=./meteoLocation-object-examples/tokyo.json --layer-type=TENANT

fsoc knowledge create-patch --type=meteodata:meteoConfig --target-object-id=meteodata:config --target-layer-type=TENANT --json-merge-patch --object-file=./meteoConfigPatch/meteoConfigPatch.json
```