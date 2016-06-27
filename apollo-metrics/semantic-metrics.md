### legend

* `+`: added tag to metric id
* `=>`: method call [`->`: new stats class]
* `type=`: metric type used

## Complete metrics tree

```
ApolloStats
`- + service:<service>
    `=> newScopeFactory(service) -> ScopeFactoryStats
        `- + component:scope-factory
            `- + endpoint:<endpoint>
            |   `=> newServiceRequest(endpoint) -> ServiceRequestStats
            |       `- + component:service-request
            |           `- + what:request-fanout-factor + unit:request/request          *** keep
            |           |   `=> fanout(requests)
            |           |       `-> type=Histogram
            |           `- + what:request-received + unit:message                       *** drop DONE
            |           |   `=> requestReceived(messageInfo) (+ content-type + sending-service)
            |           |       `-> type=Meter
            |           `- + what:incoming-request + unit:message                       *** drop DONE
            |           |   `=> requestReceived(messageInfo)                            *** keep UPDATED
            |           |       `-> type=Meter
            |           `- + what:request-payload-size + unit:B                         *** drop DONE
            |           |   `=> requestReceived(messageInfo) (+ content-type + sending-service)
            |           |       `-> type=Histogram
            |           `- + what:reply-sent + unit:message                             *** add to hermes-java reply-status DONE
            |           |   `- + status-code:<status-family>|unknown
            |           |       `=> replySent(messageInfo) (+ content-type + sending-service)
            |           |           `-> type=Meter
            |           `- + what:reply-payload-size + unit:B                           *** drop DONE
            |           |   `=> replySent(messageInfo) (+ content-type + sending-service)   
            |           |       `-> type=Histogram                                          
            |           `- + what:incoming-request-duration                             *** drop DONE
            |           |   `=> timeRequest()                                           *** keep UPDATED
            |           |       `-> type=Timer (updated from OutcomeTimer)
            |           `- + what:endpoint-run-time                                     *** drop DONE
            |               `- + section:incoming-request-handler
            |                   `- + component:endpoint-runnable
            |                       `=> reportEndpoint()
            |                           `-> type=Timer
            |
            `- + endpoint:<endpoint>
            |   `=> newEndpointRunnable(endpoint) -> EndpointRunnableStats
            |       `- + component:endpoint-runnable
            |           `- + what:endpoint-run-time                                     *** drop DONE
            |           |   `- + section:reply-failure-handler
            |           |   |   `=> reportDownstreamFailure()
            |           |   |       `-> type=Timer
            |           |   `- + section:reply-success-handler
            |           |       `=> reportDownstreamReply(service, status)
            |           |           `-> type=Timer
            |           `=> newRequestContext() -> RequestContextClientStats
            |               `- + what:reply-received + unit:message                     *** drop DONE
            |               |   `- + status-code:<status-family>|unknown
            |               |       `=> incomingReply(status)
            |               |           `-> type=Meter
            |               `- + remote-service:<remote-service>                        *** drop DONE
            |                   `=> newRequestContext(service) -> RequestContextStats
            |                       `- + component:request-context
            |                           `- + what:outgoing-request + unit:message
            |                           |   `=> time()
            |                           |       `-> type=Meter
            |                           `- + what:outgoing-request-duration
            |                               `=> time()
            |                                   `-> type=OutcomeTimer
            |
            `=> newRequestRunnable() -> RequestRunnableStats                            *** drop DONE
                `- + what:request-run-time
                    `=> reportRequest()
                        `-> type=Timer

OutcomeTimer
`- + outcome:success|failure
    `-> Timer
```
