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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eobjects.analyzer.configuration.jaxb.AccessDatastoreType;
import org.eobjects.analyzer.configuration.jaxb.BerkeleyDbStorageProviderType;
import org.eobjects.analyzer.configuration.jaxb.ClasspathScannerType;
import org.eobjects.analyzer.configuration.jaxb.ClasspathScannerType.Package;
import org.eobjects.analyzer.configuration.jaxb.CombinedStorageProviderType;
import org.eobjects.analyzer.configuration.jaxb.CompositeDatastoreType;
import org.eobjects.analyzer.configuration.jaxb.Configuration;
import org.eobjects.analyzer.configuration.jaxb.ConfigurationMetadataType;
import org.eobjects.analyzer.configuration.jaxb.CouchdbDatastoreType;
import org.eobjects.analyzer.configuration.jaxb.CsvDatastoreType;
import org.eobjects.analyzer.configuration.jaxb.CustomElementType;
import org.eobjects.analyzer.configuration.jaxb.CustomElementType.Property;
import org.eobjects.analyzer.configuration.jaxb.DatastoreCatalogType;
import org.eobjects.analyzer.configuration.jaxb.DatastoreDictionaryType;
import org.eobjects.analyzer.configuration.jaxb.DatastoreSynonymCatalogType;
import org.eobjects.analyzer.configuration.jaxb.DbaseDatastoreType;
import org.eobjects.analyzer.configuration.jaxb.ExcelDatastoreType;
import org.eobjects.analyzer.configuration.jaxb.FixedWidthDatastoreType;
import org.eobjects.analyzer.configuration.jaxb.H2StorageProviderType;
import org.eobjects.analyzer.configuration.jaxb.HsqldbStorageProviderType;
import org.eobjects.analyzer.configuration.jaxb.InMemoryStorageProviderType;
import org.eobjects.analyzer.configuration.jaxb.JdbcDatastoreType;
import org.eobjects.analyzer.configuration.jaxb.MongodbDatastoreType;
import org.eobjects.analyzer.configuration.jaxb.MultithreadedTaskrunnerType;
import org.eobjects.analyzer.configuration.jaxb.ObjectFactory;
import org.eobjects.analyzer.configuration.jaxb.OpenOfficeDatabaseDatastoreType;
import org.eobjects.analyzer.configuration.jaxb.ReferenceDataCatalogType;
import org.eobjects.analyzer.configuration.jaxb.ReferenceDataCatalogType.Dictionaries;
import org.eobjects.analyzer.configuration.jaxb.ReferenceDataCatalogType.StringPatterns;
import org.eobjects.analyzer.configuration.jaxb.ReferenceDataCatalogType.SynonymCatalogs;
import org.eobjects.analyzer.configuration.jaxb.RegexPatternType;
import org.eobjects.analyzer.configuration.jaxb.SasDatastoreType;
import org.eobjects.analyzer.configuration.jaxb.SimplePatternType;
import org.eobjects.analyzer.configuration.jaxb.SinglethreadedTaskrunnerType;
import org.eobjects.analyzer.configuration.jaxb.StorageProviderType;
import org.eobjects.analyzer.configuration.jaxb.TextFileDictionaryType;
import org.eobjects.analyzer.configuration.jaxb.TextFileSynonymCatalogType;
import org.eobjects.analyzer.configuration.jaxb.ValueListDictionaryType;
import org.eobjects.analyzer.configuration.jaxb.XmlDatastoreType;
import org.eobjects.analyzer.configuration.jaxb.XmlDatastoreType.TableDef;
import org.eobjects.analyzer.connection.AccessDatastore;
import org.eobjects.analyzer.connection.CompositeDatastore;
import org.eobjects.analyzer.connection.CouchDbDatastore;
import org.eobjects.analyzer.connection.CsvDatastore;
import org.eobjects.analyzer.connection.Datastore;
import org.eobjects.analyzer.connection.DatastoreCatalog;
import org.eobjects.analyzer.connection.DatastoreCatalogImpl;
import org.eobjects.analyzer.connection.DbaseDatastore;
import org.eobjects.analyzer.connection.ExcelDatastore;
import org.eobjects.analyzer.connection.FixedWidthDatastore;
import org.eobjects.analyzer.connection.JdbcDatastore;
import org.eobjects.analyzer.connection.MongoDbDatastore;
import org.eobjects.analyzer.connection.OdbDatastore;
import org.eobjects.analyzer.connection.SasDatastore;
import org.eobjects.analyzer.connection.XmlDatastore;
import org.eobjects.analyzer.descriptors.ClasspathScanDescriptorProvider;
import org.eobjects.analyzer.descriptors.ComponentDescriptor;
import org.eobjects.analyzer.descriptors.ConfiguredPropertyDescriptor;
import org.eobjects.analyzer.descriptors.DescriptorProvider;
import org.eobjects.analyzer.descriptors.Descriptors;
import org.eobjects.analyzer.job.concurrent.MultiThreadedTaskRunner;
import org.eobjects.analyzer.job.concurrent.SingleThreadedTaskRunner;
import org.eobjects.analyzer.job.concurrent.TaskRunner;
import org.eobjects.analyzer.lifecycle.LifeCycleHelper;
import org.eobjects.analyzer.reference.DatastoreDictionary;
import org.eobjects.analyzer.reference.DatastoreSynonymCatalog;
import org.eobjects.analyzer.reference.Dictionary;
import org.eobjects.analyzer.reference.ReferenceData;
import org.eobjects.analyzer.reference.ReferenceDataCatalog;
import org.eobjects.analyzer.reference.ReferenceDataCatalogImpl;
import org.eobjects.analyzer.reference.RegexStringPattern;
import org.eobjects.analyzer.reference.SimpleDictionary;
import org.eobjects.analyzer.reference.SimpleStringPattern;
import org.eobjects.analyzer.reference.StringPattern;
import org.eobjects.analyzer.reference.SynonymCatalog;
import org.eobjects.analyzer.reference.TextFileDictionary;
import org.eobjects.analyzer.reference.TextFileSynonymCatalog;
import org.eobjects.analyzer.storage.BerkeleyDbStorageProvider;
import org.eobjects.analyzer.storage.CombinedStorageProvider;
import org.eobjects.analyzer.storage.H2StorageProvider;
import org.eobjects.analyzer.storage.HsqldbStorageProvider;
import org.eobjects.analyzer.storage.InMemoryStorageProvider;
import org.eobjects.analyzer.storage.StorageProvider;
import org.eobjects.analyzer.util.CollectionUtils2;
import org.eobjects.analyzer.util.JaxbValidationEventHandler;
import org.eobjects.analyzer.util.ReflectionUtils;
import org.eobjects.analyzer.util.StringConverter;
import org.eobjects.analyzer.util.StringUtils;
import org.eobjects.metamodel.csv.CsvConfiguration;
import org.eobjects.metamodel.fixedwidth.FixedWidthConfiguration;
import org.eobjects.metamodel.schema.ColumnType;
import org.eobjects.metamodel.util.FileHelper;
import org.eobjects.metamodel.util.SimpleTableDef;
import org.eobjects.metamodel.xml.XmlSaxTableDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration reader that uses the JAXB model to read XML file based
 * configurations for AnalyzerBeans.
 * 
 * @author Kasper Sørensen
 * @author Nancy Sharma
 */
