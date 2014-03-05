package org.alg.elasticsearch.search.aggregations.cardinality;

import org.elasticsearch.search.aggregations.Aggregation;

/**
 *
 */
public interface Cardinality extends Aggregation {
    long getValue();
}
