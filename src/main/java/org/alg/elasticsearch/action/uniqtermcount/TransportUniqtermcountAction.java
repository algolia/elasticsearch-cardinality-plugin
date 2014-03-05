
package org.alg.elasticsearch.action.uniqtermcount;

import static org.elasticsearch.common.collect.Lists.newLinkedList;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.TransportBroadcastOperationAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.routing.GroupShardsIterator;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.shard.service.InternalIndexShard;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;

/**
 * Uniqtermcount index/indices action.
 */
public class TransportUniqtermcountAction
        extends TransportBroadcastOperationAction<UniqtermcountRequest, UniqtermcountResponse, ShardUniqtermcountRequest, ShardUniqtermcountResponse> {

    private final IndicesService indicesService;

    @Inject
    public TransportUniqtermcountAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                   TransportService transportService, IndicesService indicesService) {
        super(settings, threadPool, clusterService, transportService);
        this.indicesService = indicesService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    protected String transportAction() {
        return UniqtermcountAction.NAME;
    }

    @Override
    protected UniqtermcountRequest newRequest() {
        return new UniqtermcountRequest();
    }

    @Override
    protected UniqtermcountResponse newResponse(UniqtermcountRequest request, AtomicReferenceArray shardsResponses, ClusterState clusterState) {
        int successfulShards = 0;
        int failedShards = 0;
        List<ShardOperationFailedException> shardFailures = null;
        HyperLogLogPlus merged = new HyperLogLogPlus(15, 15);
        for (int i = 0; i < shardsResponses.length(); i++) {
            Object shardResponse = shardsResponses.get(i);
            if (shardResponse instanceof BroadcastShardOperationFailedException) {
                failedShards++;
                if (shardFailures == null) {
                    shardFailures = newLinkedList();
                }
                shardFailures.add(new DefaultShardOperationFailedException((BroadcastShardOperationFailedException) shardResponse));
            } else {
                successfulShards++;
                if (shardResponse instanceof ShardUniqtermcountResponse) {
                    ShardUniqtermcountResponse resp = (ShardUniqtermcountResponse) shardResponse;
                    try {
                        merged.addAll(resp.getCounter());
                    } catch (Exception e) {
                        failedShards++;
                        if (shardFailures == null) {
                            shardFailures = newLinkedList();
                        }
                        shardFailures.add(new DefaultShardOperationFailedException(resp.getIndex(), i, e));
                    }
                }
            }
        }
        return new UniqtermcountResponse(shardsResponses.length(), successfulShards, failedShards, shardFailures, merged.cardinality());
    }

    @Override
    protected ShardUniqtermcountRequest newShardRequest() {
        return new ShardUniqtermcountRequest();
    }

    @Override
    protected ShardUniqtermcountRequest newShardRequest(ShardRouting shard, UniqtermcountRequest request) {
        return new ShardUniqtermcountRequest(shard.index(), shard.id(), request);
    }

    @Override
    protected ShardUniqtermcountResponse newShardResponse() {
        return new ShardUniqtermcountResponse();
    }

    /**
     * The uniqtermcount request works against primary shards.
     */
    @Override
    protected GroupShardsIterator shards(ClusterState clusterState, UniqtermcountRequest request, String[] concreteIndices) {
        return clusterState.routingTable().activePrimaryShardsGrouped(concreteIndices, true);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, UniqtermcountRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, UniqtermcountRequest request, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA, concreteIndices);
    }

    @Override
    protected ShardUniqtermcountResponse shardOperation(ShardUniqtermcountRequest request) throws ElasticsearchException {
        InternalIndexShard indexShard = (InternalIndexShard) indicesService.indexServiceSafe(request.index()).shardSafe(request.shardId());
        Engine.Searcher searcher = indexShard.engine().acquireSearcher("termcount");
        HyperLogLogPlus counter = new HyperLogLogPlus(15, 15);
        try {
            IndexReader reader = searcher.reader();
            Fields fields = MultiFields.getFields(reader);
            if (fields != null) {
                for (String field : fields) {
                    // skip internal fields
                    if (field.charAt(0) == '_') {
                        continue;
                    }
                    if (request.getField() == null || field.equals(request.getField())) {

                        Terms terms = fields.terms(field);
                        if (terms != null) {
                            TermsEnum termsEnum = terms.iterator(null);
                            BytesRef text;
                            FieldMapper<?> fieldMapper = indexShard.mapperService().smartNameFieldMapper(field);
                            if (fieldMapper != null && fieldMapper.isNumeric()) {
                                TermsEnum numericsEnum = NumericUtils.filterPrefixCodedLongs(termsEnum);
                                while ((text = numericsEnum.next()) != null) {
                                    counter.offer(NumericUtils.prefixCodedToLong(text));
                                }
                            }
                            else {
                                while ((text = termsEnum.next()) != null) {
                                    counter.offer(text);
                                }
                            }

                        }
                    }
                }
            }
            return new ShardUniqtermcountResponse(request.index(), request.shardId(), counter);
        } catch (IOException ex) {
            throw new ElasticsearchException(ex.getMessage(), ex);
        } finally {
            searcher.release();
        }
    }

}