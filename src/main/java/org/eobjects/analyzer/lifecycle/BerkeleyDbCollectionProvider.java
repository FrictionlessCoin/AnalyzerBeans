package org.eobjects.analyzer.lifecycle;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eobjects.analyzer.descriptors.ProvidedPropertyDescriptorImpl;
import org.eobjects.analyzer.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sleepycat.bind.ByteArrayBinding;
import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.tuple.BooleanBinding;
import com.sleepycat.bind.tuple.ByteBinding;
import com.sleepycat.bind.tuple.CharacterBinding;
import com.sleepycat.bind.tuple.DoubleBinding;
import com.sleepycat.bind.tuple.FloatBinding;
import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.bind.tuple.LongBinding;
import com.sleepycat.bind.tuple.ShortBinding;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.collections.StoredKeySet;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

import dk.eobjects.metamodel.util.FileHelper;

public final class BerkeleyDbCollectionProvider implements CollectionProvider {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private Environment environment;
	private Boolean deleteOnExit;
	private File targetDir;

	public Object createProvidedCollection(ProvidedPropertyDescriptorImpl providedDescriptor) {
		Type typeArgument = providedDescriptor.getTypeArgument(0);
		Class<?> clazz1 = (Class<?>) typeArgument;
		if (providedDescriptor.isList()) {
			return createList(clazz1);
		} else if (providedDescriptor.isSet()) {
			return createSet(clazz1);
		} else if (providedDescriptor.isMap()) {
			Class<?> clazz2 = (Class<?>) providedDescriptor.getTypeArgument(1);
			return createMap(clazz1, clazz2);
		} else {
			// This should never happen (is checked by the
			// ProvidedDescriptor)
			throw new IllegalStateException();
		}
	}

	public void cleanUp(Object obj) {
		if (obj instanceof StoredKeySet) {
			((StoredKeySet) obj).clear();
		} else {
			StoredMap map;
			if (obj instanceof ProvidedList<?>) {
				ProvidedList<?> list = (ProvidedList<?>) obj;
				map = (StoredMap) list.getWrappedMap();
			} else if (obj instanceof StoredMap) {
				map = (StoredMap) obj;
			} else {
				throw new IllegalStateException("Cannot clean up object: " + obj);
			}
			map.clear();
		}
		

		try {
			getEnvironment().compress();
			getEnvironment().cleanLog();
			getEnvironment().sync();
		} catch (DatabaseException e) {
			log.error("Exception occurred while cleaning up object: " + obj, e);
		}

		if (deleteOnExit) {
			initDeleteOnExit(targetDir);
		}
	}

	private void initDeleteOnExit(File dir) {
		dir.deleteOnExit();
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				initDeleteOnExit(file);
			} else if (file.isFile()) {
				file.deleteOnExit();
			} else {
				log.warn("Unable to set the deleteOnExit flag on file: " + file);
			}
		}
	}

	private Environment getEnvironment() throws DatabaseException {
		if (environment == null) {
			EnvironmentConfig config = new EnvironmentConfig();
			config.setAllowCreate(true);
			File targetDir = createTargetDir();
			environment = new Environment(targetDir, config);
		}
		return environment;
	}

	private File createTargetDir() {
		File tempDir = FileHelper.getTempDir();
		deleteOnExit = false;
		while (targetDir == null) {
			try {
				File candidateDir = new File(tempDir.getAbsolutePath() + File.separatorChar + "analyzerBeans_"
						+ UUID.randomUUID().toString());
				if (!candidateDir.exists() && candidateDir.mkdir()) {
					targetDir = candidateDir;
					deleteOnExit = true;
				}
			} catch (Exception e) {
				log.error("Exception thrown while trying to create targetDir inside tempDir", e);
				targetDir = tempDir;
			}
		}
		if (log.isInfoEnabled()) {
			log.info("Using target directory for persistent collections (deleteOnExit=" + deleteOnExit + "): "
					+ targetDir.getAbsolutePath());
		}
		return targetDir;
	}

	private Database createDatabase() throws DatabaseException {
		DatabaseConfig databaseConfig = new DatabaseConfig();
		databaseConfig.setAllowCreate(true);
		String databaseName = UUID.randomUUID().toString();
		Database database = getEnvironment().openDatabase(null, databaseName, databaseConfig);
		return database;
	}

	@Override
	public <E> List<E> createList(Class<E> valueType) throws IllegalStateException {
		Map<Integer, E> map = createMap(Integer.class, valueType);

		// Berkeley StoredLists are non-functional!
		// return new StoredList<E>(createDatabase(), valueBinding, true);

		return new ProvidedList<E>(map);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> Set<E> createSet(Class<E> valueType) throws IllegalStateException {
		try {
			return new StoredKeySet(createDatabase(), createBinding(valueType), true);
		} catch (DatabaseException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <K, V> Map<K, V> createMap(Class<K> keyType, Class<V> valueType) throws IllegalStateException {
		try {
			EntryBinding keyBinding = createBinding(keyType);
			EntryBinding valueBinding = createBinding(valueType);
			return new StoredMap(createDatabase(), keyBinding, valueBinding, true);
		} catch (DatabaseException e) {
			throw new IllegalStateException(e);
		}
	}

	private EntryBinding createBinding(Type type) throws UnsupportedOperationException {
		if (ReflectionUtils.isString(type)) {
			return new StringBinding();
		}
		if (ReflectionUtils.isInteger(type)) {
			return new IntegerBinding();
		}
		if (ReflectionUtils.isLong(type)) {
			return new LongBinding();
		}
		if (ReflectionUtils.isBoolean(type)) {
			return new BooleanBinding();
		}
		if (ReflectionUtils.isShort(type)) {
			return new ShortBinding();
		}
		if (ReflectionUtils.isByte(type)) {
			return new ByteBinding();
		}
		if (ReflectionUtils.isDouble(type)) {
			return new DoubleBinding();
		}
		if (ReflectionUtils.isFloat(type)) {
			return new FloatBinding();
		}
		if (ReflectionUtils.isCharacter(type)) {
			return new CharacterBinding();
		}
		if (ReflectionUtils.isByteArray(type)) {
			return new ByteArrayBinding();
		}
		throw new UnsupportedOperationException("Cannot provide collection of type " + type);
	}
}
