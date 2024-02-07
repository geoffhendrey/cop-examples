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

```json
# example/meteodata/package/types/meteo_location.json
```

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

```json
# example/meteodata/package/objects/model/entities/location.json
```

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

## Solution file structure

```
.
├── README.md
├── meteoLocation-objects-examples
│   ├── prague.json
│   ├── san-jose.json
│   └── tokyo.json
└── package
    ├── manifest.json
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
    │   └── model
    │       ├── entities
    │       │   └── location.json
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
    │           └── location-resourceMapping.json
    └── types
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
```

