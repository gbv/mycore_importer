package de.vzg.oai_importer.importer;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamResult;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.mycore.libmeta.mods.MODSXMLProcessor;
import org.mycore.libmeta.mods.model.Mods;
import org.mycore.libmeta.mods.model._misc.CodeOrText;
import org.mycore.libmeta.mods.model._misc.DateEncoding;
import org.mycore.libmeta.mods.model._misc.enums.NamePartType;
import org.mycore.libmeta.mods.model._misc.enums.NameType;
import org.mycore.libmeta.mods.model._misc.enums.Yes;
import org.mycore.libmeta.mods.model._toplevel.Abstract;
import org.mycore.libmeta.mods.model._toplevel.AccessCondition;
import org.mycore.libmeta.mods.model._toplevel.Genre;
import org.mycore.libmeta.mods.model._toplevel.Identifier;
import org.mycore.libmeta.mods.model._toplevel.Language;
import org.mycore.libmeta.mods.model._toplevel.Location;
import org.mycore.libmeta.mods.model._toplevel.Name;
import org.mycore.libmeta.mods.model._toplevel.OriginInfo;
import org.mycore.libmeta.mods.model._toplevel.RecordInfo;
import org.mycore.libmeta.mods.model._toplevel.RelatedItem;
import org.mycore.libmeta.mods.model._toplevel.Subject;
import org.mycore.libmeta.mods.model._toplevel.TitleInfo;
import org.mycore.libmeta.mods.model._toplevel.TypeOfResource;
import org.mycore.libmeta.mods.model.language.LanguageTerm;
import org.mycore.libmeta.mods.model.location.Url;
import org.mycore.libmeta.mods.model.location.UrlAccess;
import org.mycore.libmeta.mods.model.name.Affiliation;
import org.mycore.libmeta.mods.model.name.DisplayForm;
import org.mycore.libmeta.mods.model.name.NameIdentifier;
import org.mycore.libmeta.mods.model.name.NamePart;
import org.mycore.libmeta.mods.model.name.Role;
import org.mycore.libmeta.mods.model.name.RoleTerm;
import org.mycore.libmeta.mods.model.origininfo.DateIssued;
import org.mycore.libmeta.mods.model.origininfo.Publisher;
import org.mycore.libmeta.mods.model.recordinfo.RecordContentSource;
import org.mycore.libmeta.mods.model.recordinfo.RecordIdentifier;
import org.mycore.libmeta.mods.model.recordinfo.RecordOrigin;
import org.mycore.libmeta.mods.model.subject.SubjectTopic;
import org.mycore.libmeta.mods.model.titleInfo.Title;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.mapping.MappingService;
import de.vzg.oai_importer.mapping.jpa.Mapping;
import de.vzg.oai_importer.mapping.jpa.MappingGroup;
import de.vzg.oai_importer.mycore.MODSUtil;
import de.vzg.oai_importer.mycore.MyCoReRestAPIService;
import de.vzg.oai_importer.mycore.MyCoReTargetConfiguration;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfo;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfoRepository;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * Converts Zenodo records to MyCoRe objects. The records are expected in the InvenioRDM format, see ZenodoHarvester.
 */
@Service("Zenodo2MyCoReImporter")
@Log4j2
public class Zenodo2MyCoReImporter implements Importer {

    public static final String MODS_NAMESPACE_STRING = "http://www.loc.gov/mods/v3";
    public static final Namespace MODS_NAMESPACE = Namespace.getNamespace("mods", MODS_NAMESPACE_STRING);
    public static final Namespace XLINK_NAMESPACE = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
    public static final String GENRE_MAPPING_PROPERTY = "genre";
    public static final String TYPE_MAPPING_PROPERTY = "type";
    public static final String LICENSE_MAPPING_PROPERTY = "license";
    private static final String ROLE_MAPPING_PROPERTY = "role";

    private static final String INSTITUTE_CLASSIFICATION_PROPERTY = "instituteClass";

    @Autowired
    MyCoReObjectInfoRepository objectInfoRepository;

    @Autowired
    MyCoReRestAPIService restAPIService;

    @Autowired
    MappingService mappingService;

    protected Map<String, String> config;

