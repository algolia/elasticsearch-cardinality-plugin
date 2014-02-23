
package org.alg.elasticsearch.action.uniqtermcount;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalGenericClient;

public class UniqtermcountAction extends Action<UniqtermcountRequest, UniqtermcountResponse, UniqtermcountRequestBuilder> {

    public static final UniqtermcountAction INSTANCE = new UniqtermcountAction();

    public static final String NAME = "indices/uniqtermcount";

    private UniqtermcountAction() {
        super(NAME);
    }

    @Override
    public UniqtermcountResponse newResponse() {
        return new UniqtermcountResponse();
    }

    @Override
    public UniqtermcountRequestBuilder newRequestBuilder(Client client) {
        return new UniqtermcountRequestBuilder((InternalGenericClient) client);
    }
}
