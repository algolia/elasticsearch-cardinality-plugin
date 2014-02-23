package org.alg.elasticsearch.plugin.uniqtermcount;

import org.alg.elasticsearch.action.uniqtermcount.TransportUniqtermcountAction;
import org.alg.elasticsearch.action.uniqtermcount.UniqtermcountAction;
import org.elasticsearch.action.ActionModule;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.action.uniqtermcount.RestUniqtermcountAction;

public class UniqtermcountPlugin extends AbstractPlugin {

    public String name() {
        return "index-uniqtermcount";
    }

    public String description() {
        return "Index uniqtermcount for Elasticsearch";
    }

    public void onModule(RestModule module) {
        module.addRestAction(RestUniqtermcountAction.class);
    }

    public void onModule(ActionModule module) {
        module.registerAction(UniqtermcountAction.INSTANCE, TransportUniqtermcountAction.class);
    }

}
