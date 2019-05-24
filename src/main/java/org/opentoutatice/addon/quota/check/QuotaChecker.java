/**
 * 
 */
package org.opentoutatice.addon.quota.check;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.opentoutatice.addon.quota.check.exception.QuotaExceededException;
import org.opentoutatice.addon.quota.check.util.BlobsSizeComputer;
import org.opentoutatice.addon.quota.check.util.QuotaResolver;
import org.opentoutatice.addon.quota.check.util.BlobsSizeComputer.QueryLanguage;

/**
 * @author dchevrier <chevrier.david.pro@gmail.com>
 *
 */
public class QuotaChecker {

	private static final Log log = LogFactory.getLog(QuotaChecker.class);

	private static QuotaChecker instance;

	protected QuotaResolver qResolver;

	protected BlobsSizeComputer bsComputer;

	private QuotaChecker() {
		this.qResolver = QuotaResolver.get();
		this.bsComputer = BlobsSizeComputer.get();
	}

	public static synchronized QuotaChecker get() {
		if (instance == null) {
			instance = new QuotaChecker();
		}
		return instance;
	}

	public void checkExceeding(CoreSession session, DocumentModel blobPointer, Blob blob)
			throws QuotaExceededException {
		// Get current quota
		DocumentModel quota = getQuota(session, blobPointer);
		if (quota != null) {

			if (log.isDebugEnabled()) {
				log.debug(String.format("Quota %s found.", quota.getPathAsString()));
			}

			Long quota_ = (Long) quota.getPropertyValue("qt:maxSize");

			// Get current tree size
			Long treeSize = getTreeSizeFor(session, new PathRef(quota.getPathAsString()));

			// Check
			if (treeSize + blob.getLength() > quota_) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("treeSize + blob.lenght = %d + %d = %d > quota = %d", treeSize,
							blob.getLength(), treeSize + blob.getLength(), quota_));
				}
				throw new QuotaExceededException(quota_);
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("treeSize + blob.lenght = %d + %d = %d < quota = %d", treeSize,
						blob.getLength(), treeSize + blob.getLength(), quota_));
			}
		} else {
			if (log.isInfoEnabled()) {
				log.info(String.format("No quota defined for %s: no parent with Quota facet and maxSize > 0.",
						blobPointer.getPathAsString()));
			}
		}
	}

	protected DocumentModel getQuota(CoreSession session, DocumentModel blobPointer) {
		return this.qResolver.getQuotaFor(session, blobPointer);
	}

	protected Long getTreeSizeFor(CoreSession session, PathRef pathRef) {
		return this.bsComputer.getTreeSizeFrom(session, pathRef);
	}
}
