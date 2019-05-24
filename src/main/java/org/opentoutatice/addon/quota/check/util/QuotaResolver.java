/**
 * 
 */
package org.opentoutatice.addon.quota.check.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.PathRef;

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

	// FIXME: use cache
	public DocumentModel getQuotaFor(CoreSession session, DocumentModel blobPointer) {
		// blobPointer doesn't yet exist and can not be found via session
		Path parentPath = blobPointer.getPath().removeLastSegments(1); // FIXME: Robustness / root
		DocumentModel parent = session.getDocument(new PathRef(parentPath.toString()));
		
		Filter quotaFilter = new Filter() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean accept(DocumentModel doc) {
				return doc.hasFacet("Quota") && ((Long) doc.getPropertyValue("qt:maxSize") > 0);
			}
		};
		
		// For debug log
		long beg_ = System.currentTimeMillis();
		
		DocumentModelList quota = ToutaticeDocumentHelper.getParentList(session, parent, quotaFilter, true, true,
				true);
		
		if(log.isDebugEnabled()) {
			long end_ = System.currentTimeMillis();
			log.debug(String.format("Quota serch executed in %d ms.", end_ - beg_));
		}
		
		return quota != null && !quota.isEmpty() ? quota.get(0) : null;
	}

}