    /**
     * @return the text of the node, null if the node is missing, null or blank
     */
    protected static String text(JsonNode node) {
        String text = node.asText(null);
        return text == null || text.isBlank() ? null : text;
    }

    private static void handleTitle(JsonNode restRecord, Mods.Builder mods) {
        String title = text(restRecord.at("/metadata/title"));
        if (title != null) {
            TitleInfo titleInfo = getTitleInfo(title);
            mods.addContent(titleInfo);
        }
    }

    protected static TitleInfo getTitleInfo(String title) {
        return TitleInfo.builder().addContent(Title.builder().content(title).build()).build();
    }

    public static String getPlainTextString(String text) {
        final Document document = Jsoup.parse(text);
        return document.text();
    }

    public static String getXHTMLSnippedString(String text) {
        Document document = Jsoup.parse(text);
        changeToXHTML(document);

        document.outputSettings().prettyPrint(false);
        changeToXHTML(document);
        return document.body().html();
    }

    private static void changeToXHTML(Document document) {
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);

        // this is just used to detect the protocol of relative urls
        document.setBaseUri("http://test.de/receive/placeholder");
    }

    /**
     * Adds free keywords and subjects of controlled vocabularies (e.g. EuroSciVoc). The latter have a valueURI, if
     * the vocabulary provides one.
     */
    private static void handleSubject(JsonNode restRecord, Mods.Builder mods) {
        HashSet<String> processedSubjects = new HashSet<>();

        for (JsonNode subject : restRecord.at("/metadata/subjects")) {
            String term = text(subject.path("subject"));
            if (term == null || !processedSubjects.add(term)) {
                continue;
            }
            SubjectTopic content = new SubjectTopic();
            content.setValueURI(getIdentifier(subject, "url"));
            content.setContent(term);
            Subject subjectMods = new Subject();
            subjectMods.getContent().add(content);
            mods.addContent(subjectMods);
        }
    }

    /**
     * @return the first identifier with the scheme in the identifiers of the node, null if there is none
     */
    private static String getIdentifier(JsonNode node, String scheme) {
        for (JsonNode identifier : node.path("identifiers")) {
            if (scheme.equals(identifier.path("scheme").asText())) {
                return text(identifier.path("identifier"));
            }
        }
        return null;
    }

    private static void handleRecordInfo(Mods.Builder mods, String foreignId, String configId) {
        RecordInfo.Builder builder = getRecordInfoBuilder(foreignId, configId);
        mods.addContent(builder.build());
    }

    protected static RecordInfo.Builder getRecordInfoBuilder(String foreignId, String configId) {
        RecordInfo.Builder builder = RecordInfo.builderForRecordInfo();

        builder.addContent(RecordIdentifier.builderForRecordIdentifier().content(foreignId).build());
        builder.addContent(RecordContentSource.builder().content(configId).build());
        builder.addContent(RecordOrigin.builder().content("Record has been transformed from Zenodo to MyCoRe")
            .build());
        return builder;
    }

    protected void handleDOI(JsonNode restRecord, Mods.Builder mods) {
        String doi = text(restRecord.at("/pids/doi/identifier"));
        if (doi != null) {
            mods.addContent(Identifier.builderForIdentifier().content(doi).type("doi").build());
        }
    }

    private static JsonNode parseMetadata(String metadata) {
        JsonNode restRecord;

        try {
            restRecord = new ObjectMapper().readTree(metadata);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if (restRecord.at("/metadata/resource_type/id").isMissingNode()) {
            // the legacy format of Zenodo has resource_type/type instead
            throw new IllegalArgumentException("Der Datensatz " + restRecord.path("id").asText()
                + " liegt nicht im InvenioRDM-Format vor. Bitte den Datensatz löschen und neu harvesten.");
        }
        return restRecord;
    }

    private static String getZenodoType(JsonNode restRecord) {
        return restRecord.at("/metadata/resource_type/id").asText();
    }

    /**
     * @return the ids of the licenses of the record, licenses without id (custom licenses) are ignored
     */
    private static Set<String> getLicenseIds(JsonNode restRecord) {
        Set<String> licenseIds = new LinkedHashSet<>();
        for (JsonNode right : restRecord.at("/metadata/rights")) {
            String id = text(right.path("id"));
            if (id != null) {
                licenseIds.add(id);
            }
        }
        return licenseIds;
    }

    private void handlePublicationInfo(JsonNode restRecord, Mods.Builder mods) {
        String publicationDate = text(restRecord.at("/metadata/publication_date"));
        OriginInfo.Builder originInfo = OriginInfo.builderForOriginInfo();

        if (publicationDate != null) {
            originInfo.addContent(DateIssued.builderForDateIssued().encoding(DateEncoding.W3CDTF).keyDate(Yes.YES)
                .content(publicationDate).build());

        }

        originInfo.addContent(Publisher.builderForPublisher().content("Zenodo").build());
        mods.addContent(originInfo.build());
    }

    private void handleCreators(JsonNode restRecord, Mods.Builder mods) {
        for (JsonNode creator : restRecord.at("/metadata/creators")) {
            addName(mods, creator, "aut");
        }
    }

    private boolean handleContributors(JsonNode restRecord, Mods.Builder mods) {
        for (JsonNode contributor : restRecord.at("/metadata/contributors")) {
            String zenodoRole = text(contributor.at("/role/id"));
            String mappingResult = "ctb";
            if (zenodoRole != null) {
                Optional<String> toOptional = getMappingTarget(ROLE_MAPPING_PROPERTY, zenodoRole);
                if (toOptional.isEmpty()) {
                    log.warn("Could not find role mapping for {} in record {}", zenodoRole, getId(restRecord));
                    return false;
                }
                mappingResult = toOptional.get();
            }

            addName(mods, contributor, mappingResult);
        }
        return true;
    }

    /**
     * Adds a mods:name for a creator or contributor, if it has a name, an affiliation or an identifier.
     * @param marcRelatorRole the role of the person or organization as MARC relator code
     */
    private static void addName(Mods.Builder mods, JsonNode creatorOrContributor, String marcRelatorRole) {
        JsonNode personOrOrg = creatorOrContributor.path("person_or_org");
        boolean personal = !"organizational".equals(personOrOrg.path("type").asText());
        String name = text(personOrOrg.path("name"));
        String orcid = getIdentifier(personOrOrg, "orcid");
        String gnd = getIdentifier(personOrOrg, "gnd");
        List<String> affiliations = new ArrayList<>();
        for (JsonNode affiliation : creatorOrContributor.path("affiliations")) {
            Optional.ofNullable(text(affiliation.path("name"))).ifPresent(affiliations::add);
        }
        if (name == null && affiliations.isEmpty() && orcid == null && gnd == null) {
            return;
        }

        Name.Builder builder = Name.builder();
        builder.type(personal ? NameType.PERSONAL : NameType.CORPORATE);

        if (personal) {
            String familyName = text(personOrOrg.path("family_name"));
            String givenName = text(personOrOrg.path("given_name"));
            if (familyName != null) {
                builder.addContent(NamePart.builder().type(NamePartType.FAMILY).content(familyName).build());
            }
            if (givenName != null) {
                builder.addContent(NamePart.builder().type(NamePartType.GIVEN).content(givenName).build());
            }
        }

        if (name != null) {
            builder.addContent(DisplayForm.builder().content(name).build());
        }

        for (String affiliation : affiliations) {
            builder.addContent(Affiliation.builder().content(affiliation).build());
        }

        if (orcid != null) {
            builder.addContent(NameIdentifier.builder().content(orcid).type("orcid").build());
        }

        if (gnd != null) {
            // Zenodo prefixes the identifier with the scheme, e.g. gnd:123254582
            String gndId = gnd.startsWith("gnd:") ? gnd.substring("gnd:".length()) : gnd;
            builder.addContent(NameIdentifier.builder().content(gndId).type("gnd").build());
        }

        RoleTerm roleTerm =
            RoleTerm.builder().type(CodeOrText.CODE).authority("marcrelator").content(marcRelatorRole).build();
        builder.addContent(Role.builder().addRoleTerm(roleTerm).build());

        mods.addContent(builder.build());
    }

    private static void handleLanguage(JsonNode restRecord, Mods.Builder mods) {
        for (JsonNode language : restRecord.at("/metadata/languages")) {
            String id = text(language.path("id"));
            if (id == null) {
                continue;
            }
            LanguageTerm languageTerm = LanguageTerm.builderForLanguageTerm().type(CodeOrText.CODE)
                .authority(LanguageTerm.AUTHORITY__RFC5646).content(toRFC5646(id)).build();
            mods.addContent(Language.builderForLanguage().addLanguageTerm(languageTerm).build());
        }
    }

    /**
     * Zenodo uses the three-letter codes of ISO 639, RFC 5646 requires the two-letter code if there is one.
     */
    protected static String toRFC5646(String iso639Code) {
        return Arrays.stream(Locale.getISOLanguages())
            .filter(code -> Locale.forLanguageTag(code).getISO3Language().equals(iso639Code))
            .findFirst()
            .orElse(iso639Code);
    }

    private static String getId(JsonNode restRecord) {
        return restRecord.path("id").asText();
    }

    /**
     * @return the target of the mapping in the mapping group of the config property, empty if there is no mapping or
     *         the mapping has no target yet
     */
    private Optional<String> getMappingTarget(String mappingProperty, String from) {
        MappingGroup mappingGroup = mappingService.getGroupByName(config.get(mappingProperty));
        return mappingService.getMappingByGroupAndFrom(mappingGroup, from)
            .map(Mapping::getTo)
            .filter(to -> !to.isBlank());
    }

    /**
     * Adds the mapping to the missing mappings if it has no target. The mapping is created if it does not exist.
     */
    private void checkMapping(List<Mapping> missingMappings, String mappingProperty, String from) {
        MappingGroup mappingGroup = mappingService.getGroupByName(config.get(mappingProperty));
        Optional<Mapping> mappedValue = mappingService.getMappingByGroupAndFrom(mappingGroup, from);
        if (mappedValue.isEmpty()) {
            missingMappings.add(mappingService.addMapping(mappingGroup, from, null));
        } else if (mappedValue.map(Mapping::getTo).filter(to -> !to.isBlank()).isEmpty()) {
            missingMappings.add(mappedValue.get());
        }
    }

    @SneakyThrows
    @Override
    public boolean importRecord(MyCoReTargetConfiguration target, ForeignEntity record) {
        org.jdom2.Document object = convertEntity(target, record);
        if (object == null) {
            return false;
        }
        String location = restAPIService.postObject(target, object);
        String mycoreID = location.substring(location.lastIndexOf("/") + 1);

        updateGroupingState(target, object, mycoreID);
        return true;
    }

    @SneakyThrows
    @Override
    public boolean updateRecord(MyCoReTargetConfiguration target, ForeignEntity record, MyCoReObjectInfo object) {

        org.jdom2.Document objectDoc = convertEntity(target, record);
        if (objectDoc == null) {
            return false;
        }

        String mycoreID = object.getMycoreId();
        Element metadata = MODSUtil.getMetadata(objectDoc).detach();
        restAPIService.putObjectMetadata(target, mycoreID, new org.jdom2.Document(metadata));

        return true;
    }

    private void updateGroupingState(MyCoReTargetConfiguration target, org.jdom2.Document object, String mycoreID)
        throws IOException, URISyntaxException {
        String xp = ".//mods:relatedItem[@xlink:href and @otherType='has_grouping']";
        XPathExpression<Element> parentXPath =
            XPathFactory.instance().compile(xp, Filters.element(), null, MODS_NAMESPACE, XLINK_NAMESPACE);
        List<Element> evaluate = parentXPath.evaluate(object);
        if (!evaluate.isEmpty()
            && evaluate.stream().anyMatch(e -> e.getAttributeValue("href", XLINK_NAMESPACE).endsWith("00000000"))) {
            // this means the new object has the wrong status
            // we need to update it
            // find out the parent id
            org.jdom2.Document objectWithParent = restAPIService.getObject(target, mycoreID);
            Element relatedItem = parentXPath.evaluateFirst(objectWithParent);
            if (relatedItem == null) {
                log.warn("Could not find parent for {}", mycoreID);
                return;
            }
            String parentObjectId = relatedItem.getAttributeValue("href", XLINK_NAMESPACE);
            if (parentObjectId == null) {
                log.warn("Could not find parent for {}", mycoreID);
                return;
            }

            org.jdom2.Document parent = restAPIService.getObject(target, parentObjectId);
            if (MODSUtil.setState(parent, getStatus())) {
                restAPIService.putObject(target, parentObjectId, parent);
            }
        }
    }

    @Override
    public String testRecord(MyCoReTargetConfiguration target, ForeignEntity recordEntity) {
        org.jdom2.Document document = convertEntity(target, recordEntity);
        return new XMLOutputter(Format.getPrettyFormat()).outputString(document);
    }

    @Override
    public List<Mapping> checkMapping(MyCoReTargetConfiguration target, ForeignEntity record) {
        List<Mapping> missingMappings = new ArrayList<>();

        JsonNode restRecord = parseMetadata(record.getMetadata());

        String zenodoType = getZenodoType(restRecord);
        checkMapping(missingMappings, GENRE_MAPPING_PROPERTY, zenodoType);
        checkMapping(missingMappings, TYPE_MAPPING_PROPERTY, zenodoType);
        for (String licenseId : getLicenseIds(restRecord)) {
            checkMapping(missingMappings, LICENSE_MAPPING_PROPERTY, licenseId);
        }
        Set<String> roles = new LinkedHashSet<>();
        for (JsonNode contributor : restRecord.at("/metadata/contributors")) {
            Optional.ofNullable(text(contributor.at("/role/id"))).ifPresent(roles::add);
        }
        for (String role : roles) {
            checkMapping(missingMappings, ROLE_MAPPING_PROPERTY, role);
        }

        return missingMappings;
    }

    private org.jdom2.Document convertEntity(MyCoReTargetConfiguration target, ForeignEntity recordEntity) {
        JsonNode restRecord = parseMetadata(recordEntity.getMetadata());

        Mods.Builder mods = Mods.builder();

        handleTitle(restRecord, mods);
        handleAbstract(restRecord, mods);
        handleSubject(restRecord, mods);
        handleLanguage(restRecord, mods);
        handleCreators(restRecord, mods);
        if (!handleContributors(restRecord, mods)) {
            return null;
        }
        handlePublicationInfo(restRecord, mods);
        handleDOI(restRecord, mods);
        handleRecordInfo(mods, recordEntity.getForeignId(), recordEntity.getConfigId());

        if (!handleGenre(restRecord, mods)) {
            return null;
        }

        if (!handleType(restRecord, mods)) {
            return null;
        }

        if (!handleLicense(restRecord, mods)) {
            return null;
        }

        String instituteClassification = config.get(INSTITUTE_CLASSIFICATION_PROPERTY);
        if (instituteClassification != null) {
            String[] instituteClassificationParts = instituteClassification.split("#", 2);
            if (instituteClassificationParts.length == 2) {
                Name.Builder builder = Name.builder();
                builder.type(NameType.CORPORATE);
                builder.ID("name_" + instituteClassificationParts[1]);
                builder.authorityURI(instituteClassificationParts[0]);
                builder.valueURI(instituteClassification);

                RoleTerm hisRole = RoleTerm.builder().content("his").type(CodeOrText.CODE).authority("marcrelator")
                    .build();
                builder.addContent(Role.builder().addRoleTerm(hisRole).build());

                mods.addContent(builder.build());
            }
        }

        if (Objects.equals(config.get("files"), "modsLocation")) {
            Location.Builder location = Location.builderForLocation();

            String recordUrl = restRecord.at("/links/self").asText();
            for (JsonNode file : restRecord.at("/files/entries")) {
                String name = file.path("key").asText();

                location.addUrl(Url.builderForUrl().content(recordUrl + "/files/" + name + "/content")
                    .access(UrlAccess.RAW_OBJECT)
                    .displayLabel(name).build());
            }

            mods.addContent(location.build());
        }

        handleGrouping(target, recordEntity, restRecord, mods);

        StringWriter xmlStringWriter = new StringWriter();
        StreamResult streamResult = new StreamResult(xmlStringWriter);
        try {
            MODSXMLProcessor.getInstance().marshal(mods.build(), streamResult, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return MODSUtil.wrapInMyCoReFrame(xmlStringWriter.toString(), config.get("base-id"), getStatus());
    }

    /**
     * Adds a related item to the grouping object which bundles all versions of the record.
     */
    protected void handleGrouping(MyCoReTargetConfiguration target, ForeignEntity recordEntity,
        JsonNode restRecord, Mods.Builder mods) {
        String parentId = text(restRecord.at("/parent/id"));
        if (parentId == null) {
            return;
        }
        MyCoReObjectInfo mycoreObject = objectInfoRepository
            .findFirstByRepositoryAndImportURLAndImportID(target.getUrl(), recordEntity.getConfigId(), parentId);
        RelatedItem.Builder relatedItem = RelatedItem.builderForRelatedItem();
        relatedItem.otherType("has_grouping");
        String title = text(restRecord.at("/metadata/title"));
        if (title != null) {
            relatedItem.addContent(getTitleInfo(title));
        }

        Genre intern = Genre.builderForGenre().type("intern")
            .authorityURI("http://www.mycore.org/classifications/mir_genres")
            .valueURI("http://www.mycore.org/classifications/mir_genres#grouping").build();
        relatedItem.addContent(intern);

        RecordInfo recordInfo = getRecordInfoBuilder(parentId, recordEntity.getConfigId()).build();
        relatedItem.addContent(recordInfo);

        String conceptDoi = text(restRecord.at("/parent/pids/doi/identifier"));
        if (conceptDoi != null) {
            relatedItem.addContent(Identifier.builderForIdentifier().content(conceptDoi).type("doi").build());
        }

        if (mycoreObject != null) {
            // import with existing concept pid
            relatedItem.xlinkHref(mycoreObject.getMycoreId());
        } else {
            // refresh object info required after import
            // manually create the object info here
            String baseID = config.get("base-id");
            relatedItem.xlinkHref(baseID + "_00000000");
        }
        mods.addContent(relatedItem.build());
    }

    /**
     * Adds the first license of the record. Custom licenses are ignored, because they can not be mapped.
     */
    private boolean handleLicense(JsonNode restRecord, Mods.Builder mods) {
        Optional<String> licenseId = getLicenseIds(restRecord).stream().findFirst();
        if (licenseId.isPresent()) {
            Optional<String> toOptional = getMappingTarget(LICENSE_MAPPING_PROPERTY, licenseId.get());
            if (toOptional.isPresent()) {
                String license = toOptional.get();
                AccessCondition accessCondition =
                    AccessCondition.builderForAccessCondition().type("use and reproduction")
                        .xlinkHref("http://www.mycore.org/classifications/mir_licenses#" + license)
                        .build();
                accessCondition.setXlinkType("simple");
                mods.addContent(accessCondition);
                return true;
            } else {
                log.info("Could not find license mapping for {} in record {}", licenseId.get(), getId(restRecord));
                return false;
            }
        }
        return true;
    }

    private boolean handleGenre(JsonNode restRecord, Mods.Builder mods) {
        String completeType = getZenodoType(restRecord);
        Optional<String> genreStrOptional = getMappingTarget(GENRE_MAPPING_PROPERTY, completeType);

        if (genreStrOptional.isEmpty()) {
            log.warn("Could not find genre mapping for {} in record {}", completeType, getId(restRecord));
            return false;
        }

        Genre genre = Genre.builderForGenre().type("intern")
            .authorityURI("http://www.mycore.org/classifications/mir_genres")
            .valueURI("http://www.mycore.org/classifications/mir_genres#" + genreStrOptional.get()).build();
        mods.addContent(genre);
        return true;
    }

    private boolean handleType(JsonNode restRecord, Mods.Builder mods) {
        String completeType = getZenodoType(restRecord);
        Optional<String> typeStrOptional = getMappingTarget(TYPE_MAPPING_PROPERTY, completeType);

        if (typeStrOptional.isEmpty()) {
            log.warn("Could not find type mapping for {} in record {}", completeType, getId(restRecord));
            return false;
        }

        TypeOfResource.Builder typeOfResource = TypeOfResource.builderForTypeOfResource();

        typeOfResource.content(typeStrOptional.get());
        mods.addContent(typeOfResource.build());

        return true;
    }

    private String getStatus() {
        return config.get("status");
    }

    @Override
    public void setConfig(Map<String, String> importerConfig) {
        this.config = importerConfig;
    }

    private void handleAbstract(JsonNode restRecord, Mods.Builder mods) {
        String description = text(restRecord.at("/metadata/description"));
        if (description != null) {

            String plainTextString = getPlainTextString(description);
            String xhtmlSnippedString = getXHTMLSnippedString(description);

            String repGroup = UUID.randomUUID().toString().substring(16);

            Abstract.Builder plainBuilder = Abstract.builder();
            plainBuilder.altRepGroup(repGroup);
            plainBuilder.contentType("text/plain");
            plainBuilder.content(plainTextString);

            String url;
            try {
                Abstract.Builder virtualPlainBilder = Abstract.builder();
                virtualPlainBilder.altRepGroup(repGroup);
                virtualPlainBilder.contentType("text/xml");
                virtualPlainBilder.content(xhtmlSnippedString);
                Abstract virtualAbstract = virtualPlainBilder.build();
                QName abstractQName = new QName(MODS_NAMESPACE_STRING, "abstract", "mods");
                JAXBElement<Abstract> jaxbAbstractElement =
                    new JAXBElement<Abstract>(abstractQName, Abstract.class, virtualAbstract);

                // marshall it to string
                Marshaller jaxbMarshaller = JAXBContext.newInstance(Abstract.class).createMarshaller();
                StringWriter virtualAbstractWriter = new StringWriter();
                jaxbMarshaller.marshal(jaxbAbstractElement, new StreamResult(virtualAbstractWriter));
                String virtualAbstractStr = virtualAbstractWriter.toString();

                // the string contains ugly namespaces, remove them with jdom2
                virtualAbstractStr = normalizeXMLString(virtualAbstractStr);

                String sb = "data:text/xml;charset=UTF-8;base64," + Base64.getEncoder().withoutPadding()
                    .encodeToString(virtualAbstractStr.getBytes(StandardCharsets.UTF_8));
                url = sb;
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }

            Abstract.Builder xhtmlBuilder = Abstract.builder();
            xhtmlBuilder.altRepGroup(repGroup);
            xhtmlBuilder.contentType("text/xml");
            xhtmlBuilder.altFormat(url);

            mods.addContent(plainBuilder.build());
            mods.addContent(xhtmlBuilder.build());
        }
    }

    @SneakyThrows
    public String normalizeXMLString(String xml) {
        SAXBuilder saxBuilder = new SAXBuilder();
        org.jdom2.Document parsedDoc = saxBuilder
            .build(new StringReader(xml));

        traverse(parsedDoc.getRootElement());

        return new XMLOutputter(Format.getPrettyFormat()).outputString(parsedDoc.getRootElement());
    }

    public void traverse(org.jdom2.Element element) {
        List<Namespace> additionalNamespaces = element.getAdditionalNamespaces();
        Namespace modsNS = MODS_NAMESPACE;
        Namespace xlinkNS = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

        if (element.getNamespace().getURI().equals(modsNS.getURI())) {
            element.setNamespace(modsNS);
        }
        if (element.getNamespace().getURI().equals(xlinkNS.getURI())) {
            element.setNamespace(xlinkNS);
        }
        for (org.jdom2.Element child : element.getChildren()) {
            traverse(child);
        }
        for (Attribute attribute : element.getAttributes()) {
            if (attribute.getNamespace().getURI().equals(modsNS.getURI())) {
                attribute.setNamespace(modsNS);
            }
            if (attribute.getNamespace().getURI().equals(xlinkNS.getURI())) {
                attribute.setNamespace(xlinkNS);
            }
        }
        for (Namespace namespace : additionalNamespaces) {
            if (namespace.getURI().equals(modsNS.getURI()) && !namespace.getPrefix().equals(modsNS.getPrefix())) {
                element.removeNamespaceDeclaration(namespace);
                element.addNamespaceDeclaration(modsNS);
            }
            if (namespace.getURI().equals(xlinkNS.getURI()) && !namespace.getPrefix().equals(xlinkNS.getPrefix())) {
                element.removeNamespaceDeclaration(namespace);
                element.addNamespaceDeclaration(xlinkNS);
            }
        }
    }

}
