/**
 * 
 */
package org.opentoutatice.addon.quota.check.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;

/**
 * @author dchevrier <chevrier.david.pro@gmail.com>
 *
 */
public class QuotaResolver {

	private static final Log log = LogFactory.getLog(QuotaResolver.class);

	private static QuotaResolver instance;

	private QuotaResolver() {
	};

	public static synchronized QuotaResolver get() {
		if (instance == null) {
			instance = new QuotaResolver();
		}
		return instance;
	}

	public long getQuotaFor(CoreSession session, DocumentModel blobPointer, boolean isFetchable) {

		UnrestrictedQuotaResolver uQuota = new UnrestrictedQuotaResolver(session, blobPointer, isFetchable);
		uQuota.runUnrestricted();
		
		Long quotaValue = -1L;

		DocumentModel uQuotaDoc = uQuota.getQuotaHolder();
		if( uQuotaDoc != null)
			quotaValue = ((Long) uQuotaDoc.getPropertyValue("qt:maxSize")) ;
		
		if( quotaValue != -1)
			quotaValue = quotaValue *  1048576L; // convert from Megabytes to bytes.

		return quotaValue;

	}

	/**
	 * 
	 */
	private static class UnrestrictedQuotaResolver extends UnrestrictedSessionRunner {

		DocumentModel quotaHolder;
		private DocumentModel doc;
		private boolean isFetched = false;

		/**
		 * @param session
		 */
		protected UnrestrictedQuotaResolver(CoreSession session, DocumentModel doc, boolean isFetched) {
			super(session);
			this.doc = doc;
			this.isFetched = isFetched;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.nuxeo.ecm.core.api.UnrestrictedSessionRunner#run()
		 */
		@Override
		public void run() throws ClientException {

			DocumentModel firstDoc = null;
			if (isFetched) {
				firstDoc = doc;
			} else {
				// current doc doesn't yet exist and can not be found via session
				Path parentPath = doc.getPath().removeLastSegments(1); // FIXME: Robustness / root
				firstDoc = session.getDocument(new PathRef(parentPath.toString()));
			}

			Filter quotaFilter = new Filter() {

				private static final long serialVersionUID = 1L;

				@Override
				public boolean accept(DocumentModel doc) {
					return doc.hasFacet("Quota");
				}
			};

			// For debug log
			long beg_ = System.currentTimeMillis();

			DocumentModelList quotaHolders = ToutaticeDocumentHelper.getParentList(session, firstDoc, quotaFilter, true,
					false, true);

			for (DocumentModel q : quotaHolders) {
				if (q.getPropertyValue("qt:maxSize") != null) {
					quotaHolder = q;
					break;
				}
			}

			if (log.isDebugEnabled()) {
				long end_ = System.currentTimeMillis();
				log.debug(String.format("Quota serch executed in %d ms.", end_ - beg_));
			}

		}

		/**
		 * @return the quotaHolder
		 */
		public DocumentModel getQuotaHolder() {
			return quotaHolder;
		}

	}
}
