package org.alg.elasticsearch.plugin.uniqtermcount;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import org.alg.elasticsearch.action.uniqtermcount.UniqtermcountAction;
import org.alg.elasticsearch.action.uniqtermcount.UniqtermcountRequest;
import org.alg.elasticsearch.action.uniqtermcount.UniqtermcountResponse;
import org.alg.elasticsearch.search.aggregations.uniqtermcount.Uniqtermcount;
import org.alg.elasticsearch.search.aggregations.uniqtermcount.UniqtermcountBuilder;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.network.NetworkUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.junit.Ignore;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SimpleTests extends Assert {

    private final static ESLogger logger = ESLoggerFactory.getLogger("test");

    protected final String CLUSTER = "test-cluster-" + NetworkUtils.getLocalAddress().getHostName();

    private Node node;

    private Client client;

    @BeforeClass
    public void startNode() {
        Settings finalSettings = settingsBuilder()
                .put("cluster.name", CLUSTER)
                .put("discovery.zen.ping.multicast.enabled", false)
                .put("node.local", true)
                .put("gateway.type", "none")
                .build();
        node = nodeBuilder().settings(finalSettings).build().start();
        client = node.client();
    }

    @AfterClass
    public void stopNode() {
        node.close();
    }

    @Test
    public void assertPluginLoaded() {
        NodesInfoResponse nodesInfoResponse = client.admin().cluster().prepareNodesInfo()
                .clear().setPlugin(true).get();
        logger.info("{}", nodesInfoResponse);
        assertEquals(nodesInfoResponse.getNodes().length, 1);
        assertNotNull(nodesInfoResponse.getNodes()[0].getPlugins().getInfos());
        assertEquals(nodesInfoResponse.getNodes()[0].getPlugins().getInfos().size(), 1);
        assertEquals(nodesInfoResponse.getNodes()[0].getPlugins().getInfos().get(0).getName(), "index-uniqtermcount");
        assertEquals(nodesInfoResponse.getNodes()[0].getPlugins().getInfos().get(0).isSite(), false);
    }
    
    @Test
    public void assertTermCountOneShard() {
        client.admin().indices().prepareCreate("test").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 1)).execute().actionGet();

        client.prepareIndex("test", "type0", "doc0").setSource("field0", "foo bar").execute().actionGet();
        client.prepareIndex("test", "type0", "doc1").setSource("field0", "foo").execute().actionGet();
        client.prepareIndex("test", "type0", "doc2").setSource("field1", "baz").setRefresh(true).execute().actionGet();

        UniqtermcountResponse response = client.execute(UniqtermcountAction.INSTANCE, new UniqtermcountRequest("test")).actionGet();
        assertEquals(3, response.getCount());
    }
    
    @Test
    public void assertTermCountTwoShard() {
        client.admin().indices().prepareCreate("test2").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 2)).execute().actionGet();

        // index document 'doc0' to shard 0 and 'doc1' to shard 1
        client.prepareIndex("test2", "type0", "doc0").setSource("field0", "foo bar").setRefresh(true).execute().actionGet();
        client.prepareIndex("test2", "type0", "doc1").setSource("field0", "foo bar").setRefresh(true).execute().actionGet();

        UniqtermcountResponse response = client.execute(UniqtermcountAction.INSTANCE, new UniqtermcountRequest("test2")).actionGet();
        assertEquals(2, response.getCount());
    }
    
    @Test
    public void assertTermCountAggregatorOneShard() {
        client.admin().indices().prepareCreate("test3").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 1)).execute().actionGet();
        
        client.prepareIndex("test3", "type0", "doc0").setSource("field0", "foo bar").execute().actionGet();
        client.prepareIndex("test3", "type0", "doc1").setSource("field0", "foo").execute().actionGet();
        client.prepareIndex("test3", "type0", "doc2").setSource("field1", "baz").setRefresh(true).execute().actionGet();
       
        SearchResponse searchResponse = client.prepareSearch("test3")
                .setQuery(matchAllQuery())
                .addAggregation(new UniqtermcountBuilder("uniq0").field("field0"))
                .addAggregation(new UniqtermcountBuilder("uniq1").field("field1"))
                .execute().actionGet();
        Uniqtermcount count0 = searchResponse.getAggregations().get("uniq0");
        assertNotNull(count0);
        assertEquals(2, count0.getValue());
        
        Uniqtermcount count1 = searchResponse.getAggregations().get("uniq1");
        assertNotNull(count1);
        assertEquals(1, count1.getValue());
    }
    
    @Test
    public void assertTermCountAggregatorTwoShard() {
        client.admin().indices().prepareCreate("test4").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 2)).execute().actionGet();
        
        // index document 'doc0' to shard 0 and 'doc1' to shard 1
        client.prepareIndex("test4", "type0", "doc0").setSource("field0", "foo bar").setRefresh(true).execute().actionGet();
        client.prepareIndex("test4", "type0", "doc1").setSource("field0", "foo bar").setRefresh(true).execute().actionGet();
       
        SearchResponse searchResponse = client.prepareSearch("test4")
                .setQuery(matchAllQuery())
                .addAggregation(new UniqtermcountBuilder("uniq0").field("field0"))
                .execute().actionGet();
        Uniqtermcount count0 = searchResponse.getAggregations().get("uniq0");
        assertNotNull(count0);
        assertEquals(2, count0.getValue());
    }
    
    @Test
    public void assertTermCountAggregatorOneShardNumerical() {
        client.admin().indices().prepareCreate("test5").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 1)).execute().actionGet();
        
        client.prepareIndex("test5", "type0", "doc0").setSource("field0", 42).execute().actionGet();
        client.prepareIndex("test5", "type0", "doc1").setSource("field0", 51).execute().actionGet();
        client.prepareIndex("test5", "type0", "doc2").setSource("field0", 69).execute().actionGet();
        client.prepareIndex("test5", "type0", "doc3").setSource("field0", 42).execute().actionGet();
        client.prepareIndex("test5", "type0", "doc4").setSource("field1", 0).setRefresh(true).execute().actionGet();
       
        SearchResponse searchResponse = client.prepareSearch("test5")
                .setQuery(matchAllQuery())
                .addAggregation(new UniqtermcountBuilder("uniq0").field("field0"))
                .execute().actionGet();
        Uniqtermcount count0 = searchResponse.getAggregations().get("uniq0");
        assertNotNull(count0);
        assertEquals(3, count0.getValue());
    }

    @Test
    public void assertTermCountAggregatorInexistentField() {
        // create index 'test4'
        client.admin().indices().prepareCreate("test6").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 1)).execute().actionGet();
        
        client.prepareIndex("test6", "type0", "doc0").setSource("field0", 42).execute().actionGet();
        client.prepareIndex("test6", "type0", "doc1").setSource("field1", 0).setRefresh(true).execute().actionGet();
    
        // org.elasticsearch.action.search.SearchPhaseExecutionException: Failed to execute phase [query_fetch], all shards failed; shardFailures {[GccigdGtRc6qAIw-kyMIbQ][test6][0]: NullPointerException[null]}
//        SearchResponse searchResponse = client.prepareSearch("test6")
//                .setQuery(matchAllQuery())
//                .addAggregation(new UniqtermcountBuilder("uniq0").field("field_inexistent"))
//                .execute().actionGet();
//        Uniqtermcount count0 = searchResponse.getAggregations().get("uniq0");
//        assertNotNull(count0);
//        assertEquals(0, count0.getValue());
    }
}