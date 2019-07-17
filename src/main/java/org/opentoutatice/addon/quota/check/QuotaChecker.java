/**
 * 
 */
package org.opentoutatice.addon.quota.check;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.opentoutatice.addon.quota.check.exception.QuotaExceededException;
import org.opentoutatice.addon.quota.check.util.BlobsSizeComputer;
import org.opentoutatice.addon.quota.check.util.QuotaResolver;

/**
 * @author dchevrier <chevrier.david.pro@gmail.com>
 *
 */
public class QuotaChecker {

	private static final Log log = LogFactory.getLog(QuotaChecker.class);

	private static QuotaChecker instance;

	protected QuotaResolver qResolver;

	protected BlobsSizeComputer bsComputer;
	
	private static final String DEFAULT_ERROR_EXCEEDED_MESSAGE = "Quota exceeded";
	private static final String ERROR_EXCEEDED_LOCALIZED_MESSAGE = "label.error.quota.exceeded";

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
		long quotaValue = getQuota(session, blobPointer);

		if (quotaValue != -1) { // limited space

			// Get root space for computing current quota.
			List<DocumentModel> parentDocuments = session.getParentDocuments(blobPointer.getParentRef());

			DocumentModel rootSpace = null;
			for (DocumentModel element : parentDocuments) {
				if (element.hasFacet("Space") && !element.getType().equals("Domain")) {

					rootSpace = element;
					break;
				}
			}

			// Get current tree size
			Long treeSize = getTreeSizeFor(session, new PathRef(rootSpace.getPathAsString()));

			// Check
			if (treeSize + blob.getLength() > quotaValue) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("treeSize + blob.lenght = %d + %d = %d > quota = %d", treeSize,
							blob.getLength(), treeSize + blob.getLength(), quotaValue));
				}
				throw new QuotaExceededException(DEFAULT_ERROR_EXCEEDED_MESSAGE, ERROR_EXCEEDED_LOCALIZED_MESSAGE, null);
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("treeSize + blob.lenght = %d + %d = %d < quota = %d", treeSize,
						blob.getLength(), treeSize + blob.getLength(), quotaValue));
			}
		}
	}

	protected long getQuota(CoreSession session, DocumentModel blobPointer) {
		return this.qResolver.getQuotaFor(session, blobPointer, false);
	}

	protected Long getTreeSizeFor(CoreSession session, PathRef pathRef) {
		return this.bsComputer.getTreeSizeFrom(session, pathRef);
	}
}
