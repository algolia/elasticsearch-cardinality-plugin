package org.alg.elasticsearch.search.aggregations.cardinality;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.aggregations.AggregationStreams;
import org.elasticsearch.search.aggregations.InternalAggregation;

import com.clearspring.analytics.stream.cardinality.CardinalityMergeException;
import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;

/**
*
*/
public class InternalCardinality extends InternalAggregation implements Cardinality {

    public final static Type TYPE = new Type("cardinality");

    public final static AggregationStreams.Stream STREAM = new AggregationStreams.Stream() {
        @Override
        public InternalCardinality readResult(StreamInput in) throws IOException {
            InternalCardinality result = new InternalCardinality();
            result.readFrom(in);
            return result;
        }
    };

    public static void registerStreams() {
        AggregationStreams.registerStream(STREAM, TYPE.stream());
    }

    private HyperLogLogPlus counter;

    InternalCardinality() {} // for serialization

    InternalCardinality(String name, HyperLogLogPlus counter) {
        super(name);
        this.counter = counter;
    }

    @Override
    public Type type() {
        return TYPE;
    }
    
    @Override
    public long getValue() {
        return counter == null ? 0 : counter.cardinality();
    }

    @Override
    public InternalCardinality reduce(ReduceContext reduceContext) {
        List<InternalAggregation> aggregations = reduceContext.aggregations();
        if (aggregations.size() == 1) {
            return (InternalCardinality) aggregations.get(0);
        }
        InternalCardinality reduced = null;
        for (InternalAggregation aggregation : aggregations) {
            if (reduced == null) {
                reduced = (InternalCardinality) aggregation;
            } else {
                try {
                    HyperLogLogPlus c = ((InternalCardinality) aggregation).counter;
                    if (c != null) {
                        if (reduced.counter == null) {
                            reduced.counter = c;
                        } else {
                            reduced.counter.addAll(c);
                        }
                    }
                } catch (CardinalityMergeException e) {
                    throw new Error("HyperLogLog merge failed", e);
                }
            }
        }
        if (reduced != null) {
            return reduced;
        }
        return (InternalCardinality) aggregations.get(0);
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        name = in.readString();
        if (in.readBoolean()) {
            int n = in.readInt();
            byte[] bytes = new byte[n];
            in.read(bytes);
            counter = HyperLogLogPlus.Builder.build(bytes);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(name);
        if (counter != null) {
            out.writeBoolean(true);
            byte[] bytes = counter.getBytes();
            out.writeInt(bytes.length);
            out.write(bytes);
        } else {
            out.writeBoolean(false);
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(name);
        builder.field(CommonFields.VALUE, getValue());
        builder.endObject();
        return builder;
    }

}
