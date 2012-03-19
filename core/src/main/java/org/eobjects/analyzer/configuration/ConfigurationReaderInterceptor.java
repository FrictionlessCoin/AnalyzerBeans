/**
 * eobjects.org AnalyzerBeans
 * Copyright (C) 2010 eobjects.org
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

/**
 * Defines an interface that allows for interception, decoration and other
 * handling of configuration building.
 * 
 * @author Kasper Sørensen
 */
public interface ConfigurationReaderInterceptor {

    /**
     * Intercepts a filename, allowing for eg. replacing variables or changing
     * relative paths to absolute paths.
     * 
     * @param filename
     * @return
     */
    public String createFilename(String filename);

    /**
     * Gets a temporary storage directory
     * 
     * @return
     */
    public String getTemporaryStorageDirectory();

    /**
     * Loads a class
     * 
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    public Class<?> loadClass(String className) throws ClassNotFoundException;

    /**
     * Gets an optional override of properties in the configuration.
     * 
     * @param variablePath
     *            the path of a variable, eg. "datastoreCatalog.orderdb.url" or
     *            "referenceDataCatalog.my_dictionary.filename".
     * @return a variable override, or null if the variable is not overridden.
     */
    public String getPropertyOverride(String variablePath);

}
