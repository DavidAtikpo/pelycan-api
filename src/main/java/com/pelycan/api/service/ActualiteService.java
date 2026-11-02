package com.pelycan.api.service;

import com.pelycan.api.domain.Actualite;
import com.pelycan.api.domain.ActualiteCategory;
import com.pelycan.api.domain.ActualiteStatus;
import com.pelycan.api.domain.User;
import com.pelycan.api.repository.ActualiteRepository;
import com.pelycan.api.web.dto.ActualiteAdminDto;
import com.pelycan.api.web.dto.ActualitePublicDto;
import com.pelycan.api.web.dto.ActualiteWriteRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ActualiteService {

    private final ActualiteRepository actualiteRepository;

    public ActualiteService(ActualiteRepository actualiteRepository) {
        this.actualiteRepository = actualiteRepository;
    }

    @Cacheable(cacheNames = "actualites-public", key = "#category")
    @Transactional(readOnly = true)
    public List<ActualitePublicDto> listPublished(String category) {
        List<Actualite> rows;
        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            try {
                ActualiteCategory cat = ActualiteCategory.valueOf(category);
                rows = actualiteRepository.findByStatusAndCategoryOrderByPublishedAtDescCreatedAtDesc(
                        ActualiteStatus.published, cat);
            } catch (IllegalArgumentException e) {
                rows = actualiteRepository.findByStatusOrderByPublishedAtDescCreatedAtDesc(ActualiteStatus.published);
            }
        } else {
            rows = actualiteRepository.findByStatusOrderByPublishedAtDescCreatedAtDesc(ActualiteStatus.published);
        }
        return rows.stream().map(ActualiteService::toPublic).toList();
    }

    @Transactional(readOnly = true)
    public List<ActualiteAdminDto> listAdmin() {
        return actualiteRepository.findAllForAdmin().stream().map(ActualiteService::toAdmin).toList();
    }

    @CacheEvict(cacheNames = "actualites-public", allEntries = true)
    @Transactional
    public ActualiteAdminDto create(ActualiteWriteRequest request, User author) {
        validate(request, false);
        Actualite row = new Actualite();
        applyWrite(row, request, true);
        row.setCreatedBy(author.getId());
        row.setUpdatedBy(author.getId());
        if (row.getStatus() == ActualiteStatus.published && row.getPublishedAt() == null) {
            row.setPublishedAt(Instant.now());
        }
        return toAdmin(actualiteRepository.save(row));
    }

    @CacheEvict(cacheNames = "actualites-public", allEntries = true)
    @Transactional
    public ActualiteAdminDto update(UUID id, ActualiteWriteRequest request, User editor) {
        Actualite row = actualiteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Actualité introuvable"));
        validate(request, true);
        applyWrite(row, request, false);
        row.setUpdatedBy(editor.getId());
        if (request.status() == ActualiteStatus.published && row.getPublishedAt() == null) {
            row.setPublishedAt(Instant.now());
        }
        if (request.status() == ActualiteStatus.draft) {
            row.setPublishedAt(null);
        }
        return toAdmin(actualiteRepository.save(row));
    }

    @CacheEvict(cacheNames = "actualites-public", allEntries = true)
    @Transactional
    public void delete(UUID id) {
        Actualite row = actualiteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Actualité introuvable"));
        actualiteRepository.delete(row);
    }

    private static void validate(ActualiteWriteRequest request, boolean partial) {
        List<String> errors = new ArrayList<>();
        if (!partial || request.title() != null) {
            if (request.title() == null || request.title().isBlank()) {
                errors.add("Le titre est requis");
            }
        }
        if (!partial || request.excerpt() != null) {
            if (request.excerpt() == null || request.excerpt().isBlank()) {
                errors.add("Le résumé est requis");
            }
        }
        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(", ", errors));
        }
    }

    private static void applyWrite(Actualite row, ActualiteWriteRequest request, boolean create) {
        if (request.title() != null) {
            row.setTitle(request.title().trim());
        } else if (create) {
            row.setTitle("");
        }
        if (request.excerpt() != null) {
            row.setExcerpt(request.excerpt().trim());
        } else if (create) {
            row.setExcerpt("");
        }
        if (request.content() != null) {
            row.setContent(request.content().isBlank() ? null : request.content().trim());
        }
        if (request.category() != null) {
            row.setCategory(request.category());
        } else if (create) {
            row.setCategory(ActualiteCategory.plateforme);
        }
        if (request.status() != null) {
            row.setStatus(request.status());
        } else if (create) {
            row.setStatus(ActualiteStatus.draft);
        }
        if (request.linkUrl() != null) {
            row.setLinkUrl(request.linkUrl().isBlank() ? null : request.linkUrl().trim());
        }
        if (request.linkLabel() != null) {
            row.setLinkLabel(request.linkLabel().isBlank() ? null : request.linkLabel().trim());
        }
    }

    private static ActualitePublicDto toPublic(Actualite a) {
        Instant date = a.getPublishedAt() != null ? a.getPublishedAt() : a.getCreatedAt();
        return new ActualitePublicDto(
                a.getId(),
                a.getTitle(),
                a.getExcerpt(),
                a.getCategory(),
                date,
                a.getLinkUrl(),
                a.getLinkLabel());
    }

    private static ActualiteAdminDto toAdmin(Actualite a) {
        ActualiteAdminDto.AuthorSummary author = null;
        if (a.getAuthor() != null) {
            author = new ActualiteAdminDto.AuthorSummary(
                    a.getAuthor().getId(),
                    a.getAuthor().getFirstName(),
                    a.getAuthor().getLastName());
        }
        return new ActualiteAdminDto(
                a.getId(),
                a.getTitle(),
                a.getExcerpt(),
                a.getContent(),
                a.getCategory(),
                a.getStatus(),
                a.getPublishedAt(),
                a.getLinkUrl(),
                a.getLinkLabel(),
                a.getCreatedAt(),
                a.getUpdatedAt(),
                author);
    }
}
