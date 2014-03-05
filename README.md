Elasticsearch Cardinality Plugin
===================================

This plugin extends Elasticsearch providing a new type of aggregation and a REST action to estimate the cardinality (number of uniq terms) of a field.

Installation
------------

# Prerequisites:

 - Elasticsearch 1.0.0+

# Installation 

    ./plugin --url elasticsearch-cardinality-plugin-0.0.1.zip --install index-cardinality

# Uninstallation

    ./plugin --remove index-cardinality

Introduction
------------

This plugin uses the [HyperloglogPlus](http://research.google.com/pubs/pub40671.html) algorithm provided by the [Stream-lib](https://github.com/addthis/stream-lib) library to estimate the cardinality (uniq term count) of a field.

REST Action
-----------

```
curl -XGET http://localhost:9200/INDEX/FIELD/_cardinality
```

```json
{
  count: 42
}
```

Aggregation
-----------
```json
  aggregations: {
    uniq_users_count: {
      cardinality: {
        field: 'user'
      }
    }
  }
```

```json
  uniq_users_count: {
    value: 42
  }
```
