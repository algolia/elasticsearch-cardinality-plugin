Elasticsearch Cardinality Plugin
===================================

This plugin extends Elasticsearch providing a fast & memory-efficient way to estimate the cardinality (number of uniq terms) of a field. The field can be either string, numerical or boolean. The plugin registers a new type of aggregation (`cardinality`) and a REST action (`_cardinality`).

We _love_ pull-requests!

### Prerequisites:

 - Elasticsearch 1.0.0+

### Binaries

 - Compiled versions of the plugin are stored in the [`dist`](https://github.com/algolia/elasticsearch-cardinality-plugin/tree/master/dist) directory.

## Principle

This plugin uses the [HyperloglogPlus](http://research.google.com/pubs/pub40671.html) algorithm provided by the [Stream-lib](https://github.com/addthis/stream-lib) library to estimate the cardinality (uniq term count) of a field. Basically, it estimates the number of uniq values of a field without loading all of them into RAM. The merge between shards and between indices is supported (and efficient).

Without such plugin, the only way to count the uniq number of values in a field was to retrieve all values on the client-side and to count the length of the resulting array (Totally inefficient).

## REST Action

To estimate the cardinality of a field, use the following REST action:

```
curl -XGET http://localhost:9200/{index}/{field}/_cardinality
```

For example, to estimate the number of uniq IPs in the index `logstash-2014.02.03`:

```
curl -XGET http://localhost:9200/logstash-2014.02.03/ip/_cardinality
```

```json
{
	"_shards": {
		"total": 2,
		"successful": 2,
		"failed": 0
	},
	"count": 46367
}
```

To estimate the number of uniq IPs in several indices:

```
curl -XGET http://localhost:9200/logstash-2014.01.*/ip/_cardinality
```

```json
{
	"_shards": {
		"total": 86,
		"successful": 86,
		"failed": 0
	},
	"count": 919979
}
```


## Aggregation

To build an aggregation estimating the cardinality of a field, use the following code:

```json
{
  "aggregations": {
    "<aggregation_name>": {
      "cardinality": {
        "field": "<field_name>"
      }
    }
  }
}
```

For example, to estimate the number of uniq IPs in a result set, use the following code:

```json
{
  "aggregations": {
    "uniq_ips": {
      "cardinality": {
        "field": "ip"
      }
    }
  }
}
```

```json
{
  "aggregations": {
    "uniq_ips": {
      "value": 42
    }
  }
}
```

## Setup

### Installation 

    ./plugin --url elasticsearch-cardinality-plugin-0.0.1.zip --install index-cardinality

### Uninstallation

    ./plugin --remove index-cardinality
