package de.vzg.oai_importer.mycore;

import org.jdom2.Document;
import org.jdom2.Element;

import java.util.Optional;

public class MyCoReUtil {

    public static Document createDerivate(String base_id, String parent, String mainFile) {
        Element mycorederivateElement = new Element("mycorederivate");
        Document document = new Document(mycorederivateElement);

        mycorederivateElement.setAttribute("ID", base_id + "_00000000");

        Element derivateElement = new Element("derivate");
        mycorederivateElement.addContent(derivateElement);

        Element linkmetasElement = new Element("linkmetas");
        derivateElement.addContent(linkmetasElement);
        linkmetasElement.setAttribute("class", "MCRMetaLinkID");
        linkmetasElement.setAttribute("heritable", "false");

        Element linkmetaElement = new Element("linkmeta");
        linkmetasElement.addContent(linkmetaElement);
        linkmetaElement.setAttribute("inherited", "0");
        linkmetaElement.setAttribute("type", "locator", MODSUtil.XLINK_NAMESPACE);
        linkmetaElement.setAttribute("href", parent, MODSUtil.XLINK_NAMESPACE);

        Element internalsElement = new Element("internals");
        derivateElement.addContent(internalsElement);
        internalsElement.setAttribute("class", "MCRMetaIFS");
        internalsElement.setAttribute("heritable", "false");

        Element internalElement = new Element("internal");
        internalsElement.addContent(internalElement);
        internalElement.setAttribute("inherited", "0");
        internalElement.setAttribute("maindoc", mainFile);

        Element classificationsElement = new Element("classifications");
        derivateElement.addContent(classificationsElement);
        classificationsElement.setAttribute("class", "MCRMetaClassification");
        classificationsElement.setAttribute("heritable", "false");

        Element classificationElement = new Element("classification");
        classificationsElement.addContent(classificationElement);
        classificationElement.setAttribute("inherited", "0");
        classificationElement.setAttribute("categid", "content");
        classificationElement.setAttribute("classid", "derivate_types");


        Element serviceElement = new Element("service");
        mycorederivateElement.addContent(serviceElement);

        return document;
    }

    public static void setMainFile(Document insertedDerivate, String mainFile) {
        Optional.ofNullable(insertedDerivate.getRootElement())
                .map(e -> e.getChild("derivate"))
                .map(e -> e.getChild("internals"))
                .map(e -> e.getChild("internal"))
                .ifPresent(e -> e.setAttribute("maindoc", mainFile));
    }
}
