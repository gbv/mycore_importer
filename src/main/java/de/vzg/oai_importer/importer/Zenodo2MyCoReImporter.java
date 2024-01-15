package de.vzg.oai_importer.importer;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.util.Strings;
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
import org.mycore.libmeta.mods.model._misc.enums.Yes;
import org.mycore.libmeta.mods.model._toplevel.Abstract;
import org.mycore.libmeta.mods.model._toplevel.AccessCondition;
import org.mycore.libmeta.mods.model._toplevel.Genre;
import org.mycore.libmeta.mods.model._toplevel.Identifier;
import org.mycore.libmeta.mods.model._toplevel.Location;
import org.mycore.libmeta.mods.model._toplevel.Name;
import org.mycore.libmeta.mods.model._toplevel.OriginInfo;
import org.mycore.libmeta.mods.model._toplevel.RecordInfo;
import org.mycore.libmeta.mods.model._toplevel.RelatedItem;
import org.mycore.libmeta.mods.model._toplevel.Subject;
import org.mycore.libmeta.mods.model._toplevel.TitleInfo;
import org.mycore.libmeta.mods.model.location.Url;
import org.mycore.libmeta.mods.model.location.UrlAccess;
import org.mycore.libmeta.mods.model.name.Affiliation;
import org.mycore.libmeta.mods.model.name.DisplayForm;
import org.mycore.libmeta.mods.model.name.NameIdentifier;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.vzg.oai_importer.foreign.jpa.ForeignEntity;
import de.vzg.oai_importer.foreign.zenodo.ZenodoRestFileMetadata;
import de.vzg.oai_importer.foreign.zenodo.ZenodoRestLicense;
import de.vzg.oai_importer.foreign.zenodo.ZenodoRestMetadata;
import de.vzg.oai_importer.foreign.zenodo.ZenodoRestPerson;
import de.vzg.oai_importer.foreign.zenodo.ZenodoRestRecord;
import de.vzg.oai_importer.foreign.zenodo.ZenodoRestRelations;
import de.vzg.oai_importer.foreign.zenodo.ZenodoRestResourceType;
import de.vzg.oai_importer.foreign.zenodo.ZenodoRestSubjects;
import de.vzg.oai_importer.foreign.zenodo.ZenodoRestVersion;
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

@Service("Zenodo2MyCoReImporter")
@Log4j2
public class Zenodo2MyCoReImporter implements Importer {

    public static final String MODS_NAMESPACE_STRING = "http://www.loc.gov/mods/v3";
    public static final Namespace MODS_NAMESPACE = Namespace.getNamespace("mods", MODS_NAMESPACE_STRING);
    public static final Namespace XLINK_NAMESPACE = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
    public static final String GENRE_MAPPING_PROPERTY = "genre";
    public static final String LICENSE_MAPPING_PROPERTY = "license";
    private static final String ROLE_MAPPING_PROPERTY = "role";

    @Autowired
    MyCoReObjectInfoRepository objectInfoRepository;

    @Autowired
    MyCoReRestAPIService restAPIService;

    @Autowired
    MappingService mappingService;

    private Map<String, String> config;

    private static void handleTitle(ZenodoRestRecord restRecord, Mods.Builder mods) {
        String title = restRecord.getTitle();
        if (title != null) {
            TitleInfo titleInfo = getTitleInfo(title);
            mods.addContent(titleInfo);
        }
    }

