Elasticsearch Uniq Term Count Plugin
===================================

This plugin extends Elasticsearch providing a new type of aggregation and a REST action to estimate the cardinality (number of uniq terms) of a field.

**Note:** This plugin is currently at an early stage of development.

Installation
------------

# Prerequisites:

 - Elasticsearch 1.0.0+

# Installation 

    ./plugin --url elasticsearch-uniqtermcount-plugin-0.0.1.zip --install index-uniqtermcount

# Uninstallation

    ./plugin --remove index-uniqtermcount

Introduction
------------

This plugin uses the [HyperloglogPlus](http://research.google.com/pubs/pub40671.html) algorithm provided by the [Stream-lib](https://github.com/addthis/stream-lib) library to estimate the cardinality (uniq term count) of a field.

REST Action
-----------

```
curl -XGET http://localhost:9200/INDEX/FIELD/_uniqtermcount
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
    uniq_user_count: {
      uniqtermcount: {
        field: 'user'
      }
    }
  }
```

```json
  uniq_user_count: {
    value: 42
  }
```
