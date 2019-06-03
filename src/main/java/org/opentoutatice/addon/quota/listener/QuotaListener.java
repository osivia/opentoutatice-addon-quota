/**
 * 
 */
package org.opentoutatice.addon.quota.listener;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.opentoutatice.addon.quota.check.QuotaChecker;
import org.opentoutatice.addon.quota.check.exception.QuotaExceededException;

/**
 * @author dchevrier <chevrier.david.pro@gmail.com>
 *
 */
public class QuotaListener implements EventListener {

	@Override
	public void handleEvent(Event event) throws ClientException {

		if ((event.getContext() instanceof DocumentEventContext)
				&& ( DocumentEventTypes.ABOUT_TO_CREATE.equals(event.getName())) || (DocumentEventTypes.ABOUT_TO_IMPORT.equals(event.getName())) || (DocumentEventTypes.ABOUT_TO_COPY.equals(event.getName()))) {
			DocumentEventContext evtCtx = (DocumentEventContext) event.getContext();

			DocumentModel docToCreate = evtCtx.getSourceDocument();
			// BlobHolder is defined for document having file and note schema by default
			// (could check only file schema but should work with DAM)
			BlobHolder bHolder = docToCreate.getAdapter(BlobHolder.class);
			if (bHolder != null && bHolder.getBlob() != null) {
				// Quota check
				try {
					QuotaChecker.get().checkExceeding(evtCtx.getCoreSession(), docToCreate, bHolder.getBlob());
				} catch (Exception e) {
					if (e instanceof QuotaExceededException) {
						event.markBubbleException();
						throw e;
					}
				}
			}
		}

	}

}
