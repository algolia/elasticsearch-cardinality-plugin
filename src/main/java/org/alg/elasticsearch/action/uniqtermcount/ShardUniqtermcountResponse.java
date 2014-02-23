
package org.alg.elasticsearch.action.uniqtermcount;

import java.io.IOException;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;

class ShardUniqtermcountResponse extends BroadcastShardOperationResponse {

    private HyperLogLogPlus counter;

    ShardUniqtermcountResponse() {
    }

    public ShardUniqtermcountResponse(String index, int shardId, HyperLogLogPlus counter) {
        super(index, shardId);
        this.counter = counter;
    }

    public HyperLogLogPlus getCounter() {
        return counter;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int n = in.readInt();
        byte[] bytes = new byte[n];
        in.read(bytes);
        this.counter = HyperLogLogPlus.Builder.build(bytes);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        byte[] bytes = this.counter.getBytes();
        out.writeInt(bytes.length);
        out.write(bytes);
    }
}