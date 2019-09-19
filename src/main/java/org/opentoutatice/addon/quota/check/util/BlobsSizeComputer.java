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
import org.elasticsearch.search.aggregations.metrics.sum.SumBuilder;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.query.NxqlQueryConverter;
import org.nuxeo.runtime.api.Framework;

/**
 * @author dchevrier 
 *
 */
public class BlobsSizeComputer {
	
	private static final Log log = LogFactory.getLog(BlobsSizeComputer.class);
	
	public static enum QueryLanguage {
		NXQL, ES;
	}
	

    protected static final String GET_DESCENDANTS_NXQL_AUTO_VERSIONING = "select ecm:uuid from Document where ((%s '%s') AND (ecm:primaryType != 'File' OR ecm:isLatestVersion = 1 ))";
    protected static final String GET_DESCENDANTS_NXQL_DEFAULT = "select ecm:uuid from Document where ((%s '%s') AND ecm:isVersion = 0)";

	
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
		
		final SearchRequestBuilder request = this.esAdmin.getClient()
				.prepareSearch(this.esAdmin.getIndexNameForRepository(session.getRepositoryName()))
				.setTypes("doc").setSearchType(SearchType.QUERY_THEN_FETCH);

		// Query
		QueryBuilder queryBuilder = null;
		
		switch (this.queryLanguage) {
		case NXQL:
			String clause = DocumentRef.PATH == docRef.type() ? " ecm:path startswith " : " ecm:ancestorId = ";
			String NXQLClause;
			if( "auto_versioning".equals("ottc.quota.computingPolicy"))
			    NXQLClause = GET_DESCENDANTS_NXQL_AUTO_VERSIONING;
			else
                NXQLClause = GET_DESCENDANTS_NXQL_DEFAULT;
			
			queryBuilder = NxqlQueryConverter.toESQueryBuilder(String.format(NXQLClause, clause, docRef.toString()), session);
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
		SumBuilder aggregation = AggregationBuilders.sum("tree_size").field("quota:length");
		request.addAggregation(aggregation);

		if (log.isDebugEnabled()) {
			log.debug(request.toString());
		}

		SearchResponse searchResponse = request.get();
		if (log.isDebugEnabled()) {
			log.debug(searchResponse.toString());
		}

		Sum treeSize = searchResponse.getAggregations().get("tree_size");

		return treeSize != null ? (long) treeSize.getValue() : null;
	}

}
