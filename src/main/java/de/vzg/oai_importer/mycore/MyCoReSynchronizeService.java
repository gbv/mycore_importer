package de.vzg.oai_importer.mycore;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.mycore.api.MyCoReObjectQuery;
import de.vzg.oai_importer.mycore.api.model.MyCoReObjectList;
import de.vzg.oai_importer.mycore.api.model.MyCoReObjectListEntry;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfo;
import de.vzg.oai_importer.mycore.jpa.MyCoReObjectInfoRepository;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class MyCoReSynchronizeService {

    @Autowired
    MyCoReRestAPIService mycoreRest;

    @Autowired
    MyCoReObjectInfoRepository mycoreRepo;


    public List<MyCoReObjectInfo> synchronize(MyCoReTargetConfiguration target) throws IOException, URISyntaxException {
        MyCoReObjectQuery query = new MyCoReObjectQuery();

        MyCoReObjectInfo newest = mycoreRepo.findFirstByRepositoryOrderByCreatedDesc(target.getUrl());
        if (newest != null) {
            OffsetDateTime created = newest.getCreated();
            query.setCreatedAfter(created.toInstant());
        }


        query.setCreatedBy(target.getUser());
        boolean hasMore = true;
        query.setLimit(1000);
        query.setOffset(0);
        List<MyCoReObjectInfo> infos = new ArrayList<>();

        while (hasMore) {
            MyCoReObjectList objects = mycoreRest.getObjects(target, query);
            for (MyCoReObjectListEntry entry : objects.getEntries()) {
                MyCoReObjectInfo info = mycoreRepo.findByMycoreIdAndRepository(entry.getObjectID(), target.getUrl());
                if (info == null) {
                    info = new MyCoReObjectInfo();
                }
                info.setMycoreId(entry.getObjectID());
                info.setRepository(target.getUrl());

                Document object = mycoreRest.getObject(target, entry.getObjectID());

                String parent = MODSUtil.getParent(object);
                info.setParentMycoreId(parent);

                OffsetDateTime createDate = MODSUtil.getCreateDate(object);
                info.setCreated(createDate);

                OffsetDateTime lastModified = MODSUtil.getLastModified(object);
                info.setLastModified(lastModified);

                MODSUtil.MODSRecordInfo recordInfo = MODSUtil.getRecordInfo(object);
                if (recordInfo != null) {
                    String id = recordInfo.id();
                    if (id != null) {
                        info.setImportID(id);
                    }
                    String source = recordInfo.url();
                    if (source != null) {
                        info.setImportURL(source);
                    }
                }

                String createdBy = MODSUtil.getCreatedBy(object);
                if (createdBy == null) {
                    log.error("Could not extract createdBy from " + entry.getObjectID());
                    continue;
                }
                info.setCreatedBy(createdBy);

                String state = MODSUtil.getState(object);
                info.setState(state);

                infos.add(info);
                mycoreRepo.save(info);
            }
            hasMore = objects.getEntries().size() == 1000;
            query.setOffset(query.getOffset() + 1000);
        }


        return infos;
    }
}