    private static TitleInfo getTitleInfo(String title) {
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

    private static void handleSubject(ZenodoRestRecord restRecord, Mods.Builder mods) {
        ZenodoRestMetadata zenodoRestMetadata = restRecord.getMetadata();
        List<ZenodoRestSubjects> subjects = zenodoRestMetadata.getSubjects();
        HashSet<String> processedSubjects = new HashSet<>();

        if (subjects != null) {
            for (ZenodoRestSubjects subject : subjects) {
                String scheme = subject.getScheme();
                String identifier = subject.getIdentifier();
                String term = subject.getTerm();
                if (scheme.equals("url")) {
                    SubjectTopic content = new SubjectTopic();
                    content.setValueURI(identifier);
                    content.setContent(term);
                    processedSubjects.add(term);
                    Subject subjectMods = new Subject();
                    subjectMods.getContent().add(content);
                    mods.addContent(subjectMods);
                }
            }
        }
        List<String> keywords = zenodoRestMetadata.getKeywords();

        if (keywords != null) {
            for (String keyword : keywords) {
                if (!processedSubjects.contains(keyword)) {
                    SubjectTopic content = new SubjectTopic();
                    content.setContent(keyword);
                    Subject subjectMods = new Subject();
                    subjectMods.getContent().add(content);
                    mods.addContent(subjectMods);
                }
            }
        }

    }

    private static void handleRecordInfo(Mods.Builder mods, String foreignId, String configId) {
        RecordInfo.Builder builder = getRecordInfoBuilder(foreignId, configId);
        mods.addContent(builder.build());
    }

    private static RecordInfo.Builder getRecordInfoBuilder(String foreignId, String configId) {
        RecordInfo.Builder builder = RecordInfo.builderForRecordInfo();

        builder.addContent(RecordIdentifier.builderForRecordIdentifier().content(foreignId).build());
        builder.addContent(RecordContentSource.builder().content(configId).build());
        builder.addContent(RecordOrigin.builder().content("Record has been transformed from Zenodo to MyCoRe")
            .build());
        return builder;
    }

    private static void handleDOI(ZenodoRestRecord restRecord, Mods.Builder mods) {
        String doi = restRecord.getMetadata().getDoi();
        if (doi != null) {
            mods.addContent(Identifier.builderForIdentifier().content(doi).type("doi").build());
        }
    }

    private static ZenodoRestRecord parseMetadata(String metadata) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        ZenodoRestRecord restRecord;

        try {
            restRecord = objectMapper.readValue(metadata, ZenodoRestRecord.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return restRecord;
    }

    private static String getZenodoType(ZenodoRestResourceType resourceType) {
        String type = Optional.ofNullable(resourceType.getType()).orElse(Strings.EMPTY);
        String subtype = Optional.ofNullable(resourceType.getSubtype()).orElse(Strings.EMPTY);
        return Stream.of(type, subtype).filter(s -> !s.isBlank()).collect(Collectors.joining("_"));
    }

    private void handlePublicationInfo(ZenodoRestRecord restRecord, Mods.Builder mods) {
        String publicationDate = restRecord.getMetadata().getPublication_date();
        OriginInfo.Builder originInfo = OriginInfo.builderForOriginInfo();

        if (publicationDate != null) {
            originInfo.addContent(DateIssued.builderForDateIssued().encoding(DateEncoding.W3CDTF).keyDate(Yes.YES)
                .content(publicationDate).build());

        }

        originInfo.addContent(Publisher.builderForPublisher().content("Zenodo").build());
        mods.addContent(originInfo.build());
    }

    private void handleCreators(ZenodoRestRecord restRecord, Mods.Builder mods) {
        List<ZenodoRestPerson> creators = restRecord.getMetadata().getCreators();
        if (creators == null) {
            return;
        }
        for (ZenodoRestPerson creator : creators) {
            String name = creator.getName();
            String affiliation = creator.getAffiliation();
            String orcid = creator.getOrcid();
            Name.Builder builder = Name.builder();

            if (name != null) {
                builder.addContent(DisplayForm.builder().content(name).build());
            }

            if (affiliation != null) {
                builder.addContent(Affiliation.builder().content(affiliation).build());
            }

            if (orcid != null) {
                builder.addContent(NameIdentifier.builder().content(orcid).type("orcid").build());
            }

            if (name != null || affiliation != null || orcid != null) {
                mods.addContent(builder.build());
            }

            RoleTerm roleTerm
                = RoleTerm.builder().type(CodeOrText.CODE).authority("marcrelator").content("aut").build();
            Role role = Role.builder().addRoleTerm(roleTerm).build();
            builder.addContent(role);
        }
    }

    private boolean handleContributors(ZenodoRestRecord restRecord, Mods.Builder mods) {
        List<ZenodoRestPerson> contributors = restRecord.getMetadata().getContributors();
        if (contributors == null) {
            return true;
        }
        for (ZenodoRestPerson contributor : contributors) {
            String name = contributor.getName();
            String affiliation = contributor.getAffiliation();
            String orcid = contributor.getOrcid();
            Name.Builder builder = Name.builder();

            if (name != null) {
                builder.addContent(DisplayForm.builder().content(name).build());
            }

            if (affiliation != null) {
                builder.addContent(Affiliation.builder().content(affiliation).build());
            }

            if (orcid != null) {
                builder.addContent(NameIdentifier.builder().content(orcid).type("orcid").build());
            }

            String zenodoRole = contributor.getType();
            String mappingResult = "ctb";
            if (zenodoRole != null) {
                String roleMappingName = config.get(ROLE_MAPPING_PROPERTY);
                MappingGroup roleMappingGroup = mappingService.getGroupByName(roleMappingName);

                Optional<Mapping> mappedValue = mappingService.getMappingByGroupAndFrom(roleMappingGroup, zenodoRole);
                Optional<String> toOptional = mappedValue.filter(m -> m.getTo() != null)
                    .filter(m -> !m.getTo().isBlank())
                    .map(Mapping::getTo);
                if (toOptional.isEmpty()) {
                    log.warn("Could not find role mapping for {} in record {}", zenodoRole, restRecord.getId());
                    return false;
                }
            }

            RoleTerm roleTerm
                = RoleTerm.builder().type(CodeOrText.CODE).authority("marcrelator").content(mappingResult).build();
            Role role = Role.builder().addRoleTerm(roleTerm).build();
            builder.addContent(role);

            if (name != null || affiliation != null || orcid != null) {
                mods.addContent(builder.build());
            }
        }
        return true;
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
        XPathExpression<Element> parentXPath
            = XPathFactory.instance().compile(xp, Filters.element(), null, MODS_NAMESPACE, XLINK_NAMESPACE);
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

        String metadata = record.getMetadata();
        ZenodoRestRecord restRecord = parseMetadata(metadata);

        checkTypeMapping(missingMappings, restRecord);
        checkLicenseMapping(missingMappings, restRecord);
        checkContributerMapping(missingMappings, restRecord);

        /*
         * Controlled vocabulary:
         * open: Open Access
         * embargoed: Embargoed Access
         * restricted: Restricted Access
         * closed: Closed Access
        
        String accessRight = restRecord.getMetadata().getAccess_right();
        if (accessRight != null) {
            String accessRightMappingGroupName = config.get("accessRight");
            MappingGroup accessRightMappingGroup = mappingService.getGroupByName(accessRightMappingGroupName);
            Optional<Mapping> mappedValue = mappingService.getMappingByGroupAndFrom(accessRightMappingGroup,
                accessRight);
            Optional<String> toOptional = mappedValue.filter(m -> m.getTo() != null)
                .filter(m -> !m.getTo().isBlank())
                .map(Mapping::getTo);
            if (mappedValue.isEmpty()) {
                Mapping mapping = mappingService.addMapping(accessRightMappingGroup, accessRight, null);
                missingMappings.add(mapping);
            } else if (toOptional.isEmpty()) {
                missingMappings.add(mappedValue.get());
            }
        }    */

        return missingMappings;
    }

    private void checkTypeMapping(List<Mapping> missingMappings, ZenodoRestRecord restRecord) {
        ZenodoRestResourceType resourceType = restRecord.getMetadata().getResource_type();

        String completeType = getZenodoType(resourceType);
        String mappingGroupName = config.get(GENRE_MAPPING_PROPERTY);
        MappingGroup mappingGroup = mappingService.getGroupByName(mappingGroupName);
        Optional<Mapping> mappedValue = mappingService.getMappingByGroupAndFrom(mappingGroup, completeType);
        Optional<String> toOptional = mappedValue.filter(m -> m.getTo() != null)
            .filter(m -> !m.getTo().isBlank())
            .map(Mapping::getTo);

        if (mappedValue.isEmpty()) {
            Mapping mapping = mappingService.addMapping(mappingGroup, completeType, null);
            missingMappings.add(mapping);
        } else if (toOptional.isEmpty()) {
            missingMappings.add(mappedValue.get());
        }

    }

    private void checkLicenseMapping(List<Mapping> missingMappings, ZenodoRestRecord restRecord) {
        ZenodoRestLicense license = restRecord.getMetadata().getLicense();
        if (license != null) {
            String licenseID = license.getId();
            String licenseMappingGroupName = config.get(LICENSE_MAPPING_PROPERTY);
            MappingGroup licenseMappingGroup = mappingService.getGroupByName(licenseMappingGroupName);
            Optional<Mapping> mappedValue = mappingService.getMappingByGroupAndFrom(licenseMappingGroup, licenseID);
            Optional<String> toOptional = mappedValue
                .filter(m -> m.getTo() != null)
                .filter(m -> !m.getTo().isBlank())
                .map(Mapping::getTo);
            if (mappedValue.isEmpty()) {
                Mapping mapping = mappingService.addMapping(licenseMappingGroup, licenseID, null);
                missingMappings.add(mapping);
            } else if (toOptional.isEmpty()) {
                missingMappings.add(mappedValue.get());
            }
        }
    }

    private void checkContributerMapping(List<Mapping> missingMappings, ZenodoRestRecord restRecord) {
        List<ZenodoRestPerson> contributors = restRecord.getMetadata().getContributors();
        if (contributors == null) {
            return;
        }
        String roleMappingName = config.get(ROLE_MAPPING_PROPERTY);
        for (ZenodoRestPerson contributor : contributors) {
            String type = contributor.getType();
            if (type != null) {
                MappingGroup roleMappingGroup = mappingService.getGroupByName(roleMappingName);
                Optional<Mapping> mappingOptional = mappingService.getMappingByGroupAndFrom(roleMappingGroup, type);
                Optional<String> mappingTargetOptional = mappingOptional.filter(m -> m.getTo() != null)
                    .filter(m -> !m.getTo().isBlank())
                    .map(Mapping::getTo);
                if (mappingOptional.isEmpty()) {
                    Mapping addedMapping = mappingService.addMapping(roleMappingGroup, type, null);
                    missingMappings.add(addedMapping);
                } else if (mappingTargetOptional.isEmpty()) {
                    missingMappings.add(mappingOptional.get());
                }
            }
        }
    }

    private org.jdom2.Document convertEntity(MyCoReTargetConfiguration target, ForeignEntity recordEntity) {
        String metadata = recordEntity.getMetadata();
        ZenodoRestRecord restRecord = parseMetadata(metadata);

        Mods.Builder mods = Mods.builder();

        handleTitle(restRecord, mods);
        handleAbstract(restRecord, mods);
        handleSubject(restRecord, mods);
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

        if (!handleLicense(restRecord, mods)) {
            return null;
        }

        if (Objects.equals(config.get("files"), "modsLocation")) {
            Location.Builder location = Location.builderForLocation();

            for (ZenodoRestFileMetadata file : restRecord.getFiles()) {
                String link = file.getLinks().getSelf();
                String name = file.getKey();

                location.addUrl(Url.builderForUrl().content(link)
                    .access(UrlAccess.RAW_OBJECT)
                    .displayLabel(name).build());
            }

            mods.addContent(location.build());
        }

        ZenodoRestRelations relations = restRecord.getMetadata().getRelations();
        if (relations != null) {
            List<ZenodoRestVersion> version = relations.getVersion();
            if (version != null) {
                Optional<ZenodoRestVersion> first = version.stream().findFirst();
                if (first.isPresent()) {
                    ZenodoRestVersion zenodoRestVersion = first.get();
                    String pidType = zenodoRestVersion.getParent().getPid_type();
                    if (pidType.equals("recid")) {
                        String pidValue = zenodoRestVersion.getParent().getPid_value();
                        MyCoReObjectInfo mycoreObject = objectInfoRepository
                            .findFirstByRepositoryAndImportURLAndImportID(target.getUrl(),
                                recordEntity.getConfigId(),
                                pidValue);
                        RelatedItem.Builder relatedItem = RelatedItem.builderForRelatedItem();
                        relatedItem.otherType("has_grouping");
                        String title = restRecord.getTitle();
                        if (title != null) {
                            relatedItem.addContent(getTitleInfo(title));
                        }

                        Genre intern = Genre.builderForGenre().type("intern")
                            .authorityURI("http://www.mycore.org/classifications/mir_genres")
                            .valueURI("http://www.mycore.org/classifications/mir_genres#grouping").build();
                        relatedItem.addContent(intern);

                        RecordInfo recordInfo = getRecordInfoBuilder(pidValue, recordEntity.getConfigId()).build();
                        relatedItem.addContent(recordInfo);

                        if (restRecord.getConceptdoi() != null) {
                            relatedItem.addContent(
                                Identifier.builderForIdentifier().content(restRecord.getConceptdoi()).type("doi")
                                    .build());
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
                }

            }

        }

        StringWriter xmlStringWriter = new StringWriter();
        StreamResult streamResult = new StreamResult(xmlStringWriter);
        try {
            MODSXMLProcessor.getInstance().marshal(mods.build(), streamResult, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        org.jdom2.Document document = MODSUtil.wrapInMyCoReFrame(xmlStringWriter.toString(), config.get("base-id") ,getStatus());
        return document;
    }

    private boolean handleLicense(ZenodoRestRecord restRecord, Mods.Builder mods) {
        if (restRecord.getMetadata().getLicense() != null) {
            String id = restRecord.getMetadata().getLicense().getId();
            String licenseMappingGroupName = config.get(LICENSE_MAPPING_PROPERTY);
            MappingGroup licenseMappingGroup = mappingService.getGroupByName(licenseMappingGroupName);
            Optional<Mapping> mappedValue = mappingService.getMappingByGroupAndFrom(licenseMappingGroup, id);
            Optional<String> toOptional = mappedValue.filter(m -> m.getTo() != null)
                .filter(m -> !m.getTo().isBlank())
                .map(Mapping::getTo);
            if (toOptional.isPresent()) {
                String license = toOptional.get();
                AccessCondition accessCondition
                    = AccessCondition.builderForAccessCondition().type("use and reproduction")
                        .xlinkHref("http://www.mycore.org/classifications/mir_licenses#" + license)
                        .build();
                accessCondition.setXlinkType("simple");
                mods.addContent(accessCondition);
                return true;
            } else {
                log.info("Could not find license mapping for {} in record {}", id, restRecord.getId());
                return false;
            }
        }
        return true;
    }

    private boolean handleGenre(ZenodoRestRecord restRecord, Mods.Builder mods) {
        ZenodoRestResourceType resourceType = restRecord.getMetadata().getResource_type();
        String completeType = getZenodoType(resourceType);
        String mappingGroupName = config.get(GENRE_MAPPING_PROPERTY);
        MappingGroup mappingGroup = mappingService.getGroupByName(mappingGroupName);
        Optional<String> genreStrOptional
            = mappingService.getMappingByGroupAndFrom(mappingGroup, completeType).filter(m -> m.getTo() != null)
                .filter(m -> !m.getTo().isBlank())
                .map(Mapping::getTo);

        if (genreStrOptional.isEmpty()) {
            log.warn("Could not find genre mapping for {} in record {}", completeType, restRecord.getId());
            return false;
        }

        Genre genre = Genre.builderForGenre().type("intern")
            .authorityURI("http://www.mycore.org/classifications/mir_genres")
            .valueURI("http://www.mycore.org/classifications/mir_genres#" + genreStrOptional.get()).build();
        mods.addContent(genre);
        return true;
    }



    private String getStatus() {
        return config.get("status");
    }

    @Override
    public void setConfig(Map<String, String> importerConfig) {
        this.config = importerConfig;
    }

    private void handleAbstract(ZenodoRestRecord restRecord, Mods.Builder mods) {
        String description = restRecord.getMetadata().getDescription();
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
                JAXBElement<Abstract> jaxbAbstractElement
                    = new JAXBElement<Abstract>(abstractQName, Abstract.class, virtualAbstract);

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