public final class JaxbConfigurationReader implements ConfigurationReader<InputStream> {

    private static final Logger logger = LoggerFactory.getLogger(JaxbConfigurationReader.class);

    private final JAXBContext _jaxbContext;
    private final ConfigurationReaderInterceptor _interceptor;
    private final Deque<String> _variablePathBuilder;

    public JaxbConfigurationReader() {
        this(null);
    }

    public JaxbConfigurationReader(ConfigurationReaderInterceptor configurationReaderCallback) {
        if (configurationReaderCallback == null) {
            configurationReaderCallback = new DefaultConfigurationReaderInterceptor();
        }
        _interceptor = configurationReaderCallback;
        _variablePathBuilder = new ArrayDeque<String>(4);
        try {
            _jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName(),
                    ObjectFactory.class.getClassLoader());
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public AnalyzerBeansConfiguration read(InputStream input) {
        return create(input);
    }

    public AnalyzerBeansConfiguration create(File file) {
        try {
            return create(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public AnalyzerBeansConfiguration create(InputStream inputStream) {
        Configuration configuration = unmarshall(inputStream);
        return create(configuration);
    }

    public Configuration unmarshall(InputStream inputStream) {
        try {
            Unmarshaller unmarshaller = _jaxbContext.createUnmarshaller();

            unmarshaller.setEventHandler(new JaxbValidationEventHandler());
            Configuration configuration = (Configuration) unmarshaller.unmarshal(inputStream);
            return configuration;
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public AnalyzerBeansConfiguration create(Configuration configuration) {
        ConfigurationMetadataType metadata = configuration.getConfigurationMetadata();
        if (metadata != null) {
            logger.info("Configuration name: {}", metadata.getConfigurationName());
            logger.info("Configuration version: {}", metadata.getConfigurationVersion());
            logger.info("Configuration description: {}", metadata.getConfigurationDescription());
            logger.info("Author: {}", metadata.getAuthor());
            logger.info("Created date: {}", metadata.getCreatedDate());
            logger.info("Updated date: {}", metadata.getUpdatedDate());
        }

        AnalyzerBeansConfigurationImpl analyzerBeansConfiguration = new AnalyzerBeansConfigurationImpl();
        
        // injection manager will be used throughout building the configuration.
        // It will be used to host dependencies as they appear
        InjectionManager injectionManager = analyzerBeansConfiguration.getInjectionManager(null);
        
        TaskRunner taskRunner = createTaskRunner(configuration, injectionManager);
        
        analyzerBeansConfiguration = analyzerBeansConfiguration.replace(taskRunner);
        injectionManager = analyzerBeansConfiguration.getInjectionManager(null);

        DescriptorProvider descriptorProvider = createDescriptorProvider(configuration, taskRunner, injectionManager);
        
        analyzerBeansConfiguration =  analyzerBeansConfiguration.replace(descriptorProvider);
        injectionManager = analyzerBeansConfiguration.getInjectionManager(null);

        addVariablePath("datastoreCatalog");
        DatastoreCatalog datastoreCatalog = createDatastoreCatalog(configuration.getDatastoreCatalog(),
                injectionManager);
        removeVariablePath();

        analyzerBeansConfiguration = analyzerBeansConfiguration.replace(datastoreCatalog);
        injectionManager = analyzerBeansConfiguration.getInjectionManager(null);

        addVariablePath("referenceDataCatalog");
        ReferenceDataCatalog referenceDataCatalog = createReferenceDataCatalog(configuration.getReferenceDataCatalog(),
                injectionManager);
        removeVariablePath();

        analyzerBeansConfiguration = analyzerBeansConfiguration.replace(referenceDataCatalog);
        injectionManager = analyzerBeansConfiguration.getInjectionManager(null);

        StorageProvider storageProvider = createStorageProvider(configuration.getStorageProvider(), injectionManager);
        analyzerBeansConfiguration = analyzerBeansConfiguration.replace(storageProvider);

        return analyzerBeansConfiguration;
    }

    private DescriptorProvider createDescriptorProvider(Configuration configuration, TaskRunner taskRunner,
            InjectionManager injectionManager) {
        final DescriptorProvider descriptorProvider;
        final CustomElementType customDescriptorProviderElement = configuration.getCustomDescriptorProvider();
        final ClasspathScannerType classpathScannerElement = configuration.getClasspathScanner();
        if (customDescriptorProviderElement != null) {
            descriptorProvider = createCustomElement(customDescriptorProviderElement, DescriptorProvider.class,
                    injectionManager, true);
        } else {
            final ClasspathScanDescriptorProvider classpathScanner = new ClasspathScanDescriptorProvider(taskRunner);
            if (classpathScannerElement != null) {
                final List<Package> packages = classpathScannerElement.getPackage();
                for (Package pkg : packages) {
                    String packageName = pkg.getValue();
                    if (packageName != null) {
                        packageName = packageName.trim();
                        Boolean recursive = pkg.isRecursive();
                        if (recursive == null) {
                            recursive = true;
                        }
                        classpathScanner.scanPackage(packageName, recursive);
                    }
                }
            }
            descriptorProvider = classpathScanner;
        }

        return descriptorProvider;
    }

    private StorageProvider createStorageProvider(StorageProviderType storageProviderType,
            InjectionManager injectionManager) {
        if (storageProviderType == null) {
            // In-memory is the default storage provider
            return new InMemoryStorageProvider();
        }

        CombinedStorageProviderType combinedStorageProvider = storageProviderType.getCombined();
        if (combinedStorageProvider != null) {
            final StorageProviderType collectionsStorage = combinedStorageProvider.getCollectionsStorage();
            final StorageProviderType rowAnnotationStorage = combinedStorageProvider.getRowAnnotationStorage();

            final StorageProvider collectionsStorageProvider = createStorageProvider(collectionsStorage,
                    injectionManager);
            final StorageProvider rowAnnotationStorageProvider = createStorageProvider(rowAnnotationStorage,
                    injectionManager);

            return new CombinedStorageProvider(collectionsStorageProvider, rowAnnotationStorageProvider);
        }

        InMemoryStorageProviderType inMemoryStorageProvider = storageProviderType.getInMemory();
        if (inMemoryStorageProvider != null) {
            int maxRowsThreshold = inMemoryStorageProvider.getMaxRowsThreshold();
            return new InMemoryStorageProvider(maxRowsThreshold);
        }

        CustomElementType customStorageProvider = storageProviderType.getCustomStorageProvider();
        if (customStorageProvider != null) {
            return createCustomElement(customStorageProvider, StorageProvider.class, injectionManager, true);
        }

        BerkeleyDbStorageProviderType berkeleyDbStorageProvider = storageProviderType.getBerkeleyDb();
        if (berkeleyDbStorageProvider != null) {
            File parentDirectory = new File(_interceptor.getTemporaryStorageDirectory());
            BerkeleyDbStorageProvider storageProvider = new BerkeleyDbStorageProvider(parentDirectory);
            Boolean cleanDirectoryOnStartup = berkeleyDbStorageProvider.isCleanDirectoryOnStartup();
            if (cleanDirectoryOnStartup != null && cleanDirectoryOnStartup.booleanValue()) {
                storageProvider.cleanDirectory();
            }
            return storageProvider;
        }

        HsqldbStorageProviderType hsqldbStorageProvider = storageProviderType.getHsqldb();
        if (hsqldbStorageProvider != null) {
            String directoryPath = hsqldbStorageProvider.getTempDirectory();
            if (directoryPath == null) {
                directoryPath = _interceptor.getTemporaryStorageDirectory();
            }

            directoryPath = _interceptor.createFilename(directoryPath);

            if (directoryPath == null) {
                return new HsqldbStorageProvider();
            } else {
                return new HsqldbStorageProvider(directoryPath);
            }
        }

        H2StorageProviderType h2StorageProvider = storageProviderType.getH2Database();
        if (h2StorageProvider != null) {
            String directoryPath = h2StorageProvider.getTempDirectory();
            if (directoryPath == null) {
                directoryPath = _interceptor.getTemporaryStorageDirectory();
            }

            directoryPath = _interceptor.createFilename(directoryPath);

            if (directoryPath == null) {
                return new H2StorageProvider();
            } else {
                return new H2StorageProvider(directoryPath);
            }
        }

        throw new IllegalStateException("Unknown storage provider type: " + storageProviderType);
    }

    private ReferenceDataCatalog createReferenceDataCatalog(ReferenceDataCatalogType referenceDataCatalog,
            InjectionManager injectionManager) {
        List<Dictionary> dictionaryList = new ArrayList<Dictionary>();
        List<SynonymCatalog> synonymCatalogList = new ArrayList<SynonymCatalog>();

        List<StringPattern> stringPatterns = new ArrayList<StringPattern>();

        if (referenceDataCatalog != null) {

            Dictionaries dictionaries = referenceDataCatalog.getDictionaries();
            if (dictionaries != null) {
                for (Object dictionaryType : dictionaries
                        .getTextFileDictionaryOrValueListDictionaryOrDatastoreDictionary()) {
                    if (dictionaryType instanceof DatastoreDictionaryType) {
                        DatastoreDictionaryType ddt = (DatastoreDictionaryType) dictionaryType;

                        String name = ddt.getName();
                        checkName(name, Dictionary.class, dictionaryList);

                        addVariablePath(name);

                        String dsName = getStringVariable("datastoreName", ddt.getDatastoreName());
                        String columnPath = getStringVariable("columnPath", ddt.getColumnPath());

                        DatastoreDictionary dict = new DatastoreDictionary(name, dsName, columnPath);
                        dict.setDescription(ddt.getDescription());

                        dictionaryList.add(dict);

                        removeVariablePath();

                    } else if (dictionaryType instanceof TextFileDictionaryType) {
                        TextFileDictionaryType tfdt = (TextFileDictionaryType) dictionaryType;

                        String name = tfdt.getName();
                        checkName(name, Dictionary.class, dictionaryList);

                        addVariablePath(name);

                        String filenamePath = getStringVariable("filename", tfdt.getFilename());
                        String filename = _interceptor.createFilename(filenamePath);
                        String encoding = getStringVariable("encoding", tfdt.getEncoding());
                        if (encoding == null) {
                            encoding = FileHelper.UTF_8_ENCODING;
                        }
                        TextFileDictionary dict = new TextFileDictionary(name, filename, encoding);
                        dict.setDescription(tfdt.getDescription());
                        dictionaryList.add(dict);

                        removeVariablePath();
                    } else if (dictionaryType instanceof ValueListDictionaryType) {
                        ValueListDictionaryType vldt = (ValueListDictionaryType) dictionaryType;

                        String name = vldt.getName();
                        checkName(name, Dictionary.class, dictionaryList);

                        List<String> values = vldt.getValue();
                        SimpleDictionary dict = new SimpleDictionary(name, values);
                        dict.setDescription(vldt.getDescription());
                        dictionaryList.add(dict);
                    } else if (dictionaryType instanceof CustomElementType) {
                        Dictionary customDictionary = createCustomElement((CustomElementType) dictionaryType,
                                Dictionary.class, injectionManager, false);
                        checkName(customDictionary.getName(), Dictionary.class, dictionaryList);
                        dictionaryList.add(customDictionary);
                    } else {
                        throw new IllegalStateException("Unsupported dictionary type: " + dictionaryType);
                    }
                }
            }

            SynonymCatalogs synonymCatalogs = referenceDataCatalog.getSynonymCatalogs();
            if (synonymCatalogs != null) {
                for (Object synonymCatalogType : synonymCatalogs
                        .getTextFileSynonymCatalogOrDatastoreSynonymCatalogOrCustomSynonymCatalog()) {
                    if (synonymCatalogType instanceof TextFileSynonymCatalogType) {
                        TextFileSynonymCatalogType tfsct = (TextFileSynonymCatalogType) synonymCatalogType;

                        String name = tfsct.getName();
                        checkName(name, SynonymCatalog.class, synonymCatalogList);

                        addVariablePath(name);

                        String filenamePath = getStringVariable("filename", tfsct.getFilename());
                        String filename = _interceptor.createFilename(filenamePath);
                        String encoding = getStringVariable("encoding", tfsct.getEncoding());
                        if (encoding == null) {
                            encoding = FileHelper.UTF_8_ENCODING;
                        }
                        Boolean caseSensitive = getBooleanVariable("caseSensitive", tfsct.isCaseSensitive());
                        if (caseSensitive == null) {
                            caseSensitive = true;
                        }
                        TextFileSynonymCatalog sc = new TextFileSynonymCatalog(name, filename,
                                caseSensitive.booleanValue(), encoding);
                        sc.setDescription(tfsct.getDescription());
                        synonymCatalogList.add(sc);

                        removeVariablePath();

                    } else if (synonymCatalogType instanceof CustomElementType) {
                        SynonymCatalog customSynonymCatalog = createCustomElement(
                                (CustomElementType) synonymCatalogType, SynonymCatalog.class, injectionManager, false);
                        checkName(customSynonymCatalog.getName(), SynonymCatalog.class, synonymCatalogList);
                        synonymCatalogList.add(customSynonymCatalog);
                    } else if (synonymCatalogType instanceof DatastoreSynonymCatalogType) {
                        DatastoreSynonymCatalogType datastoreSynonymCatalogType = (DatastoreSynonymCatalogType) synonymCatalogType;

                        String name = datastoreSynonymCatalogType.getName();
                        checkName(name, SynonymCatalog.class, synonymCatalogList);

                        addVariablePath(name);

                        String dataStoreName = getStringVariable("datastoreName",
                                datastoreSynonymCatalogType.getDatastoreName());
                        String masterTermColumnPath = getStringVariable("masterTermColumnPath",
                                datastoreSynonymCatalogType.getMasterTermColumnPath());

                        String[] synonymColumnPaths = datastoreSynonymCatalogType.getSynonymColumnPath().toArray(
                                new String[0]);
                        DatastoreSynonymCatalog sc = new DatastoreSynonymCatalog(name, dataStoreName,
                                masterTermColumnPath, synonymColumnPaths);
                        sc.setDescription(datastoreSynonymCatalogType.getDescription());
                        synonymCatalogList.add(sc);

                        removeVariablePath();
                    } else {
                        throw new IllegalStateException("Unsupported synonym catalog type: " + synonymCatalogType);
                    }
                }
            }

            StringPatterns stringPatternTypes = referenceDataCatalog.getStringPatterns();
            if (stringPatternTypes != null) {
                for (Object obj : stringPatternTypes.getRegexPatternOrSimplePattern()) {
                    if (obj instanceof RegexPatternType) {
                        RegexPatternType regexPatternType = (RegexPatternType) obj;

                        String name = regexPatternType.getName();
                        checkName(name, StringPattern.class, stringPatterns);

                        addVariablePath(name);

                        String expression = getStringVariable("expression", regexPatternType.getExpression());
                        boolean matchEntireString = getBooleanVariable("matchEntireString",
                                regexPatternType.isMatchEntireString());
                        RegexStringPattern sp = new RegexStringPattern(name, expression, matchEntireString);
                        sp.setDescription(regexPatternType.getDescription());
                        stringPatterns.add(sp);

                        removeVariablePath();
                    } else if (obj instanceof SimplePatternType) {
                        SimplePatternType simplePatternType = (SimplePatternType) obj;

                        String name = simplePatternType.getName();
                        checkName(name, StringPattern.class, stringPatterns);

                        addVariablePath(name);

                        String expression = getStringVariable("expression", simplePatternType.getExpression());
                        SimpleStringPattern sp = new SimpleStringPattern(name, expression);
                        sp.setDescription(simplePatternType.getDescription());
                        stringPatterns.add(sp);

                        removeVariablePath();
                    } else {
                        throw new IllegalStateException("Unsupported string pattern type: " + obj);
                    }
                }
            }
        }

        return new ReferenceDataCatalogImpl(dictionaryList, synonymCatalogList, stringPatterns);
    }

    private DatastoreCatalog createDatastoreCatalog(DatastoreCatalogType datastoreCatalogType,
            InjectionManager injectionManager) {
        Map<String, Datastore> datastores = new HashMap<String, Datastore>();

        List<Object> datastoreTypes = datastoreCatalogType.getJdbcDatastoreOrAccessDatastoreOrCsvDatastore();

        List<CsvDatastoreType> csvDatastores = CollectionUtils2.filterOnClass(datastoreTypes, CsvDatastoreType.class);
        for (CsvDatastoreType csvDatastoreType : csvDatastores) {
            String name = csvDatastoreType.getName();
            checkName(name, Datastore.class, datastores);

            addVariablePath(name);

            String filename = _interceptor
                    .createFilename(getStringVariable("filename", csvDatastoreType.getFilename()));

            String quoteCharString = getStringVariable("quoteChar", csvDatastoreType.getQuoteChar());
            Character quoteChar = null;

            String separatorCharString = getStringVariable("separatorChar", csvDatastoreType.getSeparatorChar());
            Character separatorChar = null;

            String escapeCharString = getStringVariable("escapeChar", csvDatastoreType.getEscapeChar());
            Character escapeChar = null;

            if (!StringUtils.isNullOrEmpty(separatorCharString)) {
                assert separatorCharString.length() == 1;
                separatorChar = separatorCharString.charAt(0);
            }

            if (!StringUtils.isNullOrEmpty(quoteCharString)) {
                assert quoteCharString.length() == 1;
                quoteChar = quoteCharString.charAt(0);
            }

            if (!StringUtils.isNullOrEmpty(escapeCharString)) {
                assert escapeCharString.length() == 1;
                escapeChar = escapeCharString.charAt(0);
            }

            String encoding = getStringVariable("encoding", csvDatastoreType.getEncoding());
            if (StringUtils.isNullOrEmpty(encoding)) {
                encoding = FileHelper.UTF_8_ENCODING;
            }

            Boolean failOnInconsistencies = getBooleanVariable("failOnInconsistencies",
                    csvDatastoreType.isFailOnInconsistencies());
            if (failOnInconsistencies == null) {
                failOnInconsistencies = true;
            }

            Integer headerLineNumber = getIntegerVariable("headerLineNumber", csvDatastoreType.getHeaderLineNumber());
            if (headerLineNumber == null) {
                headerLineNumber = CsvConfiguration.DEFAULT_COLUMN_NAME_LINE;
            }

            CsvDatastore ds = new CsvDatastore(name, filename, quoteChar, separatorChar, escapeChar, encoding,
                    failOnInconsistencies, headerLineNumber);
            ds.setDescription(csvDatastoreType.getDescription());
            datastores.put(name, ds);

            removeVariablePath();
        }

        List<FixedWidthDatastoreType> fixedWidthDatastores = CollectionUtils2.filterOnClass(datastoreTypes,
                FixedWidthDatastoreType.class);
        for (FixedWidthDatastoreType fixedWidthDatastore : fixedWidthDatastores) {
            String name = fixedWidthDatastore.getName();
            checkName(name, Datastore.class, datastores);

            addVariablePath(name);

            String filename = _interceptor.createFilename(getStringVariable("filename",
                    fixedWidthDatastore.getFilename()));
            String encoding = getStringVariable("encoding", fixedWidthDatastore.getEncoding());
            if (!StringUtils.isNullOrEmpty(encoding)) {
                encoding = FileHelper.UTF_8_ENCODING;
            }

            Boolean failOnInconsistencies = getBooleanVariable("failOnInconsistencies",
                    fixedWidthDatastore.isFailOnInconsistencies());
            if (failOnInconsistencies == null) {
                failOnInconsistencies = true;
            }

            Integer headerLineNumber = getIntegerVariable("headerLineNumber", fixedWidthDatastore.getHeaderLineNumber());
            if (headerLineNumber == null) {
                headerLineNumber = FixedWidthConfiguration.DEFAULT_COLUMN_NAME_LINE;
            }

            final FixedWidthDatastore ds;
            final Integer fixedValueWidth = getIntegerVariable("fixedValueWidth",
                    fixedWidthDatastore.getFixedValueWidth());
            if (fixedValueWidth == null) {
                final List<Integer> valueWidthsBoxed = fixedWidthDatastore.getValueWidth();
                int[] valueWidths = new int[valueWidthsBoxed.size()];
                for (int i = 0; i < valueWidths.length; i++) {
                    valueWidths[i] = valueWidthsBoxed.get(i).intValue();
                }
                ds = new FixedWidthDatastore(name, filename, encoding, valueWidths, failOnInconsistencies,
                        headerLineNumber.intValue());
            } else {
                ds = new FixedWidthDatastore(name, filename, encoding, fixedValueWidth, failOnInconsistencies,
                        headerLineNumber.intValue());
            }
            ds.setDescription(fixedWidthDatastore.getDescription());
            datastores.put(name, ds);

            removeVariablePath();
        }

        List<SasDatastoreType> sasDatastores = CollectionUtils2.filterOnClass(datastoreTypes, SasDatastoreType.class);
        for (SasDatastoreType sasDatastoreType : sasDatastores) {
            final String name = sasDatastoreType.getName();
            checkName(name, Datastore.class, datastores);
            addVariablePath(name);
            String directoryPath = getStringVariable("directory", sasDatastoreType.getDirectory());
            final File directory = new File(directoryPath);
            final SasDatastore ds = new SasDatastore(name, directory);
            ds.setDescription(sasDatastoreType.getDescription());
            datastores.put(name, ds);
            removeVariablePath();
        }

        List<AccessDatastoreType> accessDatastores = CollectionUtils2.filterOnClass(datastoreTypes,
                AccessDatastoreType.class);
        for (AccessDatastoreType accessDatastoreType : accessDatastores) {
            String name = accessDatastoreType.getName();
            checkName(name, Datastore.class, datastores);
            addVariablePath(name);
            String filenamePath = getStringVariable("filename", accessDatastoreType.getFilename());
            String filename = _interceptor.createFilename(filenamePath);
            AccessDatastore ds = new AccessDatastore(name, filename);
            ds.setDescription(accessDatastoreType.getDescription());
            datastores.put(name, ds);
            removeVariablePath();
        }

        List<XmlDatastoreType> xmlDatastores = CollectionUtils2.filterOnClass(datastoreTypes, XmlDatastoreType.class);
        for (XmlDatastoreType xmlDatastoreType : xmlDatastores) {
            String name = xmlDatastoreType.getName();
            checkName(name, Datastore.class, datastores);
            addVariablePath(name);
            String filenamePath = getStringVariable("filename", xmlDatastoreType.getFilename());
            String filename = _interceptor.createFilename(filenamePath);
            List<TableDef> tableDefList = xmlDatastoreType.getTableDef();
            final XmlSaxTableDef[] tableDefs;
            if (tableDefList.isEmpty()) {
                tableDefs = null;
            } else {
                tableDefs = new XmlSaxTableDef[tableDefList.size()];
                for (int i = 0; i < tableDefs.length; i++) {
                    String rowXpath = tableDefList.get(i).getRowXpath();
                    String[] valueXpaths = tableDefList.get(i).getValueXpath().toArray(new String[0]);
                    tableDefs[i] = new XmlSaxTableDef(rowXpath, valueXpaths);
                }
            }

            XmlDatastore ds = new XmlDatastore(name, filename, tableDefs);
            ds.setDescription(xmlDatastoreType.getDescription());
            datastores.put(name, ds);
            removeVariablePath();
        }

        List<ExcelDatastoreType> excelDatastores = CollectionUtils2.filterOnClass(datastoreTypes,
                ExcelDatastoreType.class);
        for (ExcelDatastoreType excelDatastoreType : excelDatastores) {
            String name = excelDatastoreType.getName();
            checkName(name, Datastore.class, datastores);
            addVariablePath(name);
            String filenamePath = getStringVariable("filename", excelDatastoreType.getFilename());
            String filename = _interceptor.createFilename(filenamePath);
            ExcelDatastore ds = new ExcelDatastore(name, filename);
            ds.setDescription(excelDatastoreType.getDescription());
            datastores.put(name, ds);
            removeVariablePath();
        }

        List<JdbcDatastoreType> jdbcDatastores = CollectionUtils2
                .filterOnClass(datastoreTypes, JdbcDatastoreType.class);
        for (JdbcDatastoreType jdbcDatastoreType : jdbcDatastores) {
            String name = jdbcDatastoreType.getName();
            checkName(name, Datastore.class, datastores);

            addVariablePath(name);

            JdbcDatastore ds;

            String datasourceJndiUrl = getStringVariable("jndiUrl", jdbcDatastoreType.getDatasourceJndiUrl());
            if (datasourceJndiUrl == null) {
                String url = getStringVariable("url", jdbcDatastoreType.getUrl());
                String driver = getStringVariable("driver", jdbcDatastoreType.getDriver());
                String username = getStringVariable("username", jdbcDatastoreType.getUsername());
                String password = getStringVariable("password", jdbcDatastoreType.getPassword());
                Boolean multipleConnections = getBooleanVariable("multipleConnections",
                        jdbcDatastoreType.isMultipleConnections());
                if (multipleConnections == null) {
                    multipleConnections = true;
                }
                ds = new JdbcDatastore(name, url, driver, username, password, multipleConnections.booleanValue());
            } else {
                ds = new JdbcDatastore(name, datasourceJndiUrl);
            }

            ds.setDescription(jdbcDatastoreType.getDescription());

            datastores.put(name, ds);

            removeVariablePath();
        }

        List<DbaseDatastoreType> dbaseDatastores = CollectionUtils2.filterOnClass(datastoreTypes,
                DbaseDatastoreType.class);
        for (DbaseDatastoreType dbaseDatastoreType : dbaseDatastores) {
            String name = dbaseDatastoreType.getName();
            checkName(name, Datastore.class, datastores);

            addVariablePath(name);

            String filenamePath = getStringVariable("filename", dbaseDatastoreType.getFilename());
            String filename = _interceptor.createFilename(filenamePath);
            DbaseDatastore ds = new DbaseDatastore(name, filename);

            ds.setDescription(dbaseDatastoreType.getDescription());

            datastores.put(name, ds);

            removeVariablePath();
        }

        List<OpenOfficeDatabaseDatastoreType> odbDatastores = CollectionUtils2.filterOnClass(datastoreTypes,
                OpenOfficeDatabaseDatastoreType.class);
        for (OpenOfficeDatabaseDatastoreType odbDatastoreType : odbDatastores) {
            String name = odbDatastoreType.getName();
            checkName(name, Datastore.class, datastores);

            addVariablePath(name);

            String filenamePath = getStringVariable("filename", odbDatastoreType.getFilename());
            String filename = _interceptor.createFilename(filenamePath);
            OdbDatastore ds = new OdbDatastore(name, filename);
            ds.setDescription(odbDatastoreType.getDescription());
            datastores.put(name, ds);

            removeVariablePath();
        }

        List<CouchdbDatastoreType> couchDbDatastores = CollectionUtils2.filterOnClass(datastoreTypes,
                CouchdbDatastoreType.class);
        for (CouchdbDatastoreType couchdbDatastoreType : couchDbDatastores) {
            String name = couchdbDatastoreType.getName();
            checkName(name, Datastore.class, datastores);

            addVariablePath(name);

            String hostname = getStringVariable("hostname", couchdbDatastoreType.getHostname());
            Integer port = getIntegerVariable("port", couchdbDatastoreType.getPort());
            String username = getStringVariable("username", couchdbDatastoreType.getUsername());
            String password = getStringVariable("password", couchdbDatastoreType.getPassword());
            Boolean sslEnabled = getBooleanVariable("ssl", couchdbDatastoreType.isSsl());

            List<org.eobjects.analyzer.configuration.jaxb.CouchdbDatastoreType.TableDef> tableDefList = couchdbDatastoreType
                    .getTableDef();
            final SimpleTableDef[] tableDefs;
            if (tableDefList.isEmpty()) {
                tableDefs = null;
            } else {
                tableDefs = new SimpleTableDef[tableDefList.size()];
                for (int i = 0; i < tableDefs.length; i++) {
                    org.eobjects.analyzer.configuration.jaxb.CouchdbDatastoreType.TableDef tableDef = tableDefList
                            .get(i);
                    String databaseName = tableDef.getDatabase();
                    List<org.eobjects.analyzer.configuration.jaxb.CouchdbDatastoreType.TableDef.Field> fieldList = tableDef
                            .getField();
                    String[] propertyNames = new String[fieldList.size()];
                    ColumnType[] columnTypes = new ColumnType[fieldList.size()];
                    for (int j = 0; j < columnTypes.length; j++) {
                        String propertyName = fieldList.get(j).getName();
                        String propertyTypeName = fieldList.get(j).getType();
                        final ColumnType propertyType;
                        if (StringUtils.isNullOrEmpty(propertyTypeName)) {
                            propertyType = ColumnType.VARCHAR;
                        } else {
                            propertyType = ColumnType.valueOf(propertyTypeName);
                        }
                        propertyNames[j] = propertyName;
                        columnTypes[j] = propertyType;
                    }

                    tableDefs[i] = new SimpleTableDef(databaseName, propertyNames, columnTypes);
                }
            }

            CouchDbDatastore ds = new CouchDbDatastore(name, hostname, port, username, password, sslEnabled, tableDefs);
            ds.setDescription(couchdbDatastoreType.getDescription());
            datastores.put(name, ds);

            removeVariablePath();
        }

        List<MongodbDatastoreType> mongoDbDatastores = CollectionUtils2.filterOnClass(datastoreTypes,
                MongodbDatastoreType.class);
        for (MongodbDatastoreType mongodbDatastoreType : mongoDbDatastores) {
            String name = mongodbDatastoreType.getName();
            checkName(name, Datastore.class, datastores);

            addVariablePath(name);

            String hostname = getStringVariable("hostname", mongodbDatastoreType.getHostname());
            Integer port = getIntegerVariable("port", mongodbDatastoreType.getPort());
            String databaseName = getStringVariable("databaseName", mongodbDatastoreType.getDatabaseName());
            String username = getStringVariable("username", mongodbDatastoreType.getUsername());
            String password = getStringVariable("password", mongodbDatastoreType.getPassword());

            List<org.eobjects.analyzer.configuration.jaxb.MongodbDatastoreType.TableDef> tableDefList = mongodbDatastoreType
                    .getTableDef();
            final SimpleTableDef[] tableDefs;
            if (tableDefList.isEmpty()) {
                tableDefs = null;
            } else {
                tableDefs = new SimpleTableDef[tableDefList.size()];
                for (int i = 0; i < tableDefs.length; i++) {
                    org.eobjects.analyzer.configuration.jaxb.MongodbDatastoreType.TableDef tableDef = tableDefList
                            .get(i);
                    String collectionName = tableDef.getCollection();
                    List<org.eobjects.analyzer.configuration.jaxb.MongodbDatastoreType.TableDef.Property> propertyList = tableDef
                            .getProperty();
                    String[] propertyNames = new String[propertyList.size()];
                    ColumnType[] columnTypes = new ColumnType[propertyList.size()];
                    for (int j = 0; j < columnTypes.length; j++) {
                        String propertyName = propertyList.get(j).getName();
                        String propertyTypeName = propertyList.get(j).getType();
                        final ColumnType propertyType;
                        if (StringUtils.isNullOrEmpty(propertyTypeName)) {
                            propertyType = ColumnType.VARCHAR;
                        } else {
                            propertyType = ColumnType.valueOf(propertyTypeName);
                        }
                        propertyNames[j] = propertyName;
                        columnTypes[j] = propertyType;
                    }

                    tableDefs[i] = new SimpleTableDef(collectionName, propertyNames, columnTypes);
                }
            }

            MongoDbDatastore ds = new MongoDbDatastore(name, hostname, port, databaseName, username, password,
                    tableDefs);
            ds.setDescription(mongodbDatastoreType.getDescription());
            datastores.put(name, ds);

            removeVariablePath();
        }

        List<CustomElementType> customDatastores = CollectionUtils2.filterOnClass(datastoreTypes,
                CustomElementType.class);
        for (CustomElementType customElementType : customDatastores) {
            Datastore ds = createCustomElement(customElementType, Datastore.class, injectionManager, true);
            String name = ds.getName();
            checkName(name, Datastore.class, datastores);
            datastores.put(name, ds);
        }

        List<CompositeDatastoreType> compositeDatastores = CollectionUtils2.filterOnClass(datastoreTypes,
                CompositeDatastoreType.class);
        for (CompositeDatastoreType compositeDatastoreType : compositeDatastores) {
            String name = compositeDatastoreType.getName();
            checkName(name, Datastore.class, datastores);

            List<String> datastoreNames = compositeDatastoreType.getDatastoreName();
            List<Datastore> childDatastores = new ArrayList<Datastore>(datastoreNames.size());
            for (String datastoreName : datastoreNames) {
                Datastore datastore = datastores.get(datastoreName);
                if (datastore == null) {
                    throw new IllegalStateException("No such datastore: " + datastoreName
                            + " (found in composite datastore: " + name + ")");
                }
                childDatastores.add(datastore);
            }

            CompositeDatastore ds = new CompositeDatastore(name, childDatastores);
            ds.setDescription(compositeDatastoreType.getDescription());
            datastores.put(name, ds);
        }

        DatastoreCatalogImpl result = new DatastoreCatalogImpl(datastores.values());
        return result;
    }

    private void addVariablePath(String name) {
        name = StringUtils.toCamelCase(name);
        _variablePathBuilder.add(name);
    }

    private void removeVariablePath() {
        _variablePathBuilder.pollLast();
    }

    private String getStringVariable(String key, String valueIfNull) {
        final StringBuilder sb = new StringBuilder();
        for (final String keyElement : _variablePathBuilder) {
            if (sb.length() > 0) {
                sb.append('.');
            }
            sb.append(keyElement);
        }
        sb.append('.');
        sb.append(key);

        final String variablePath = sb.toString();
        final String value = _interceptor.getPropertyOverride(variablePath);
        if (value == null) {
            return valueIfNull;
        }
        logger.info("Overriding variable '{}' with value: {}", variablePath, value);
        return value;
    }

    public Integer getIntegerVariable(String key, Integer valueIfNull) {
        String value = getStringVariable(key, null);
        if (value == null) {
            return valueIfNull;
        }
        return Integer.parseInt(value);
    }

    private Boolean getBooleanVariable(String key, Boolean valueIfNull) {
        String value = getStringVariable(key, null);
        if (value == null) {
            return valueIfNull;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Checks if a string is a valid name of a component.
     * 
     * @param name
     *            the name to be validated
     * @param type
     *            the type of component (used for error messages)
     * @param previousEntries
     *            the previous entries of that component type (for uniqueness
     *            check)
     * @throws IllegalStateException
     *             if the name is invalid
     */
    private static void checkName(final String name, Class<?> type, final Map<String, ?> previousEntries)
            throws IllegalStateException {
        if (StringUtils.isNullOrEmpty(name)) {
            throw new IllegalStateException(type.getSimpleName() + " name cannot be null");
        }
        if (previousEntries.containsKey(name)) {
            throw new IllegalStateException(type.getSimpleName() + " name is not unique: " + name);
        }
    }

    /**
     * Checks if a string is a valid name of a component.
     * 
     * @param name
     *            the name to be validated
     * @param type
     *            the type of component (used for error messages)
     * @param previousEntries
     *            the previous entries of that component type (for uniqueness
     *            check)
     * @throws IllegalStateException
     *             if the name is invalid
     */
    private static void checkName(String name, Class<?> type, List<? extends ReferenceData> previousEntries)
            throws IllegalStateException {
        if (StringUtils.isNullOrEmpty(name)) {
            throw new IllegalStateException(type.getSimpleName() + " name cannot be null");
        }
        for (ReferenceData referenceData : previousEntries) {
            if (name.equals(referenceData.getName())) {
                throw new IllegalStateException(type.getSimpleName() + " name is not unique: " + name);
            }
        }
    }

    private TaskRunner createTaskRunner(Configuration configuration, InjectionManager injectionManager) {
        SinglethreadedTaskrunnerType singlethreadedTaskrunner = configuration.getSinglethreadedTaskrunner();
        MultithreadedTaskrunnerType multithreadedTaskrunner = configuration.getMultithreadedTaskrunner();
        CustomElementType customTaskrunner = configuration.getCustomTaskrunner();

        TaskRunner taskRunner;
        if (singlethreadedTaskrunner != null) {
            taskRunner = new SingleThreadedTaskRunner();
        } else if (multithreadedTaskrunner != null) {
            Short maxThreads = multithreadedTaskrunner.getMaxThreads();
            if (maxThreads != null) {
                taskRunner = new MultiThreadedTaskRunner(maxThreads.intValue());
            } else {
                taskRunner = new MultiThreadedTaskRunner();
            }
        } else if (customTaskrunner != null) {
            taskRunner = createCustomElement(customTaskrunner, TaskRunner.class, injectionManager, true);
        } else {
            // default task runner type is multithreaded
            taskRunner = new MultiThreadedTaskRunner();
        }

        return taskRunner;
    }

    /**
     * Creates a custom component based on an element which specified just a
     * class name and an optional set of properties.
     * 
     * @param <E>
     * @param customElementType
     *            the JAXB custom element type
     * @param expectedClazz
     *            an expected class or interface that the component should honor
     * @param datastoreCatalog
     *            the datastore catalog (for lookups/injections)
     * @param referenceDataCatalog
     *            the reference data catalog (for lookups/injections)
     * @param initialize
     *            whether or not to call any initialize methods on the component
     *            (reference data should not be initialized, while eg. custom
     *            task runners support this.
     * @return the custom component
     */
    @SuppressWarnings("unchecked")
    private <E> E createCustomElement(CustomElementType customElementType, Class<E> expectedClazz,
            InjectionManager injectionManager, boolean initialize) {
        Class<?> foundClass;
        String className = customElementType.getClassName();

        assert className != null;
        try {
            foundClass = _interceptor.loadClass(className);
        } catch (Exception e) {
            logger.error("Failed to load class: {}", className);
            throw new IllegalStateException(e);
        }
        if (!ReflectionUtils.is(foundClass, expectedClazz)) {
            throw new IllegalStateException(className + " is not a valid " + expectedClazz);
        }

        E result = (E) ReflectionUtils.newInstance(foundClass);

        ComponentDescriptor<?> descriptor = Descriptors.ofComponent(foundClass);

        StringConverter stringConverter = new StringConverter(injectionManager);

        List<Property> propertyTypes = customElementType.getProperty();
        if (propertyTypes != null) {
            for (Property property : propertyTypes) {
                String propertyName = property.getName();
                String propertyValue = property.getValue();

                ConfiguredPropertyDescriptor configuredProperty = descriptor.getConfiguredProperty(propertyName);
                if (configuredProperty == null) {
                    logger.warn("Missing configured property name: {}", propertyName);
                    if (logger.isInfoEnabled()) {
                        Set<ConfiguredPropertyDescriptor> configuredProperties = descriptor.getConfiguredProperties();
                        for (ConfiguredPropertyDescriptor configuredPropertyDescriptor : configuredProperties) {
                            logger.info("Available configured property name: {}, {}",
                                    configuredPropertyDescriptor.getName(), configuredPropertyDescriptor.getType());
                        }
                    }
                    throw new IllegalStateException("No such property in " + foundClass.getName() + ": " + propertyName);
                }

                Object configuredValue = stringConverter.deserialize(propertyValue, configuredProperty.getType(),
                        configuredProperty.getCustomConverter());

                configuredProperty.setValue(result, configuredValue);
            }
        }

        final LifeCycleHelper lifeCycleHelper = new LifeCycleHelper(injectionManager, null);
        lifeCycleHelper.assignProvidedProperties(descriptor, result);

        if (initialize) {
            lifeCycleHelper.initialize(descriptor, result);
        }

        return result;
    }
}
