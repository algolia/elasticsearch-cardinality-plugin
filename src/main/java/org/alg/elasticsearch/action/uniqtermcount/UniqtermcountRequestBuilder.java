
package org.alg.elasticsearch.action.uniqtermcount;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.broadcast.BroadcastOperationRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalGenericClient;

/**
 * A request to get uniqtermcount of one or more indices.
 */
public class UniqtermcountRequestBuilder extends BroadcastOperationRequestBuilder<UniqtermcountRequest, UniqtermcountResponse, UniqtermcountRequestBuilder> {

    public UniqtermcountRequestBuilder(InternalGenericClient client) {
        super(client, new UniqtermcountRequest());
    }

    @Override
    protected void doExecute(ActionListener<UniqtermcountResponse> listener) {
        ((Client) client).execute(UniqtermcountAction.INSTANCE, request, listener);
    }
}
