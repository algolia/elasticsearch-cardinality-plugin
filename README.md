Elasticsearch Uniq Term Count Plugin
===================================

This plugin extends Elasticsearch with a uniq term count capabilit. Uniq terms count can be generated from indexes, or even of all of the
indexes in the cluster.

**Note: ** This plugin is in alpha version.

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

This plugin uses the [HyperloglogPlus](http://research.google.com/pubs/pub40671.html) implemented by the library [Stream-lib](https://github.com/addthis/stream-lib) to count statistically the cardinality. 

Example
--------

    aggregations: {
      uniq_queries: {
        uniqtermcount: {
          field: 'user'
        }
      }
    }
