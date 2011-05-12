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
package org.eobjects.analyzer.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.eobjects.analyzer.reference.TextFileDictionary;
import org.eobjects.analyzer.reference.TextFileSynonymCatalog;
import org.eobjects.metamodel.util.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ObjectInputStream} implementation that is aware of changes such as
 * class or package renaming. This can be used to deserialize classes with
 * historic/legacy class names.
 * 
 * @author Kasper Sørensen
 */
public class ChangeAwareObjectInputStream extends ObjectInputStream {

	private static final Logger logger = LoggerFactory.getLogger(ChangeAwareObjectInputStream.class);

	private static final Comparator<String> comparator = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			if (EqualsBuilder.equals(o1, o2)) {
				return 0;
			}
			// use length as the primary differentiator, to make sure long
			// packages are placed before short ones.
			int diff = o1.length() - o2.length();
			if (diff == 0) {
				diff = o1.compareTo(o2);
			}
			return diff;
		}
	};

	private final Map<String, String> renamedPackages;
	private final Map<String, String> renamedClasses;

	public ChangeAwareObjectInputStream(InputStream in) throws IOException {
		super(in);
		renamedPackages = new TreeMap<String, String>(comparator);
		renamedClasses = new HashMap<String, String>();

		// add analyzerbeans' own renamed classes
		addRenamedClass("org.eobjects.analyzer.reference.TextBasedDictionary", TextFileDictionary.class);
		addRenamedClass("org.eobjects.analyzer.reference.TextBasedSynonymCatalog", TextFileSynonymCatalog.class);
	}

	public void addRenamedPackage(String originalPackageName, String newPackageName) {
		renamedPackages.put(originalPackageName, newPackageName);
	}

	public void addRenamedClass(String originalClassName, Class<?> newClass) {
		addRenamedClass(originalClassName, newClass.getName());
	}

	public void addRenamedClass(String originalClassName, String newClassName) {
		renamedClasses.put(originalClassName, newClassName);
	}

	@Override
	protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
		final ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

		final String originalClassName = resultClassDescriptor.getName();
		if (renamedClasses.containsKey(originalClassName)) {
			final String className = renamedClasses.get(originalClassName);
			logger.info("Class '{}' was encountered. Returning class descriptor of new class name: '{}'", originalClassName,
					className);
			return getClassDescriptor(className, resultClassDescriptor);
		} else {
			final Set<Entry<String, String>> entrySet = renamedPackages.entrySet();
			for (Entry<String, String> entry : entrySet) {
				final String legacyPackage = entry.getKey();
				if (originalClassName.startsWith(legacyPackage)) {
					final String className = originalClassName.replaceFirst(legacyPackage, entry.getValue());
					logger.info("Class '{}' was encountered. Returning class descriptor of new class name: '{}'",
							originalClassName, className);
					return getClassDescriptor(className, resultClassDescriptor);
				}
			}
		}

		return resultClassDescriptor;
	}

	private ObjectStreamClass getClassDescriptor(final String className, final ObjectStreamClass originalClassDescriptor)
			throws ClassNotFoundException {
		final ObjectStreamClass newClassDescriptor = ObjectStreamClass.lookup(Class.forName(className));
		final String[] newFieldNames = getFieldNames(newClassDescriptor);
		final String[] originalFieldNames = getFieldNames(originalClassDescriptor);
		if (!EqualsBuilder.equals(originalFieldNames, newFieldNames)) {
			logger.warn("Field names of original and new class ({}) does not correspond!", className);

			// try to hack our way out of it by changing the value of the "name"
			// field in the ORIGINAL descriptor
			try {
				Field field = ObjectStreamClass.class.getDeclaredField("name");
				assert field != null;
				assert field.getType() == String.class;
				field.setAccessible(true);
				field.set(originalClassDescriptor, className);
				return originalClassDescriptor;
			} catch (Exception e) {
				logger.error("Unsuccesful attempt at changing the name of the original class descriptor");
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new IllegalStateException(e);
			}
		}
		return newClassDescriptor;
	}

	private String[] getFieldNames(ObjectStreamClass classDescriptor) {
		ObjectStreamField[] fields = classDescriptor.getFields();
		String[] fieldNames = new String[fields.length];
		for (int i = 0; i < fieldNames.length; i++) {
			fieldNames[i] = fields[i].getName();
		}
		return fieldNames;
	}
}
