package org.opentoutatice.addon.quota.elasticsearch;

import fr.toutatice.ecm.es.customizer.writers.api.AbstractCustomJsonESWriter;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class CustomizedJsonESDocumentWriter extends AbstractCustomJsonESWriter {

    /** Schema. */
    private static final String SCHEMA = "file";


    @Override
    public boolean accept(DocumentModel documentModel) {
        return documentModel.hasSchema(SCHEMA);
    }

    @Override
    public void writeData(JsonGenerator jsonGenerator, DocumentModel documentModel, String[] strings, Map<String, String> map) throws IOException {
        Property parentProperty = documentModel.getProperty("file:content");

        if ((parentProperty != null) && (parentProperty.getValue() != null)) {
            Serializable value = parentProperty.getValue("length");
            if (value instanceof Long) {
                Long length = (Long) value;
                jsonGenerator.writeNumberField("quota:length", length);
            }
        }
    }

}
