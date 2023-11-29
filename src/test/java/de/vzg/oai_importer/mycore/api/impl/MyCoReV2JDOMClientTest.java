package de.vzg.oai_importer.mycore.api.impl;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

@Log4j2()
class MyCoReV2JDOMClientTest {

    @Test
    void test() throws IOException, URISyntaxException {
        /*
        MyCoReV2JDOMClient client = new MyCoReV2JDOMClient(new ApacheHttpClientTransferLayer());


        MyCoReObjectQuery query = new MyCoReObjectQuery();

        MyCoReObjectList list = client.listObjects("https://www.openagrar.de/", query, null);
        Document b2importMods00000004 = client.getObject("https://www.openagrar.de/",
                "b2import_mods_00000004", null);


        list.getEntries().forEach(log::info);
        String mods = new XMLOutputter().outputString(b2importMods00000004);
        log.info(mods);
        */
    }

}
