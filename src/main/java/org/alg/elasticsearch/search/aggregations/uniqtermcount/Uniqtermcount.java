package org.alg.elasticsearch.search.aggregations.uniqtermcount;

import org.elasticsearch.search.aggregations.Aggregation;

/**
 *
 */
public interface Uniqtermcount extends Aggregation {
    long getValue();
}
