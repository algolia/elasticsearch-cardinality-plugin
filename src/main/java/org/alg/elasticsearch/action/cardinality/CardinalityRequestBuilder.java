
package org.alg.elasticsearch.action.cardinality;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.broadcast.BroadcastOperationRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalGenericClient;

/**
 * A request to get cardinality of one or more indices.
 */
public class CardinalityRequestBuilder extends BroadcastOperationRequestBuilder<CardinalityRequest, CardinalityResponse, CardinalityRequestBuilder> {

    public CardinalityRequestBuilder(InternalGenericClient client) {
        super(client, new CardinalityRequest());
    }

    @Override
    protected void doExecute(ActionListener<CardinalityResponse> listener) {
        ((Client) client).execute(CardinalityAction.INSTANCE, request, listener);
    }
}
