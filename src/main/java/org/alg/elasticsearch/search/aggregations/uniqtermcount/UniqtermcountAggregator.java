package org.alg.elasticsearch.search.aggregations.uniqtermcount;

import java.io.IOException;

import org.elasticsearch.common.lease.Releasables;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.util.ObjectArray;
import org.elasticsearch.index.fielddata.BytesValues;
import org.elasticsearch.search.aggregations.Aggregator;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.bucket.SingleBucketAggregator;
import org.elasticsearch.search.aggregations.support.AggregationContext;
import org.elasticsearch.search.aggregations.support.ValueSourceAggregatorFactory;
import org.elasticsearch.search.aggregations.support.ValuesSource;
import org.elasticsearch.search.aggregations.support.ValuesSourceConfig;

import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;

/**
 *
 */
public class UniqtermcountAggregator extends SingleBucketAggregator {
    
    private ValuesSource valuesSource;
    
    private ObjectArray<HyperLogLogPlus> counters;

    public UniqtermcountAggregator(String name, AggregatorFactories factories, long estimatedBucketsCount, ValuesSource valuesSource, AggregationContext aggregationContext, Aggregator parent) {
        super(name, factories, aggregationContext, parent);
        this.valuesSource = valuesSource;
        if (valuesSource != null) {
            final long initialSize = estimatedBucketsCount < 2 ? 1 : estimatedBucketsCount;
            this.counters = BigArrays.newObjectArray(initialSize, context.pageCacheRecycler());
        }
    }
    
    @Override
    public boolean shouldCollect() {
        return this.valuesSource != null;
    }

    @Override
    public void collect(int doc, long owningBucketOrdinal) throws IOException {
        assert this.valuesSource != null : "should collect first";
        
        BytesValues values = this.valuesSource.bytesValues();
        if (values == null) {
            return;
        }

        this.counters = BigArrays.grow(this.counters, owningBucketOrdinal + 1);

        HyperLogLogPlus counter = counters.get(owningBucketOrdinal);
        if (counter == null) {
            counter = new HyperLogLogPlus(15, 15);
            counters.set(owningBucketOrdinal, counter);
        }

        final int valuesCount = values.setDocument(doc);
        for (int i = 0; i < valuesCount; i++) {
            counter.offer(values.nextValue());
        }
    }

    @Override
    public InternalAggregation buildAggregation(long owningBucketOrdinal) {
        return new InternalUniqtermcount(name, counters == null ? null : counters.get(owningBucketOrdinal));
    }

    @Override
    public InternalAggregation buildEmptyAggregation() {
        return new InternalUniqtermcount(name, null);
    }

    public static class Factory extends ValueSourceAggregatorFactory<ValuesSource> {
        public Factory(String name, ValuesSourceConfig<ValuesSource> valueSourceConfig) {
            super(name, InternalUniqtermcount.TYPE.name(), valueSourceConfig);
        }

        @Override
        public Aggregator create(ValuesSource valuesSource, long expectedBucketsCount, AggregationContext aggregationContext, Aggregator parent) {
            return new UniqtermcountAggregator(name, factories, expectedBucketsCount, valuesSource, aggregationContext, parent);
        }

        @Override
        protected Aggregator createUnmapped(AggregationContext aggregationContext, Aggregator parent) {
            return new UniqtermcountAggregator(name, factories, 0, null, aggregationContext, parent);
        }
    }

    @Override
    public void doRelease() {
        if (this.counters != null) {
            Releasables.release(this.counters);
        }
    }
}
