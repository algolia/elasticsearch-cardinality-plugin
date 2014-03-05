
package org.alg.elasticsearch.action.cardinality;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

/**
 * A response for cardinality action.
 */
public class CardinalityResponse extends BroadcastOperationResponse {

    private long count;

    CardinalityResponse() {
    }

    CardinalityResponse(int totalShards, int successfulShards, int failedShards,
                     List<ShardOperationFailedException> shardFailures, long count) {
        super(totalShards, successfulShards, failedShards, shardFailures);
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        this.count = in.readLong();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeLong(count);
    }
}