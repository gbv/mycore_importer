package de.vzg.oai_importer.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.mapping.MappingService;
import de.vzg.oai_importer.mapping.jpa.Mapping;
import de.vzg.oai_importer.mapping.jpa.MappingGroup;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfoRepository;

/**
 * Tests the conversion of Zenodo records with the records of the community dhirom, as they are returned by the
 * search of Zenodo in the InvenioRDM format.
 */
class Zenodo2MyCoReImporterTest {

    private static final String LIBRETTI = "17423441";

    private static final String VATICAN_EDITION = "15421307";

    private static final String ONTOLOGY = "17420005";

    private static final String POSTER = "6610846";

    private static final String CONFIG_ID = "zenodo-test";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, ObjectNode> records;

    /**
     * Mappings by config property of the mapping group and source value.
     */
    private Map<String, Map<String, Mapping>> mappings;

    private Map<String, String> config;

    @BeforeEach
    void setUp() throws IOException {
        records = new HashMap<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("zenodo_dhirom_records.json")) {
            for (JsonNode node : objectMapper.readTree(is)) {
                records.put(node.get("id").asText(), (ObjectNode) node);
            }
        }

        config = new HashMap<>(Map.of("base-id", "zenodo_mods", "status", "submitted", "genre", "genre", "type",
            "type", "license", "license", "role", "role"));

