## 9.3.0

#### Project

* Bump up the embedded `swctl` version in OAP Docker image.

#### OAP Server

* Add component ID(133) for impala JDBC Java agent plugin and component ID(134) for impala server.
* Use prepareStatement in H2SQLExecutor#getByIDs.(No function change).
* Bump up snakeyaml to 1.32 for fixing CVE.
* Fix `DurationUtils.convertToTimeBucket` missed verify date format.
* Enhance LAL to support converting LogData to DatabaseSlowStatement.
* [**Breaking Change**] Change the LAL script format(Add layer property).
* Adapt ElasticSearch 8.1+, migrate from removed APIs to recommended APIs.
* Support monitoring MySQL slow SQLs.
* Support analyzing cache related spans to provide metrics and slow commands for cache services from client side
* Optimize virtual database, fix dynamic config watcher NPE when default value is null
* Remove physical index existing check and keep template existing check only to avoid meaningless `retry wait`
  in `no-init` mode.
* Make sure instance list ordered in TTL processor to avoid TTL timer never runs.
* Support monitoring PostgreSQL slow SQLs.
* [**Breaking Change**] Support sharding MySQL database instances and tables
  by [Shardingsphere-Proxy](https://shardingsphere.apache.org/document/current/en/overview/#shardingsphere-proxy).
  SQL-Database requires removing tables `log_tag/segment_tag/zipkin_query` before OAP starts, if bump up from previous
  releases.
* Fix meter functions `avgHistogram`, `avgHistogramPercentile`, `avgLabeled`, `sumHistogram` having data conflict when
  downsampling.
* Do sorting `readLabeledMetricsValues` result forcedly in case the storage(database) doesn't return data consistent
  with the parameter list.
* Fix the wrong watch semantics in Kubernetes watchers, which causes heavy traffic to API server in some Kubernetes
  clusters,
  we should use `Get State and Start at Most Recent` semantic instead of `Start at Exact`
  because we don't need the changing history events,
  see https://kubernetes.io/docs/reference/using-api/api-concepts/#semantics-for-watch.
* Unify query services and DAOs codes time range condition to `Duration`.
* [**Breaking Change**]: Remove prometheus-fetcher plugin, please use OpenTelemetry to scrape Prometheus metrics and
  set up SkyWalking OpenTelemetry receiver instead.
* BugFix: histogram metrics sent to MAL should be treated as OpenTelemetry style, not Prometheus style:
  ```
  (-infinity, explicit_bounds[i]] for i == 0
  (explicit_bounds[i-1], explicit_bounds[i]] for 0 < i < size(explicit_bounds)
  (explicit_bounds[i-1], +infinity) for i == size(explicit_bounds)
  ```
* Support Golang runtime metrics analysis.
* Add APISIX metrics monitoring
* Support skywalking-client-js report empty `service version` and `page path` , set default version as `latest` and
  default page path as `/`(root). Fix the
  error `fetching data (/browser_app_page_pv0) : Can't split endpoint id into 2 parts`.
* [**Breaking Change**] Limit the max length of trace/log/alarm tag's `key=value`, set the max length of column `tags`
  in tables`log_tag/segment_tag/alarm_record_tag` and column `query` in `zipkin_query` and column `tag_value` in `tag_autocomplete` to 256.
  SQL-Database requires altering these columns' length or removing these tables before OAP starts, if bump up from previous releases.
* Optimize the creation conditions of profiling task.
* Lazy load the Kubernetes metadata and switch from event-driven to polling.
  Previously we set up watchers to watch the Kubernetes metadata changes, this is perfect when there are deployments changes and
  SkyWalking can react to the changes in real time. However when the cluster has many events (such as in large cluster
  or some special Kubernetes engine like OpenShift), the requests sent from SkyWalking becomes unpredictable, i.e. SkyWalking might
  send massive requests to Kubernetes API server, causing heavy load to the API server.
  This PR switches from the watcher mechanism to polling mechanism, SkyWalking polls the metadata in a specified interval,
  so that the requests sent to API server is predictable (~10 requests every `interval`, 3 minutes), and the requests count is constant
  regardless of the cluster's changes. However with this change SkyWalking can't react to the cluster changes in time, but the delay
  is acceptable in our case.

#### UI

* Fix: tab active incorrectly, when click tab space
* Add impala icon for impala JDBC Java agent plugin.
* (Webapp)Bump up snakeyaml to 1.31 for fixing CVE-2022-25857
* [Breaking Change]: migrate from Spring Web to Armeria, now you should use the environment variable
  name `SW_OAP_ADDRESS`
  to change the OAP backend service addresses, like `SW_OAP_ADDRESS=localhost:12800,localhost:12801`, and use
  environment
  variable `SW_SERVER_PORT` to change the port. Other Spring-related configurations don't take effect anymore.
* Polish the endpoint list graph.
* Fix styles for an adaptive height.
* Fix setting up a new time range after clicking the refresh button.
* Enhance the process topology graph to support dragging nodes.
* UI-template: Fix metrics calculation in `general-service/mesh-service/faas-function` top-list dashboard.
* Update MySQL dashboard to visualize collected slow SQLs.
* Add virtual cache dashboard
* Remove `responseCode` fields of all OAL sources, as well as examples to avoid user's confusion.
* Remove All from the endpoints selector.
* Enhance menu configurations to make it easier to change.
* Update PostgreSQL dashboard to visualize collected slow SQLs.
* Add Golang runtime metrics and cpu/memory used rate panels in General-Instance dashboard
* Add gateway apisix menu
* Query logs with the specific service ID
* Bump d3-color from 3.0.1 to 3.1.0

#### Documentation

* Add `metadata-uid` setup doc about Kubernetes coordinator in the cluster management.
* Add a doc for adding menus to booster UI.
* Move general good read blogs from `Agent Introduction` to `Academy`.
* Add re-post for blog `Scaling with Apache SkyWalking` in the academy list.
* Add re-post for blog `Diagnose Service Mesh Network Performance with eBPF` in the academy list.
* Add **Security Notice** doc.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/149?closed=1)
