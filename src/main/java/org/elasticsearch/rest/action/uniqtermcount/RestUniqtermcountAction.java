
package org.elasticsearch.rest.action.uniqtermcount;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.action.support.RestActions.buildBroadcastShardsHeader;

import java.io.IOException;

import org.alg.elasticsearch.action.uniqtermcount.UniqtermcountAction;
import org.alg.elasticsearch.action.uniqtermcount.UniqtermcountRequest;
import org.alg.elasticsearch.action.uniqtermcount.UniqtermcountResponse;
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

public class RestUniqtermcountAction extends BaseRestHandler {

    @Inject
    public RestUniqtermcountAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(GET, "/_uniqtermcount", this);
        controller.registerHandler(GET, "/{index}/_uniqtermcount", this);
        controller.registerHandler(GET, "/{index}/{field}/_uniqtermcount", this);
    }

    public void handleRequest(final RestRequest request, final RestChannel channel) {
        UniqtermcountRequest uniqtermcountRequest = new UniqtermcountRequest(
                Strings.splitStringByCommaToArray(request.param("index")));
        uniqtermcountRequest.setField(request.param("field"));
        client.execute(UniqtermcountAction.INSTANCE, uniqtermcountRequest, new ActionListener<UniqtermcountResponse>() {

            public void onResponse(UniqtermcountResponse response) {
                try {
                    XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
                    builder.startObject();
                    buildBroadcastShardsHeader(builder, response);
                    builder.field("count", response.getCount());
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