        mappings = new HashMap<>();
        for (JsonNode node : records.values()) {
            String type = node.at("/metadata/resource_type/id").asText();
            addMapping("genre", type, "genre_" + type);
            addMapping("type", type, "type_" + type);
            for (JsonNode right : node.at("/metadata/rights")) {
                if (right.has("id")) {
                    addMapping("license", right.get("id").asText(), "license_" + right.get("id").asText());
                }
            }
            for (JsonNode contributor : node.at("/metadata/contributors")) {
                if (contributor.has("role")) {
                    String role = contributor.at("/role/id").asText();
                    addMapping("role", role, "role_" + role);
                }
            }
        }
    }

    private Mapping addMapping(String group, String from, String to) {
        Mapping mapping = new Mapping();
        mapping.setFrom(from);
        mapping.setTo(to);
        mappings.computeIfAbsent(group, g -> new HashMap<>()).put(from, mapping);
        return mapping;
    }

    private <T extends Zenodo2MyCoReImporter> T configure(T importer) {
        importer.mappingService = new MappingService() {
            @Override
            public MappingGroup getGroupByName(String name) {
                MappingGroup group = new MappingGroup();
                group.setName(name);
                return group;
            }

            @Override
            public Optional<Mapping> getMappingByGroupAndFrom(MappingGroup group, String from) {
                return Optional.ofNullable(mappings.getOrDefault(group.getName(), Map.of()).get(from));
            }

            @Override
            public Mapping addMapping(MappingGroup group, String from, String to) {
                return Zenodo2MyCoReImporterTest.this.addMapping(group.getName(), from, to);
            }
        };
        // there are no imported objects
        importer.objectInfoRepository = (MyCoReObjectInfoRepository) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[] { MyCoReObjectInfoRepository.class },
            (proxy, method, args) -> null);
        importer.setConfig(config);
        return importer;
    }

    private Zenodo2MyCoReImporter versionImporter() {
        return configure(new Zenodo2MyCoReImporter());
    }

    private ZenodoConcept2MyCoReImporter conceptImporter() {
        return configure(new ZenodoConcept2MyCoReImporter());
    }

    private ForeignEntity entity(JsonNode zenodoRecord) {
        ForeignEntity entity = new ForeignEntity();
        entity.setForeignId(zenodoRecord.get("id").asText());
        entity.setConfigId(CONFIG_ID);
        entity.setMetadata(zenodoRecord.toString());
        return entity;
    }

    private Document convert(Zenodo2MyCoReImporter importer, JsonNode zenodoRecord)
        throws IOException, JDOMException {
        String xml = importer.testRecord(new MyCoReTargetConfiguration(), entity(zenodoRecord));
        return new SAXBuilder().build(new StringReader(xml));
    }

    private static List<String> values(Document document, String xpath) {
        return XPathFactory.instance()
            .compile(xpath, Filters.fpassthrough(), null, Zenodo2MyCoReImporter.MODS_NAMESPACE,
                Zenodo2MyCoReImporter.XLINK_NAMESPACE)
            .evaluate(document).stream()
            .map(o -> o instanceof org.jdom2.Attribute attribute ? attribute.getValue()
                : ((org.jdom2.Content) o).getValue())
            .toList();
    }

    private static String value(Document document, String xpath) {
        List<String> values = values(document, xpath);
        assertEquals(1, values.size(), "Expected one result for " + xpath + " but got " + values);
        return values.get(0);
    }

    private static String name(String displayForm) {
        return "//mods:mods/mods:name[mods:displayForm='" + displayForm + "']";
    }

    @Test
    void testToRFC5646() {
        assertEquals("it", Zenodo2MyCoReImporter.toRFC5646("ita"));
        assertEquals("de", Zenodo2MyCoReImporter.toRFC5646("deu"));
        assertEquals("en", Zenodo2MyCoReImporter.toRFC5646("eng"));
        // no two-letter code
        assertEquals("nds", Zenodo2MyCoReImporter.toRFC5646("nds"));
    }

    @Test
    void testAllRecordsAreConverted() throws IOException, JDOMException {
        assertEquals(9, records.size());
        for (JsonNode zenodoRecord : records.values()) {
            for (Zenodo2MyCoReImporter importer : List.of(versionImporter(), conceptImporter())) {
                Document document = convert(importer, zenodoRecord);
                String id = zenodoRecord.get("id").asText();
                assertEquals(zenodoRecord.at("/metadata/title").asText(),
                    value(document, "//mods:mods/mods:titleInfo/mods:title"), id);
                assertEquals(id, value(document, "//mods:mods/mods:recordInfo/mods:recordIdentifier"), id);
                assertFalse(values(document, "//mods:mods/mods:name").isEmpty(), id);
                assertTrue(values(document, "//mods:mods/mods:name[not(@type) or not(mods:role/mods:roleTerm)]")
                    .isEmpty(), id);
            }
        }
    }

    @Test
    void testBasicMetadata() throws IOException, JDOMException {
        Document document = convert(conceptImporter(), records.get(LIBRETTI));

        assertEquals("Libretti of the Music History Department of the German Historical Institute in Rome",
            value(document, "//mods:mods/mods:titleInfo/mods:title"));
        assertTrue(value(document, "//mods:mods/mods:abstract[@contentType='text/plain']")
            .startsWith("Metadat-Set of 1,340 records"));
        assertEquals("2025-10-23",
            value(document, "//mods:mods/mods:originInfo/mods:dateIssued[@encoding='w3cdtf'][@keyDate='yes']"));
        assertEquals("Zenodo", value(document, "//mods:mods/mods:originInfo/mods:publisher"));
        assertEquals(CONFIG_ID, value(document, "//mods:mods/mods:recordInfo/mods:recordContentSource"));
        assertEquals("http://www.mycore.org/classifications/mir_genres#genre_dataset",
            value(document, "//mods:mods/mods:genre[@type='intern']/@valueURI"));
        assertEquals("type_dataset", value(document, "//mods:mods/mods:typeOfResource"));
        assertEquals("http://www.mycore.org/classifications/mir_licenses#license_cc-by-4.0",
            value(document, "//mods:mods/mods:accessCondition[@type='use and reproduction']/@xlink:href"));
        assertEquals("submitted", value(document, "//servstate/@categid"));
    }

    @Test
    void testCreators() throws IOException, JDOMException {
        Document document = convert(conceptImporter(), records.get(LIBRETTI));

        String engelhardt = name("Engelhardt, Markus");
        assertEquals("personal", value(document, engelhardt + "/@type"));
        assertEquals("Engelhardt", value(document, engelhardt + "/mods:namePart[@type='family']"));
        assertEquals("Markus", value(document, engelhardt + "/mods:namePart[@type='given']"));
        assertEquals("123254582", value(document, engelhardt + "/mods:nameIdentifier[@type='gnd']"));
        assertEquals("Deutsches Historisches Institut Rom", value(document, engelhardt + "/mods:affiliation"));
        // the role of a creator in Zenodo is ignored, creators are always authors
        assertEquals("aut",
            value(document, engelhardt + "/mods:role/mods:roleTerm[@authority='marcrelator'][@type='code']"));

        String tillinger = name("Tillinger, Christian");
        assertEquals("personal", value(document, tillinger + "/@type"));
        assertTrue(values(document, tillinger + "/mods:nameIdentifier").isEmpty());

        assertEquals(List.of("Engelhardt, Markus", "Tillinger, Christian", "Magniez, Anne-Claire",
            "Grünewälder, Jan-Peter"), values(document, "//mods:mods/mods:name/mods:displayForm"));
    }

    @Test
    void testCreatorWithOrcidAndGnd() throws IOException, JDOMException {
        Document document = convert(conceptImporter(), records.get(VATICAN_EDITION));

        String hinkel = name("Hinkel, Sascha");
        assertEquals("0000-0001-5917-4740", value(document, hinkel + "/mods:nameIdentifier[@type='orcid']"));
        assertEquals("142341770", value(document, hinkel + "/mods:nameIdentifier[@type='gnd']"));
    }

    @Test
    void testCreatorWithoutGivenName() throws IOException, JDOMException {
        Document document = convert(conceptImporter(), records.get(POSTER));

        String hoernschemeyer = name("Jörg Hörnschemeyer");
        assertEquals("Jörg Hörnschemeyer", value(document, hoernschemeyer + "/mods:namePart[@type='family']"));
        assertTrue(values(document, hoernschemeyer + "/mods:namePart[@type='given']").isEmpty());
        assertEquals("DHI Rom", value(document, hoernschemeyer + "/mods:affiliation"));
    }

    @Test
    void testOrganizationalCreator() throws IOException, JDOMException {
        ObjectNode zenodoRecord = records.get(ONTOLOGY);
        ObjectNode organization = objectMapper.createObjectNode();
        organization.putObject("person_or_org").put("type", "organizational").put("name", "DHI Rom");
        ((com.fasterxml.jackson.databind.node.ArrayNode) zenodoRecord.at("/metadata/creators")).add(organization);

        Document document = convert(conceptImporter(), zenodoRecord);

        assertEquals("corporate", value(document, name("DHI Rom") + "/@type"));
        assertTrue(values(document, name("DHI Rom") + "/mods:namePart").isEmpty());
        assertEquals("aut", value(document, name("DHI Rom") + "/mods:role/mods:roleTerm"));
        assertEquals("personal", value(document, name("Sander, Christoph") + "/@type"));
    }

    @Test
    void testContributorRoleIsMapped() throws IOException, JDOMException {
        Document document = convert(conceptImporter(), records.get(LIBRETTI));

        String gruenewaelder = name("Grünewälder, Jan-Peter");
        assertEquals("role_datamanager",
            value(document, gruenewaelder + "/mods:role/mods:roleTerm[@authority='marcrelator'][@type='code']"));
        assertEquals("0009-0006-9112-4042",
            value(document, gruenewaelder + "/mods:nameIdentifier[@type='orcid']"));
    }

    @Test
    void testLanguages() throws IOException, JDOMException {
        String xpath = "//mods:mods/mods:language/mods:languageTerm[@authority='rfc5646'][@type='code']";

        assertEquals(List.of("it"), values(convert(conceptImporter(), records.get(LIBRETTI)), xpath));
        assertEquals(List.of("en", "it", "de", "la", "es", "pl"),
            values(convert(conceptImporter(), records.get(VATICAN_EDITION)), xpath));
        assertTrue(values(convert(conceptImporter(), records.get(ONTOLOGY)), xpath).isEmpty());
    }

    @Test
    void testSubjects() throws IOException, JDOMException {
        Document document = convert(conceptImporter(), records.get(LIBRETTI));

        assertEquals(List.of("Musicology", "Opera"), values(document, "//mods:mods/mods:subject/mods:topic"));
        // subject of the controlled vocabulary EuroSciVoc
        assertEquals("http://data.europa.eu/8mn/euroscivoc/47eb9d78-a29d-4759-8c86-d7d4a8065997",
            value(document, "//mods:mods/mods:subject/mods:topic[text()='Musicology']/@valueURI"));
        // free keyword
        assertTrue(values(document, "//mods:mods/mods:subject/mods:topic[text()='Opera']/@valueURI").isEmpty());
    }

    @Test
    void testConceptImporterUsesConceptDOIWithoutGrouping() throws IOException, JDOMException {
        Document document = convert(conceptImporter(), records.get(LIBRETTI));

        assertEquals("10.5281/zenodo.17423440", value(document, "//mods:mods/mods:identifier[@type='doi']"));
        assertTrue(values(document, "//mods:mods/mods:relatedItem").isEmpty());
    }

    @Test
    void testVersionImporterUsesVersionDOIWithGrouping() throws IOException, JDOMException {
        Document document = convert(versionImporter(), records.get(LIBRETTI));

        assertEquals("10.5281/zenodo.17423441", value(document, "//mods:mods/mods:identifier[@type='doi']"));
        String grouping = "//mods:mods/mods:relatedItem[@otherType='has_grouping']";
        assertEquals("zenodo_mods_00000000", value(document, grouping + "/@xlink:href"));
        assertEquals("17423440", value(document, grouping + "/mods:recordInfo/mods:recordIdentifier"));
        assertEquals("10.5281/zenodo.17423440", value(document, grouping + "/mods:identifier[@type='doi']"));
        assertEquals("http://www.mycore.org/classifications/mir_genres#grouping",
            value(document, grouping + "/mods:genre/@valueURI"));
    }

    @Test
    void testFilesAsLocation() throws IOException, JDOMException {
        String xpath = "//mods:mods/mods:location/mods:url[@access='raw object']";
        assertTrue(values(convert(conceptImporter(), records.get(ONTOLOGY)), xpath).isEmpty());

        config.put("files", "modsLocation");
        Document document = convert(conceptImporter(), records.get(ONTOLOGY));

        assertEquals("https://zenodo.org/api/records/17420005/files/DHI-Roma/grace-ontology-v1.0.3.zip/content",
            value(document, xpath));
        assertEquals("DHI-Roma/grace-ontology-v1.0.3.zip", value(document, xpath + "/@displayLabel"));
    }

    @Test
    void testCustomLicenseIsIgnored() throws IOException, JDOMException {
        Document document = convert(conceptImporter(), records.get(POSTER));

        assertTrue(values(document, "//mods:mods/mods:accessCondition").isEmpty());
    }

    @Test
    void testInstituteClassification() throws IOException, JDOMException {
        config.put("instituteClass", "http://www.mycore.org/classifications/mir_institutes#DHIR");
        Document document = convert(conceptImporter(), records.get(LIBRETTI));

        String institute = "//mods:mods/mods:name[@type='corporate']";
        assertEquals("http://www.mycore.org/classifications/mir_institutes#DHIR",
            value(document, institute + "/@valueURI"));
        assertEquals("his", value(document, institute + "/mods:role/mods:roleTerm"));
    }

    @Test
    void testCheckMappingWithCompleteMappings() {
        for (JsonNode zenodoRecord : records.values()) {
            assertTrue(conceptImporter().checkMapping(new MyCoReTargetConfiguration(), entity(zenodoRecord))
                .isEmpty());
        }
    }

    @Test
    void testCheckMappingReportsAndCreatesMissingMappings() {
        mappings.clear();
        addMapping("license", "cc-by-4.0", " ");

        List<Mapping> missing =
            conceptImporter().checkMapping(new MyCoReTargetConfiguration(), entity(records.get(LIBRETTI)));

        // genre, type, license and the role of the contributor
        assertEquals(List.of("dataset", "dataset", "cc-by-4.0", "datamanager"),
            missing.stream().map(Mapping::getFrom).toList());
        assertTrue(mappings.get("genre").containsKey("dataset"));
        assertTrue(mappings.get("type").containsKey("dataset"));
        assertTrue(mappings.get("role").containsKey("datamanager"));
    }

    @Test
    void testRecordIsNotImportedWithMissingMapping() {
        for (String group : new ArrayList<>(mappings.keySet())) {
            Map<String, Mapping> removed = mappings.remove(group);

            assertFalse(conceptImporter().importRecord(new MyCoReTargetConfiguration(),
                entity(records.get(LIBRETTI))), "Record was imported without mapping of " + group);

            mappings.put(group, removed);
        }
    }

    @Test
    void testLegacyFormatIsRejected() {
        ObjectNode legacyRecord = objectMapper.createObjectNode();
        legacyRecord.put("id", 17423441);
        legacyRecord.putObject("metadata").put("title", "Libretti").putObject("resource_type").put("type",
            "dataset");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> conceptImporter().testRecord(new MyCoReTargetConfiguration(), entity(legacyRecord)));
        assertTrue(exception.getMessage().contains("17423441"));
    }
}
