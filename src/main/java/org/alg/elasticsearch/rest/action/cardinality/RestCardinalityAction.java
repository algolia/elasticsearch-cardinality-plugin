
package org.alg.elasticsearch.rest.action.cardinality;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.action.support.RestActions.buildBroadcastShardsHeader;

import java.io.IOException;

import org.alg.elasticsearch.action.cardinality.CardinalityAction;
import org.alg.elasticsearch.action.cardinality.CardinalityRequest;
import org.alg.elasticsearch.action.cardinality.CardinalityResponse;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.XContentRestResponse;
import org.elasticsearch.rest.XContentThrowableRestResponse;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

public class RestCardinalityAction extends BaseRestHandler {

    @Inject
    public RestCardinalityAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(GET, "/_cardinality", this);
        controller.registerHandler(POST, "/_cardinality", this);
        controller.registerHandler(GET, "/{index}/_cardinality", this);
        controller.registerHandler(POST, "/{index}/_cardinality", this);
        controller.registerHandler(GET, "/{index}/{field}/_cardinality", this);
        controller.registerHandler(POST, "/{index}/{field}/_cardinality", this);
    }

    public void handleRequest(final RestRequest request, final RestChannel channel) {
        CardinalityRequest cardinalityRequest = new CardinalityRequest(
                Strings.splitStringByCommaToArray(request.param("index")));
        cardinalityRequest.setField(request.param("field"));
        client.execute(CardinalityAction.INSTANCE, cardinalityRequest, new ActionListener<CardinalityResponse>() {

            public void onResponse(CardinalityResponse response) {
                try {
                    XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
                    builder.startObject();
                    buildBroadcastShardsHeader(builder, response);
                    builder.field("count", response.getCount());
                    builder.endObject();
                    channel.sendResponse(new XContentRestResponse(request, OK, builder));
                } catch (Exception e) {
                    onFailure(e);
                }
            }

            public void onFailure(Throwable e) {
                try {
                    channel.sendResponse(new XContentThrowableRestResponse(request, e));
                } catch (IOException e1) {
                    logger.error("Failed to send failure response", e1);
                }
            }
        });
    }
}