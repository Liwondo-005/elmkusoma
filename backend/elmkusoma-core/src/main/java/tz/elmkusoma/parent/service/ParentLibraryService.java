package tz.elmkusoma.parent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.parent.dto.ParentLibraryResponse;
import tz.elmkusoma.parent.dto.ParentLibraryResponse.LibraryCategory;
import tz.elmkusoma.parent.dto.ParentLibraryResponse.LibraryItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParentLibraryService {

    private final ResourceRepository resourceRepository;

    public ParentLibraryResponse getLibrary(UUID institutionId) {
        List<Resource> allResources = resourceRepository.findAllAndIsDeletedFalse();

        List<LibraryCategory> categories = new ArrayList<>();

        List<Resource> documents = allResources.stream()
                .filter(r -> r.getResourceType() == Resource.ResourceType.DOCUMENT)
                .collect(Collectors.toList());
        if (!documents.isEmpty()) {
            categories.add(LibraryCategory.builder()
                    .name("Help Your Child")
                    .description("Study guides and learning materials")
                    .items(documents.stream().map(this::toLibraryItem).collect(Collectors.toList()))
                    .build());
        }

        List<Resource> videos = allResources.stream()
                .filter(r -> r.getResourceType() == Resource.ResourceType.VIDEO)
                .collect(Collectors.toList());
        if (!videos.isEmpty()) {
            categories.add(LibraryCategory.builder()
                    .name("Video Resources")
                    .description("Educational videos for learning support")
                    .items(videos.stream().map(this::toLibraryItem).collect(Collectors.toList()))
                    .build());
        }

        List<Resource> other = allResources.stream()
                .filter(r -> r.getResourceType() != Resource.ResourceType.DOCUMENT
                        && r.getResourceType() != Resource.ResourceType.VIDEO)
                .collect(Collectors.toList());
        if (!other.isEmpty()) {
            categories.add(LibraryCategory.builder()
                    .name("Learning Resources")
                    .description("Additional learning materials")
                    .items(other.stream().map(this::toLibraryItem).collect(Collectors.toList()))
                    .build());
        }

        return ParentLibraryResponse.builder()
                .categories(categories)
                .build();
    }

    private LibraryItem toLibraryItem(Resource r) {
        return LibraryItem.builder()
                .id(r.getId().toString())
                .title(r.getTitle())
                .description(r.getDescription())
                .resourceType(r.getResourceType() != null ? r.getResourceType().name() : null)
                .fileUrl(r.getFileUrl())
                .build();
    }
}
