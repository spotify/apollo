> NOTE: the recommended way to do deployment of apollo-standalone applications is via [Helios](https://ghe.spotify.net/helios/helios/blob/master/docs/apollo.md). Consider the approach described here as a legacy solution.

# The simple recipe
In order to get a standalone packaged apollo application to run on a standard spotify host you need two things.

1. A build that publishes the jar to artifactory, [ping](https://jenkins-tools.spotify.net/view/Ping/)
2. A role configuration in hiera-data that ties the apollo::standalone class to your machines

The minimal puppet configuration you need is the following:

`spotify-puppet/hiera-data/role/ping.yaml`
```yaml
---
apollo::standalone::services:
  ping:
    artifact_id: ping
    version: latest-snapshot

classes:
  - apollo::standalone
```
`version` can by any of `latest`, `latest-snapshot`, `z.y.x`, `z.y.x-SNAPSHOT`, `@/path/to/file.version`.

If you want to pass more arguments to the service, add `args: [...]` to the innermost hash.

For all possible parameters, see: [apollo::standalone::service](https://ghe.spotify.net/puppet/spotify-puppet/blob/master/modules/apollo/manifests/standalone/service.pp)

Applying the apollo::standalone class to your machines will take care of installing it with supervise or upstart. There's no need to create debian packages for your service any more. The installed job will take care of downloading the specified version on first startup. For future updates, you can use the [spapollo](https://ghe.spotify.net/apollo/apollo-tools) command:

```
spapollo install ping 3.1.4 && sudo restart spotify-ping
```

The version string can take the same values as for the puppet class parameter described above.

## Exposing Munin metrics

In order to hook the service up with our Munin/Monster metrics infrastructure, you'll have to specify a munin port that the service exposes metrics through. This is done in the role yaml file by adding the 'munin_port' key with a suitable port value (usually 4951).

```yaml
apollo::standalone::services:
  ping:
    artifact_id: ping
    version: latest-snapshot
    munin_port: 4951
```

## Troubleshooting suggestions

Some things to try, in random order:

* Check the logs in `/var/log/upstart/spotify-<service>.log`
* Check the logs in `/spotify/log/<service>`
* Check if there are any jar files in `/spotify/var/apollo/repo/`
* Try `sudo netstat -nltp | grep /java`
* Try `sudo spapollo run <service>:latest-snapshot shared.cloud -v`
* Check that the service is in `sudo initctl list`
  * Check if it restarting often by `watch -d -n1 'sudo initctl list | grep <service>'`

