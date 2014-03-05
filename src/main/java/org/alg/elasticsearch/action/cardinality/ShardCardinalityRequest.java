
package org.alg.elasticsearch.action.cardinality;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

class ShardCardinalityRequest extends BroadcastShardOperationRequest {

    private String field;

    ShardCardinalityRequest() {
    }

    public ShardCardinalityRequest(String index, int shardId, CardinalityRequest request) {
        super(index, shardId, request);
        this.field = request.getField();
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        field = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeOptionalString(field);
    }
}