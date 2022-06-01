# temporal-newrelic
A couple of useful classes to integrate NewRelic into Temporal Java SDK applications

[![Java CI with Maven](https://github.com/javiercanillas/temporal-newrelic/actions/workflows/maven-build.yml/badge.svg)](https://github.com/javiercanillas/temporal-newrelic/actions/workflows/maven-build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=javiercanillas_temporal-newrelic&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=javiercanillas_temporal-newrelic)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=javiercanillas_temporal-newrelic&metric=coverage)](https://sonarcloud.io/summary/new_code?id=javiercanillas_temporal-newrelic)

## Workflow & Activity executions as NewRelic Transaction

By default, workflow execution and activity methods will not be traced by NewRelic (like it happens with web requests), so manual
instrumentation is required to hint NewRelic about this kind of "background executions".

In order to support this without messing up business logic code, Temporal has an interface to intercept calls to activities
and workflows by wrapping those executions with interceptors implementing `WorkflowInboundCallsInterceptor` and `ActivityInboundCallsInterceptor`. 
By implementing those interface and adding required NewRelic instrumentation, we will achieve visibility of those executions
in NewRelic platform. 

To enable this, we only need to configure `TraceWorkerInterceptor` when configuring `WorkerFactory`, like this:
```java
WorkflowClient workflowClient = ...
var factory =
    WorkerFactory.newInstance(
        workflowClient,
        WorkerFactoryOptions.newBuilder()
            .setWorkerInterceptors(new TraceWorkerInterceptor())
            .build());
```

Moreover, if [Distributed Tracing](https://docs.newrelic.com/docs/distributed-tracing/concepts/introduction-distributed-tracing/) is enabled
for your application, it will automatically use it, see below.

## Distributed Tracing 

Temporal has an interface to enable context information propagation between executions (threads). By implementing this
interface we can pass through all [Distributed Tracing](https://docs.newrelic.com/docs/distributed-tracing/concepts/introduction-distributed-tracing/) 
information. This includes not only from the client to the workers, but also between workflows and activities.

To enable this you only need to add `NewRelicDistributedTraceContextPropagator` to ``, during initialization of workflow clients like this:
```java
 WorkflowServiceStubs service = ...
 var workflowClientOptionsBuilder = WorkflowClientOptions.newBuilder();
 workflowClientOptionsBuilder.setContextPropagators(
      List.of(new NewRelicDistributedTraceContextPropagator()));
 WorkflowClient.newInstance(service, workflowClientOptionsBuilder.validateAndBuildWithDefaults())
```
Any unhandled error happening inside `NewRelicDistributedTraceContextPropagator` will probably mess up the workflow execution,
so I do recommend to wrap this propagator with `SilentWrapperContextPropagator` to log and discard any unhandled error related 
with propagation (not the workflow execution itself), like this:

```java
var propagator = SilentWrapperContextPropagator.wrap(new NewRelicDistributedTraceContextPropagator());
```

## Metric Scope
Temporal also has the capability to report its internal metrics to external services. To be able to send this information
to NewRelic as custom metrics you can configure `MetricScopeReporter` as following:
```java
WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(WorkflowServiceStubsOptions.newBuilder()
            .setMetricsScope(MetricScopeReporter.getScope())
            .validateAndBuildWithDefaults());
```

This will produce metrics starting with the pattern `Custom/temporalio/`. At the moment there is support for:
* timer: `Custom/temporalio/timer`
* gauge: `Custom/temporalio/gauge`
* counter: `Custom/temporalio/counter`

After that is appended the following relevant information:
*   namespace
*   worker_type
*   task_queue
*   workflow_type
*   operation_name
*   signal_name
*   activity_type
*   query_type
*   exception
*   status_code

If any of this information is not present on the reported record, the word `none` will take its place instead of null. For example:

```java
MetricScopeReporter.instance().reportCounter("name", Map.of(), 1L);
```
will append the following metric: `Custom/temporalio/timer/none/none/none/none/none/none/none/none/none/none/name` with the given value.

Furthermore, `timer`s will be reported using `NewRelic.recordResponseTimeMetric`, `counter`s with `NewRelic.recordResponseTimeMetric` 
and finally `gauge`s with `NewRelic.recordMetric`.

## How to install
If you prefer to use maven central releases, you can find it [here](https://search.maven.org/artifact/io.github.javiercanillas/temporal-newrelic). Also, if you support [Jitpack.io](https://jitpack.io/) you can find it [here](https://jitpack.io/#javiercanillas/temporal-newrelic)

