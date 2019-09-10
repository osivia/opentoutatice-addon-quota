/**
 * 
 */
package org.opentoutatice.addon.quota.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.opentoutatice.addon.quota.check.QuotaChecker;
import org.opentoutatice.addon.quota.check.exception.QuotaExceededException;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;
import fr.toutatice.ecm.platform.core.listener.ToutaticeBulkDocumentCopiedListener;

/**
 * @author dchevrier <chevrier.david.pro@gmail.com>
 *
 */
public class QuotaListener implements EventListener {

    /** Log. */
    private static final Log log = LogFactory.getLog(QuotaListener.class);

    
    
	@Override
	public void handleEvent(Event event) throws ClientException {
	    
	    if( log.isDebugEnabled())
	        log.debug("QuotaListener.handleEvent "+ event.getName());

		if ((event.getContext() instanceof DocumentEventContext)
				&& ( DocumentEventTypes.ABOUT_TO_CREATE.equals(event.getName())) || (DocumentEventTypes.ABOUT_TO_IMPORT.equals(event.getName())) || (DocumentEventTypes.ABOUT_TO_COPY.equals(event.getName())) ) {
			DocumentEventContext evtCtx = (DocumentEventContext) event.getContext();
			DocumentEventContext s = (DocumentEventContext) event.getContext();

			
			long size = 0;
			
			DocumentModel docToCreate = evtCtx.getSourceDocument();

			// Get file size
            if( DocumentEventTypes.ABOUT_TO_COPY.equals(event.getName()) &&  docToCreate.hasFacet("Folderish"))    {
                // folderish : get source size
                DocumentModel srcFolder = (DocumentModel) event.getContext().getArguments()[0];  
                size = QuotaChecker.get().getTreeSizeFor(evtCtx.getCoreSession(), new PathRef(srcFolder.getPath().toString()));
           }  else {
                // BlobHolder is defined for document having file 
                BlobHolder bHolder = docToCreate.getAdapter(BlobHolder.class);
                if (bHolder != null && bHolder.getBlob() != null) {
                    size = bHolder.getBlob().getLength();
                }   
            }
			

			if (size != 0) {
				// Quota check
				try {
					QuotaChecker.get().checkExceeding(evtCtx.getCoreSession(), docToCreate, size );
				} catch (Exception e) {
				    if( log.isDebugEnabled())
				        log.debug("Quota exceeded ");
					if (e instanceof QuotaExceededException) {
						event.markBubbleException();
						throw e;
					}
				}
			}
		}

	}

}
