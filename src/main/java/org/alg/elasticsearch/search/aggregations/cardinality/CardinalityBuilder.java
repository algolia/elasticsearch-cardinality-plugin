package org.alg.elasticsearch.search.aggregations.cardinality;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;

public class CardinalityBuilder extends AggregationBuilder<CardinalityBuilder> {
    
    private String field;

    public CardinalityBuilder(String name) {
        super(name, InternalCardinality.TYPE.name());
    }

    public CardinalityBuilder field(String field) {
        this.field = field;
        return this;
    }

    @Override
    protected XContentBuilder internalXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (field != null) {
            builder.field("field", field);
        }
        return builder.endObject();
    }

}
