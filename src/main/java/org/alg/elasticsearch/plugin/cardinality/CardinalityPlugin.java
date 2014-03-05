package org.alg.elasticsearch.plugin.cardinality;

import org.alg.elasticsearch.action.cardinality.TransportCardinalityAction;
import org.alg.elasticsearch.action.cardinality.CardinalityAction;
import org.alg.elasticsearch.rest.action.cardinality.RestCardinalityAction;
import org.alg.elasticsearch.search.aggregations.cardinality.InternalCardinality;
import org.alg.elasticsearch.search.aggregations.cardinality.CardinalityParser;
import org.elasticsearch.action.ActionModule;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.search.aggregations.AggregationModule;

public class CardinalityPlugin extends AbstractPlugin {

    public String name() {
        return "index-cardinality";
    }

    public String description() {
        return "Index cardinality for Elasticsearch";
    }

    public void onModule(RestModule module) {
        module.addRestAction(RestCardinalityAction.class);
    }

    public void onModule(ActionModule module) {
        module.registerAction(CardinalityAction.INSTANCE, TransportCardinalityAction.class);
    }
    
    public void onModule(AggregationModule module) {
        module.addAggregatorParser(CardinalityParser.class);
        InternalCardinality.registerStreams();
    }

}
