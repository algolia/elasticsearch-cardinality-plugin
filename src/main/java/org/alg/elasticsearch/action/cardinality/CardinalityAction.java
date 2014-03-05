
package org.alg.elasticsearch.action.cardinality;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalGenericClient;

public class CardinalityAction extends Action<CardinalityRequest, CardinalityResponse, CardinalityRequestBuilder> {

    public static final CardinalityAction INSTANCE = new CardinalityAction();

    public static final String NAME = "indices/cardinality";

    private CardinalityAction() {
        super(NAME);
    }

    @Override
    public CardinalityResponse newResponse() {
        return new CardinalityResponse();
    }

    @Override
    public CardinalityRequestBuilder newRequestBuilder(Client client) {
        return new CardinalityRequestBuilder((InternalGenericClient) client);
    }
}
