package org.eobjects.analyzer.reference;

import junit.framework.TestCase;

public class TextBasedSynonymCatalogTest extends TestCase {

	public void testCountrySynonyms() throws Exception {
		SynonymCatalog cat = new TextBasedSynonymCatalog("foobar", "src/test/resources/synonym-countries.txt");
		assertNull(cat.getMasterTerm("foobar"));
		assertEquals("DNK", cat.getMasterTerm("Denmark"));
		assertEquals("GBR", cat.getMasterTerm("England"));

		assertEquals("GBR", cat.getMasterTerm("GBR"));
		assertEquals("DNK", cat.getMasterTerm("DNK"));
	}
}
