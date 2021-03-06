/**
 * AnalyzerBeans
 * Copyright (C) 2014 Neopost - Customer Information Management
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.eobjects.analyzer.configuration;

import java.io.InputStream;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eobjects.analyzer.connection.CsvDatastore;
import org.eobjects.analyzer.connection.Datastore;
import org.eobjects.analyzer.connection.DatastoreCatalog;
import org.eobjects.analyzer.connection.ExcelDatastore;
import org.eobjects.analyzer.connection.JdbcDatastore;
import org.eobjects.analyzer.util.StringUtils;
import org.apache.metamodel.csv.CsvConfiguration;
import org.apache.metamodel.schema.TableType;
import org.apache.metamodel.util.FileResource;
import org.apache.metamodel.util.Func;
import org.apache.metamodel.util.Resource;
import org.apache.metamodel.xml.XmlDomDataContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Strings;

/**
 * Utility class for externalizing datastores to the XML format of conf.xml.
 * 
 * Generally speaking, XML elements created by this class, and placed in a the
 * <datastore-catalog> element of conf.xml, will be readable by
 * {@link JaxbConfigurationReader}.
 */
public class DatastoreXmlExternalizer {

    private final Document _document;

    public DatastoreXmlExternalizer() {
        final DocumentBuilder documentBuilder;
        try {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        _document = documentBuilder.newDocument();
    }

    public DatastoreXmlExternalizer(Resource resource) {
        _document = resource.read(new Func<InputStream, Document>() {
            @Override
            public Document eval(InputStream in) {
                try {
                    final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                    final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                    return documentBuilder.parse(in);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });

    }

    public DatastoreXmlExternalizer(Document document) {
        _document = document;
    }

    /**
     * Determines if the given datastore is externalizable by this object.
     * 
     * @param datastore
     * @return
     */
    public boolean isExternalizable(final Datastore datastore) {
        if (datastore == null) {
            return false;
        }

        if (datastore instanceof JdbcDatastore) {
            return true;
        }

        if (datastore instanceof CsvDatastore) {
            final Resource resource = ((CsvDatastore) datastore).getResource();
            if (resource instanceof FileResource) {
                return true;
            }
        }

        if (datastore instanceof ExcelDatastore) {
            final Resource resource = ((ExcelDatastore) datastore).getResource();
            if (resource instanceof FileResource) {
                return true;
            }
        }

        return false;
    }

    /**
     * Removes a datastore by it's name, if it exists and is recognizeable by
     * the externalizer.
     * 
     * @param datastoreName
     * @return true if a datastore element was removed from the XML document.
     */
    public boolean removeDatastore(final String datastoreName) {
        final Element datastoreCatalogElement = getDatastoreCatalogElement();
        final NodeList childNodes = datastoreCatalogElement.getChildNodes();
        final int length = childNodes.getLength();
        for (int i = 0; i < length; i++) {
            final Node node = childNodes.item(i);
            if (node instanceof Element) {
                final Element element = (Element) node;
                final Attr[] attributes = XmlDomDataContext.getAttributes(element);
                for (Attr attr : attributes) {
                    if ("name".equals(attr.getName())) {
                        final String value = attr.getValue();
                        if (datastoreName.equals(value)) {
                            // we have a match
                            datastoreCatalogElement.removeChild(element);
                            
                            onDocumentChanged(getDocument());
                            
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Externalizes the given datastore
     * 
     * @param datastore
     * @return
     * @throws UnsupportedOperationException
     */
    public Element externalize(Datastore datastore) throws UnsupportedOperationException {
        if (datastore == null) {
            throw new IllegalArgumentException("Datastore cannot be null");
        }

        final Element elem;

        if (datastore instanceof CsvDatastore) {
            final Resource resource = ((CsvDatastore) datastore).getResource();
            final String filename = toFilename(resource);
            elem = toElement((CsvDatastore) datastore, filename);
        } else if (datastore instanceof ExcelDatastore) {
            final Resource resource = ((ExcelDatastore) datastore).getResource();
            final String filename = toFilename(resource);
            elem = toElement((ExcelDatastore) datastore, filename);
        } else if (datastore instanceof JdbcDatastore) {
            elem = toElement((JdbcDatastore) datastore);
        } else {
            throw new UnsupportedOperationException("Non-supported datastore: " + datastore);
        }

        final Element datastoreCatalogElement = getDatastoreCatalogElement();
        datastoreCatalogElement.appendChild(elem);

        onDocumentChanged(getDocument());

        return elem;
    }

    /**
     * Overrideable method, invoked whenever the document has changed
     * 
     * @param document
     */
    protected void onDocumentChanged(Document document) {
    }

    /**
     * Creates a filename string to externalize, based on a given
     * {@link Resource}.
     * 
     * @param resource
     * @return
     * @throws UnsupportedOperationException
     */
    protected String toFilename(final Resource resource) throws UnsupportedOperationException {
        if (resource instanceof FileResource) {
            return ((FileResource) resource).getFile().getPath();
        }

        throw new UnsupportedOperationException("Unsupported resource type: " + resource);
    }

    /**
     * Externalizes a {@link JdbcDatastore} to a XML element.
     * 
     * @param datastore
     * @param doc
     * @return
     */
    public Element toElement(JdbcDatastore datastore) {
        final Element ds = getDocument().createElement("jdbc-datastore");
        ds.setAttribute("name", datastore.getName());
        if (!StringUtils.isNullOrEmpty(datastore.getDescription())) {
            ds.setAttribute("description", datastore.getDescription());
        }

        String jndiUrl = datastore.getDatasourceJndiUrl();
        if (Strings.isNullOrEmpty(jndiUrl)) {
            appendElement(ds, "url", datastore.getJdbcUrl());
            appendElement(ds, "driver", datastore.getDriverClass());
            appendElement(ds, "username", datastore.getUsername());
            appendElement(ds, "password", datastore.getPassword());
            appendElement(ds, "multiple-connections", datastore.isMultipleConnections() + "");
        } else {
            appendElement(ds, "datasource-jndi-url", jndiUrl);
        }

        final TableType[] tableTypes = datastore.getTableTypes();
        if (tableTypes != null && tableTypes.length != 0 && !Arrays.equals(TableType.DEFAULT_TABLE_TYPES, tableTypes)) {
            final Element tableTypesElement = getDocument().createElement("table-types");
            ds.appendChild(tableTypesElement);

            for (final TableType tableType : tableTypes) {
                appendElement(tableTypesElement, "table-type", tableType.name());
            }
        }

        final String catalogName = datastore.getCatalogName();
        if (!Strings.isNullOrEmpty(catalogName)) {
            appendElement(ds, "catalog-name", catalogName);
        }

        return ds;
    }

    /**
     * Externalizes a {@link ExcelDatastore} to a XML element.
     * 
     * @param datastore
     * @param filename
     *            the filename/path to use in the XML element. Since the
     *            appropriate path will depend on the reading application's
     *            environment (supported {@link Resource} types), this specific
     *            property of the datastore is provided separately.
     * @return
     */
    public Element toElement(ExcelDatastore datastore, String filename) {
        final Element ds = getDocument().createElement("excel-datastore");

        ds.setAttribute("name", datastore.getName());
        if (!StringUtils.isNullOrEmpty(datastore.getDescription())) {
            ds.setAttribute("description", datastore.getDescription());
        }

        appendElement(ds, "filename", filename);

        return ds;
    }

    /**
     * Externalizes a {@link CsvDatastore} to a XML element.
     * 
     * @param datastore
     *            the datastore to externalize
     * @param filename
     *            the filename/path to use in the XML element. Since the
     *            appropriate path will depend on the reading application's
     *            environment (supported {@link Resource} types), this specific
     *            property of the datastore is provided separately.
     * @return a XML element representing the datastore.
     */
    public Element toElement(CsvDatastore datastore, String filename) {
        final Element datastoreElement = getDocument().createElement("csv-datastore");
        datastoreElement.setAttribute("name", datastore.getName());

        final String description = datastore.getDescription();
        if (!StringUtils.isNullOrEmpty(description)) {
            datastoreElement.setAttribute("description", description);
        }

        appendElement(datastoreElement, "filename", filename);
        appendElement(datastoreElement, "quote-char", datastore.getQuoteChar());
        appendElement(datastoreElement, "separator-char", datastore.getSeparatorChar());
        appendElement(datastoreElement, "escape-char", datastore.getEscapeChar());
        appendElement(datastoreElement, "encoding", datastore.getEncoding());
        appendElement(datastoreElement, "fail-on-inconsistencies", datastore.isFailOnInconsistencies());
        appendElement(datastoreElement, "multiline-values", datastore.isMultilineValues());
        appendElement(datastoreElement, "header-line-number", datastore.getHeaderLineNumber());

        return datastoreElement;
    }

    /**
     * Gets the XML document that has been built.
     * 
     * @return
     */
    public final Document getDocument() {
        return _document;
    }

    /**
     * Gets the XML element that represents the {@link DatastoreCatalog}.
     * 
     * @return
     */
    public Element getDatastoreCatalogElement() {
        final Element configurationFileDocumentElement = getDocumentElement();

        final Element datastoreCatalogElement = getOrCreateChildElementByTagName(configurationFileDocumentElement,
                "datastore-catalog");
        if (datastoreCatalogElement == null) {
            throw new IllegalStateException("Could not find <datastore-catalog> element in configuration file");
        }
        return datastoreCatalogElement;
    }

    private Element getDocumentElement() {
        final Document document = getDocument();
        Element documentElement = document.getDocumentElement();
        if (documentElement == null) {
            documentElement = document.createElement("configuration");
            document.appendChild(documentElement);
        }
        return documentElement;
    }

    private Element getOrCreateChildElementByTagName(Element element, String tagName) {
        Element elem = getChildElementByTagName(element, tagName);
        if (elem == null) {
            elem = getDocument().createElement(tagName);
            final Element configurationFileDocumentElement = getDocumentElement();
            configurationFileDocumentElement.appendChild(elem);
        }
        return elem;
    }

    private Element getChildElementByTagName(Element element, String tagName) {
        final NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList == null) {
            return null;
        }
        final int length = nodeList.getLength();
        for (int i = 0; i < length; i++) {
            final Node node = nodeList.item(i);
            if (node instanceof Element) {
                return (Element) node;
            }
        }
        return null;
    }

    private void appendElement(Element parent, String elementName, Object value) {
        if (value == null) {
            return;
        }

        String stringValue = value.toString();

        if (value instanceof Character) {
            final char c = ((Character) value).charValue();
            if (c == CsvConfiguration.NOT_A_CHAR) {
                stringValue = "NOT_A_CHAR";
            } else if (c == '\t') {
                stringValue = "\\t";
            } else if (c == '\n') {
                stringValue = "\\n";
            } else if (c == '\r') {
                stringValue = "\\r";
            }
        }

        final Element element = getDocument().createElement(elementName);
        element.setTextContent(stringValue);
        parent.appendChild(element);
    }
}
