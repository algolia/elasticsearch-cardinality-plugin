package org.alg.elasticsearch.search.aggregations.uniqtermcount;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;

public class UniqtermcountBuilder extends AggregationBuilder<UniqtermcountBuilder> {
    
    private String field;

    public UniqtermcountBuilder(String name) {
        super(name, InternalUniqtermcount.TYPE.name());
    }

    public UniqtermcountBuilder field(String field) {
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
