meta model
==========

Apollo service metadata

An API to query meta information about a running service.

TODO: move to its own artifact (like it used to be)

API
---

### Info

Collects short, bounded, pieces of information about the service, such as build version and uptime.

    /_meta/0/info =>
    {
      result: {
        buildVersion: "...",
        serviceUptime: 60.0,
        container: "apollo/artemis/...?",
        systemVersion: "python/java/... version?",
      }
    }


### Config

The current loaded config of the service, possibly [filtered](#config-filtering).

Initial versions of the meta api exposed the config by default, but it now
should be enabled by putting `"_meta": { "expose-config": true },` in the root of the service
configuration.

    /_meta/0/config =>
    {
      result: {
        spnode: {
          _meta: { expose-config: true },
          . . .
        },
      }
    }


### Calls

Lists outgoing/incoming services to/from which calls have been made from/to the service.

    /_meta/0/calls =>
    {
      result: {
        outgoing: {
          "service": {
            endpoints: [ # optional
              uri: "...",
              method: ["..."],
            ],
          },
        ],
        incoming: [
          "service": {
            endpoints: [ # optional
              uri: "...",
              method: ["..."],
            ],
          },
        ],
      }
    }


### Endpoints

Lists the endpoints of the service, with as much metadata as available.

    /_meta/0/endpoints =>
    {
      result: {
        docstring: "...",
        endpoints: [
          {
            uri: "...",
            method: ["..."],
            requestPayloadSchema: {
              contentType: "...",
            }
            responsePayloadSchema: {
              contentType: "...",
            }
            docstring: "...",
            queryParameters: [
              {
                name: "...",
              },
            ],
          },
        ],
      }
    }


Config filtering
----------------

Config keys may be filtered away, by adding config to do so:

    {
      _meta:
        {
          config-filter:
            {
              secret: true
            }
        },
      secretStuff: . . .
    }

When serving configuration keys and values (in the `/_meta/0/config` resource),
if a key contains any substring listed in config-filter, its value will be replaced with `"*******"`.

By default, any key containing `passw`, `secret` or `private` is filtered.
