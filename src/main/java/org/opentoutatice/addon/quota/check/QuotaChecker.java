/**
 * 
 */
package org.opentoutatice.addon.quota.check;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.PathRef;
import org.opentoutatice.addon.quota.check.exception.QuotaExceededException;
import org.opentoutatice.addon.quota.check.util.BlobsSizeComputer;
import org.opentoutatice.addon.quota.check.util.QuotaResolver;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;

/**
 * @author dchevrier 
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

	public void checkExceeding(CoreSession session, DocumentModel docToCreate, long fileSize)
			throws QuotaExceededException {
		
		// Get current quota
		long quotaValue = getQuota(session, docToCreate);

		if (quotaValue != -1) { // limited space
		    
	         
            // current doc may not exist and can not be found via session
            Path parentPath = docToCreate.getPath().removeLastSegments(1); 
            DocumentModel firstDoc = session.getDocument(new PathRef(parentPath.toString()));

			// Get root space for computing current quota.
			DocumentModelList parentDocuments = ToutaticeDocumentHelper.getParentList(session, firstDoc,  new SpaceDocumentFilter(), true);
			
			DocumentModel rootSpace = null;
			if( parentDocuments.size() > 0)
					rootSpace = parentDocuments.get(0);

			// Get current tree size
			Long treeSize = getTreeSizeFor(session, new PathRef(rootSpace.getPathAsString()));

			// Check
			if (treeSize +fileSize > quotaValue) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("treeSize + blob.lenght = %d + %d = %d > quota = %d", treeSize,
					        fileSize, treeSize + fileSize, quotaValue));
				}
				throw new QuotaExceededException(DEFAULT_ERROR_EXCEEDED_MESSAGE, ERROR_EXCEEDED_LOCALIZED_MESSAGE, null);
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("treeSize + blob.lenght = %d + %d = %d < quota = %d", treeSize,
				        fileSize, treeSize +fileSize, quotaValue));
			}
		}
	}

	protected long getQuota(CoreSession session, DocumentModel blobPointer) {
		return this.qResolver.getQuotaFor(session, blobPointer, false);
	}

	public Long getTreeSizeFor(CoreSession session, PathRef pathRef) {
		return this.bsComputer.getTreeSizeFrom(session, pathRef);
	}
	
	
	
	private class SpaceDocumentFilter implements Filter {

        private static final long serialVersionUID = 3207718135474475149L;

        @Override
        public boolean accept(DocumentModel document) {
            boolean status = false;

            try {
                status = ((document.hasFacet("Space") || document.hasFacet("SuperSpace"))  && !document.getType().equals("Domain")) ;

            } catch (Exception e) {
                log.error("Failed to filter the quota space document, error: " + e.getMessage());
                status = false;
            }

            return status;
        }
        
    }
}
