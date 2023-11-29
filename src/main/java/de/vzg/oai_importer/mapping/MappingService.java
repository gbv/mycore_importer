package de.vzg.oai_importer.mapping;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import de.vzg.oai_importer.mapping.jpa.Mapping;
import de.vzg.oai_importer.mapping.jpa.MappingGroup;
import de.vzg.oai_importer.mapping.jpa.MappingGroupRepository;
import de.vzg.oai_importer.mapping.jpa.MappingRepository;

@Service
public class MappingService {

    @Autowired
    private MappingRepository mappingRepository;

    @Autowired
    private MappingGroupRepository mappingGroupRepository;

    public Page<MappingGroup> getMappingGroups(int page, int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return mappingGroupRepository.findAll(pageable);
    }


    public MappingGroup getGroup(Long id) {
        return mappingGroupRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No group with id " + id));
    }

    public MappingGroup getGroupByName(String name) {
        return mappingGroupRepository.findByName(name).orElseThrow(() -> new IllegalArgumentException("No group with name " + name));
    }

    public List<MappingGroup> getGroupsByTarget(String target) {
        return mappingGroupRepository.findByTarget(target);
    }

    public Optional<Mapping> getMappingByGroupAndFrom(MappingGroup group, String from) {
        return mappingRepository.findByMappingGroupAndFrom(group, from);
    }

    public MappingGroup updateGroup(Long id, String name, String description, String target, String classId) {
        MappingGroup group = getGroup(id);
        group.setName(name);
        group.setDescription(description);
        group.setTarget(target);
        group.setClassId(classId);
        return mappingGroupRepository.save(group);
    }

    public MappingGroup addGroup(String name, String description, String target, String classId) {
        MappingGroup group = new MappingGroup();
        group.setName(name);
        group.setDescription(description);
        group.setTarget(target);
        group.setClassId(classId);
        return mappingGroupRepository.save(group);
    }

    public Page<Mapping> getMappingsByGroup(MappingGroup mappingGroup, int page, int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return mappingRepository.findByMappingGroup(mappingGroup, pageable);
    }

    public Mapping addMapping(MappingGroup group, String from, String to) {
        Mapping mapping = new Mapping();
        mapping.setMappingGroup(group);
        mapping.setFrom(from);
        mapping.setTo(to);
        return mappingRepository.save(mapping);
    }

    public Mapping updateMapping(Long mid, String from, String to) {
        Mapping mapping = getMapping(mid);
        mapping.setFrom(from);
        mapping.setTo(to);
        return mappingRepository.save(mapping);
    }

    public Mapping getMapping(Long mid) {
        return mappingRepository.findById(mid).orElseThrow(() -> new IllegalArgumentException("No mapping with id " + mid));
    }

    public void deleteMapping(Long mid) {
        mappingRepository.deleteById(mid);
    }
}
