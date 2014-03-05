package org.alg.elasticsearch.plugin.uniqtermcount;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
import org.elasticsearch.node.Node;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SimpleTests extends Assert {

    private final static ESLogger logger = ESLoggerFactory.getLogger("test");

    protected final String CLUSTER = "test-cluster-" + NetworkUtils.getLocalAddress().getHostName();

    private Node node;
    private Node node2;

    private Client client;

    @BeforeClass
    public void startNode() {
        ImmutableSettings.Builder finalSettings = settingsBuilder()
                .put("cluster.name", CLUSTER)
                .put("discovery.zen.ping.multicast.enabled", false)
                .put("node.local", true)
                .put("gateway.type", "none");
        node = nodeBuilder().settings(finalSettings.put("node.name", "node1").build()).build().start();
        node2 = nodeBuilder().settings(finalSettings.put("node.name", "node2").build()).build().start();

        client = node.client();
    }

    @AfterClass
    public void stopNode() {
        node.close();
        node2.close();
    }

    @Test
    public void assertPluginLoaded() {
        NodesInfoResponse nodesInfoResponse = client.admin().cluster().prepareNodesInfo()
                .clear().setPlugin(true).get();
        logger.info("{}", nodesInfoResponse);
        assertEquals(nodesInfoResponse.getNodes().length, 2);
        assertNotNull(nodesInfoResponse.getNodes()[0].getPlugins().getInfos());
        assertEquals(nodesInfoResponse.getNodes()[0].getPlugins().getInfos().size(), 1);
        assertEquals(nodesInfoResponse.getNodes()[0].getPlugins().getInfos().get(0).getName(), "index-uniqtermcount");
        assertEquals(nodesInfoResponse.getNodes()[0].getPlugins().getInfos().get(0).isSite(), false);
    }
    
    @Test
    public void assertTermCountActionOneShardString() {
        client.admin().indices().prepareCreate("test").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 1)).execute().actionGet();

        client.prepareIndex("test", "type0", "doc0").setSource("field0", "foo bar").setRefresh(true).execute().actionGet();
        client.prepareIndex("test", "type0", "doc1").setSource("field0", "foo").setRefresh(true).execute().actionGet();
        client.prepareIndex("test", "type0", "doc2").setSource("field1", "baz").setRefresh(true).execute().actionGet();

        UniqtermcountResponse response = client.execute(UniqtermcountAction.INSTANCE, new UniqtermcountRequest("test")).actionGet();
        assertEquals(response.getCount(), 3);

        response = client.execute(UniqtermcountAction.INSTANCE, new UniqtermcountRequest("test").withField("field0")).actionGet();
        assertEquals(response.getCount(), 2);

        response = client.execute(UniqtermcountAction.INSTANCE, new UniqtermcountRequest("test").withField("field1")).actionGet();
        assertEquals(response.getCount(), 1);
    }
    
    @Test
    public void assertTermCountActionOneShardNumerical() {
        client.admin().indices().prepareCreate("test1").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 1)).execute().actionGet();

        client.prepareIndex("test1", "type0", "doc0").setSource("field2", 42).execute().actionGet();
        client.prepareIndex("test1", "type0", "doc1").setSource("field2", 42).execute().actionGet();
        client.prepareIndex("test1", "type0", "doc2").setSource("field2", 51).setRefresh(true).execute().actionGet();

        UniqtermcountResponse response = client.execute(UniqtermcountAction.INSTANCE, new UniqtermcountRequest("test1").withField("field2")).actionGet();
        assertEquals(response.getCount(), 2);
    }
    
    @Test
    public void assertTermCountActionTwoShard() {
        client.admin().indices().prepareCreate("test2").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 2)).execute().actionGet();

        // index document 'doc0' to shard 0 and 'doc1' to shard 1
        client.prepareIndex("test2", "type0", "doc0").setSource("field0", "foo bar").setRefresh(true).execute().actionGet();
        client.prepareIndex("test2", "type0", "doc1").setSource("field0", "foo bar").setRefresh(true).execute().actionGet();

        UniqtermcountResponse response = client.execute(UniqtermcountAction.INSTANCE, new UniqtermcountRequest("test2")).actionGet();
        assertEquals(2, response.getCount());
    }

    @Test
    public void assertTermCountActionTwoIndices() {
        client.admin().indices().prepareCreate("test2a").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 1)).execute().actionGet();
        client.admin().indices().prepareCreate("test2b").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 1)).execute().actionGet();

        // index document 'doc0' to shard 0 and 'doc1' to shard 1

        client.prepareIndex("test2a", "type0", "doc1").setSource("field1", 42).setRefresh(true).execute().actionGet();
        client.prepareIndex("test2a", "type0", "doc2").setSource("field2", 2).setRefresh(true).execute().actionGet();
        client.prepareIndex("test2a", "type0", "doc3").setSource("field3", "1").setRefresh(true).execute().actionGet();
        client.prepareIndex("test2a", "type0", "doc4").setSource("field4", "2324332").setRefresh(true).execute().actionGet();
        client.prepareIndex("test2a", "type0", "doc0").setSource("field0", 3).setRefresh(true).execute().actionGet();
        UniqtermcountResponse response = client.execute(UniqtermcountAction.INSTANCE, new UniqtermcountRequest("test2a")).actionGet();
        assertEquals(5, response.getCount());
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
    public void assertTermCountAggregatorLotsOfEmptyShard() {
        client.admin().indices().prepareCreate("test6").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 1)).execute().actionGet();
        
        client.prepareIndex("test6", "type0", "doc0").setSource("field0", 42).execute().actionGet();
        client.prepareIndex("test6", "type0", "doc1").setSource("field1", 0).setRefresh(true).execute().actionGet();
    
        SearchResponse searchResponse = client.prepareSearch("test6")
                .setQuery(matchAllQuery())
                .addAggregation(new UniqtermcountBuilder("uniq0").field("field_inexistent"))
                .execute().actionGet();
        Uniqtermcount count0 = searchResponse.getAggregations().get("uniq0");
        assertNotNull(count0);
        assertEquals(0, count0.getValue());
    }

    @Test
    public void assertTermCountAggregatorInexistentField() {
        client.admin().indices().prepareCreate("test7").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 8)).execute().actionGet();
        
        for (int i = 0; i < 100; ++i) {
            client.prepareIndex("test7", "type0", "doc" + i).setSource("field0", i).execute().actionGet();
        }
        client.prepareIndex("test7", "type0", "doc1").setSource("field1", 1).setRefresh(true).execute().actionGet();
    
        SearchResponse searchResponse = client.prepareSearch("test7")
                .setQuery(matchAllQuery())
                .addAggregation(new UniqtermcountBuilder("uniq0").field("field1"))
                .execute().actionGet();
        Uniqtermcount count0 = searchResponse.getAggregations().get("uniq0");
        assertNotNull(count0);
        assertEquals(1, count0.getValue());
    }

    @Test
    public void assertTermCountAggregatorStressTestOneShard() {
        client.admin().indices().prepareCreate("test8").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 1)).execute().actionGet();

        for (int i = 0; i < 100000; ++i) {
            client.prepareIndex("test8", "type0", "doc" + i).setSource("field0", i).execute().actionGet();
        }
        for (int i = 100000; i < 200000; ++i) {
            client.prepareIndex("test8", "type0", "doc" + i).setSource("field0", null).execute().actionGet();
        }
        client.prepareIndex("test8", "type0", "docb").setSource("field1", null).setRefresh(true).execute().actionGet();

        SearchResponse searchResponse = client.prepareSearch("test8")
                .setQuery(matchAllQuery())
                .addAggregation(new UniqtermcountBuilder("uniq0").field("field0"))
                .execute().actionGet();
        Uniqtermcount count = searchResponse.getAggregations().get("uniq0");
        assertNotNull(count);
        double precision = ((double)count.getValue() / 100000.);
        assertTrue(precision >= 0.98, "The value is " + precision);
    }

    @Test
    public void assertTermCountAggregatorStressTestEightShard() {
        client.admin().indices().prepareCreate("test9").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 8)).execute().actionGet();

        for (int i = 0; i < 100000; ++i) {
            client.prepareIndex("test9", "type0", "doc" + i).setSource("field0", i).execute().actionGet();
        }
        client.prepareIndex("test9", "type0", "docb").setSource("field0", null).setRefresh(true).execute().actionGet();

        SearchResponse searchResponse = client.prepareSearch("test9")
                .setQuery(matchAllQuery())
                .addAggregation(new UniqtermcountBuilder("uniq0").field("field0"))
                .execute().actionGet();
        Uniqtermcount count = searchResponse.getAggregations().get("uniq0");
        assertNotNull(count);
        double precision = ((double)count.getValue() / 100000.);
        assertTrue(precision >= 0.87, "The value is " + precision);
    }

    @Test
    public void assertTermCountAggregatorStressTestTwoIndexes() {
        client.admin().indices().prepareCreate("test10a").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 8)).execute().actionGet();
        client.admin().indices().prepareCreate("test10b").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 8)).execute().actionGet();

        for (int i = 0; i < 100000; ++i) {
            client.prepareIndex("test10a", "type0", "doc" + i).setSource("field0", i).execute().actionGet();
        }
        client.prepareIndex("test10a", "type0", "docb").setSource("field0", null).setRefresh(true).execute().actionGet();

        for (int i = 0; i < 100000; ++i) {
            client.prepareIndex("test10b", "type0", "doc" + (i * 10)).setSource("field0", i * 10).execute().actionGet();
        }
        client.prepareIndex("test10b", "type0", "docb").setSource("field0", null).setRefresh(true).execute().actionGet();

        SearchResponse searchResponse = client.prepareSearch("test10*")
                .setQuery(matchAllQuery())
                .addAggregation(new UniqtermcountBuilder("uniq0").field("field0"))
                .execute().actionGet();
        Uniqtermcount count = searchResponse.getAggregations().get("uniq0");
        assertNotNull(count);
        double precision = ((double)count.getValue() / 200000.);
        assertTrue(precision >= 0.83, "The value is " + precision);
    }

    @Test
    public void assertTermCountAggregatorStressStringTestTwoIndexes() {
        client.admin().indices().prepareCreate("test11a").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 8)).execute().actionGet();
        client.admin().indices().prepareCreate("test11b").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 8)).execute().actionGet();
        Set<String> values = new HashSet<String>();
        for (int i = 0; i < 100000; ++i) {
            String val =  UUID.randomUUID().toString();
            values.add(val);
            client.prepareIndex("test11a", "type0", "doc" + i).setSource("field0", UUID.randomUUID().toString()).execute().actionGet();
        }
        client.prepareIndex("test11a", "type0", "docb").setSource("field1", null).setRefresh(true).execute().actionGet();

        for (int i = 100000; i < 200000; ++i) {
            String val =  UUID.randomUUID().toString();
            values.add(val);
            client.prepareIndex("test11b", "type0", "doc" + i).setSource("field0", i).execute().actionGet();
        }
        client.prepareIndex("test11b", "type0", "docb").setSource("field0", null).setRefresh(true).execute().actionGet();

        SearchResponse searchResponse = client.prepareSearch("test11*")
                .setQuery(matchAllQuery())
                .addAggregation(new UniqtermcountBuilder("uniq0").field("field0"))
                .execute().actionGet();
        Uniqtermcount count = searchResponse.getAggregations().get("uniq0");
        assertNotNull(count);
        double precision = ((double)count.getValue() / values.size());
        assertTrue(precision >= 0.98, "The value is " + precision);
    }

    @Test
    public void assertTermCountAggregatorTestEmptyIndexes() {
        client.admin().indices().prepareCreate("test12a").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 8)).execute().actionGet();
        client.admin().indices().prepareCreate("test12b").setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 8)).execute().actionGet();

        for (int i = 0; i < 100000; ++i) {
            client.prepareIndex("test12a", "type0", "doc" + i).setSource("field0", i).execute().actionGet();
        }
        client.prepareIndex("test12a", "type0", "docb").setSource("field0", null).setRefresh(true).execute().actionGet();

        for (int i = 0; i < 100000; ++i) {
            client.prepareIndex("test12b", "type0", "doc" + (i * 10)).setSource("field1", i * 10).execute().actionGet();
        }
        client.prepareIndex("test12b", "type0", "docb").setSource("field1", null).setRefresh(true).execute().actionGet();

        SearchResponse searchResponse = client.prepareSearch("test12*")
                .setQuery(matchAllQuery())
                .addAggregation(new UniqtermcountBuilder("uniq0").field("field0"))
                .execute().actionGet();
        Uniqtermcount count = searchResponse.getAggregations().get("uniq0");
        assertNotNull(count);
        double precision = ((double)count.getValue() / 100000.);
        assertTrue(precision >= 0.96, "The value is " + precision);
    }
}