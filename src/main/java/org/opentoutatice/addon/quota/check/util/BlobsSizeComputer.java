/**
 * 
 */
package org.opentoutatice.addon.quota.check.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermFilterBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.NestedBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.query.NxqlQueryConverter;
import org.nuxeo.runtime.api.Framework;

/**
 * @author dchevrier <chevrier.david.pro@gmail.com>
 *
 */
public class BlobsSizeComputer {
	
	private static final Log log = LogFactory.getLog(BlobsSizeComputer.class);
	
	public static enum QueryLanguage {
		NXQL, ES;
	}
	
	// FIXME: exclude versions & trashed?
	protected static final String GET_DESCENDANTS_NXQL = "select ecm:uuid from Document where %s '%s'";
	
	protected ElasticSearchAdmin esAdmin;
	
	protected QueryLanguage queryLanguage = QueryLanguage.NXQL;
	
	private static BlobsSizeComputer instance;
	
	private BlobsSizeComputer() {
		this.esAdmin = (ElasticSearchAdmin) Framework.getService(ElasticSearchAdmin.class);
	}
	
	private BlobsSizeComputer(QueryLanguage qL) {
		this.esAdmin = (ElasticSearchAdmin) Framework.getService(ElasticSearchAdmin.class);
		this.queryLanguage = qL;
	}
	
	public static synchronized BlobsSizeComputer get() {
		if(instance == null) {
			instance = new BlobsSizeComputer();
		}
		return instance;
	}
	
	public static synchronized BlobsSizeComputer get(QueryLanguage qL) {
		if(instance == null) {
			instance = new BlobsSizeComputer(qL);
		}
		return instance;
	}
	
	public Long getTreeSizeFrom(CoreSession session, DocumentRef docRef) {
		// TODO: try with NXQL for query part (nested and sum aggregates are not implemented in Nx)
		
		final SearchRequestBuilder request = this.esAdmin.getClient()
				.prepareSearch(this.esAdmin.getIndexNameForRepository(session.getRepositoryName()))
				.setTypes("doc").setSearchType(SearchType.QUERY_THEN_FETCH);

		// Query
		QueryBuilder queryBuilder = null;
		
		switch (this.queryLanguage) {
		case NXQL:
			String clause = DocumentRef.PATH == docRef.type() ? " ecm:path startswith " : " ecm:ancestorId = ";
			queryBuilder = NxqlQueryConverter.toESQueryBuilder(String.format(GET_DESCENDANTS_NXQL, clause, docRef.toString()), session);
			break;

		case ES:
			String term = DocumentRef.PATH == docRef.type() ? " ecm:path.children " : " ecm:ancestorId ";
			queryBuilder = QueryBuilders.termQuery(term, docRef.toString());
//			TermFilterBuilder filter = FilterBuilders.termFilter(term, docRef.toString());
//			queryBuilder = QueryBuilders.boolQuery().filter(filter);
			break;
		}
		
		request.setQuery(queryBuilder);
		
		// Sum aggregation
		NestedBuilder aggregation = AggregationBuilders.nested("nested_field").path("file:content").subAggregation(AggregationBuilders.sum("tree_size").field("length"));
		request.addAggregation(aggregation);

		if (log.isDebugEnabled()) {
			log.debug(request.toString());
		}

		SearchResponse searchResponse = request.get();
		if (log.isDebugEnabled()) {
			log.debug(searchResponse.toString());
		}

		Sum treeSize = ((Nested) searchResponse.getAggregations().get("nested_field")).getAggregations().get("tree_size");

		return treeSize != null ? (long) treeSize.getValue() : null;
	}

}
