package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.CircleRequestCreateDTO;
import com.sidequest.sidequest.dto.CircleBlockCreateDTO;
import com.sidequest.sidequest.dto.CircleRelationDTO;
import com.sidequest.sidequest.dto.CircleRelationStatus;
import com.sidequest.sidequest.dto.CircleOverviewDTO;
import com.sidequest.sidequest.dto.CircleContactDTO;
import com.sidequest.sidequest.dto.CircleRequestResponseDTO;
import com.sidequest.sidequest.dto.CircleSearchResultDTO;
import com.sidequest.sidequest.mapper.CircleRequestMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.CircleRequest;
import com.sidequest.sidequest.repository.AppUserRepository;
import com.sidequest.sidequest.repository.CircleRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CircleService {
    private final CircleRequestRepository circleRequestRepository;
    private final AppUserRepository appUserRepository;
    private final CircleRequestMgr circleRequestMgr;
    private final QuestNewsService questNewsService;

    public List<CircleRequestResponseDTO> getMyCircles(AppUser currentUser) {
        return circleRequestRepository.findAcceptedByUserId(currentUser.getId())
                .stream()
                .map(circleRequestMgr::toDto)
                .toList();
    }

    public CircleOverviewDTO getOverview(AppUser currentUser) {
        return CircleOverviewDTO.builder()
                .circles(getCircleContacts(currentUser))
                .incomingRequests(getIncomingRequests(currentUser))
                .outgoingRequests(getOutgoingRequests(currentUser))
                .inviteCandidates(getInviteCandidates(currentUser))
                .build();
    }

    private List<CircleContactDTO> getCircleContacts(AppUser currentUser) {
        return circleRequestRepository.findAcceptedByUserId(currentUser.getId()).stream()
                .map(relation -> toContact(currentUser, relation))
                .toList();
    }

    private CircleContactDTO toContact(AppUser currentUser, CircleRequest relation) {
        AppUser contact = relation.getRequester().getId().equals(currentUser.getId())
                ? relation.getRecipient()
                : relation.getRequester();

        return CircleContactDTO.builder()
                .relationId(relation.getId())
                .userId(contact.getId())
                .username(contact.getUsername())
                .profileDescription(contact.getProfileDescription())
                .profileAvatarDataUrl(contact.getProfileAvatarDataUrl())
                .build();
    }

    public List<CircleRequestResponseDTO> getIncomingRequests(AppUser currentUser) {
        return circleRequestRepository.findIncomingPendingByRecipientId(currentUser.getId())
                .stream()
                .map(circleRequestMgr::toDto)
                .toList();
    }

    public List<CircleRequestResponseDTO> getOutgoingRequests(AppUser currentUser) {
        return circleRequestRepository.findOutgoingPendingByRequesterId(currentUser.getId())
                .stream()
                .map(circleRequestMgr::toDto)
                .toList();
    }

    public List<CircleSearchResultDTO> getInviteCandidates(AppUser currentUser) {
        return appUserRepository.findAll().stream()
                .filter(candidate -> !candidate.getId().equals(currentUser.getId()))
                .map(candidate -> toSearchResult(currentUser, candidate))
                .filter(candidate -> candidate.getRelationStatus() == CircleRelationStatus.NONE)
                .sorted(Comparator.comparing(CircleSearchResultDTO::getUsername, String.CASE_INSENSITIVE_ORDER))
                .limit(12)
                .toList();
    }

    public CircleRelationDTO getRelationWithUser(AppUser currentUser, Long otherUserId) {
        if (currentUser == null || otherUserId == null) {
            return CircleRelationDTO.builder()
                    .relationStatus(CircleRelationStatus.NONE)
                    .blockedByCurrentUser(false)
                    .build();
        }

        AppUser otherUser = requireAppUser(otherUserId);
        Optional<CircleRequest> relation = findRelation(currentUser, otherUser);
        return CircleRelationDTO.builder()
                .relationStatus(resolveRelationStatus(relation, currentUser.getId()))
                .blockedByCurrentUser(isBlockedByCurrentUser(relation, currentUser.getId()))
                .build();
    }

    public List<CircleSearchResultDTO> searchCircleUsers(AppUser currentUser, String query) {
        String normalizedQuery = SearchQueryNormalizer.normalize(query);

        if (normalizedQuery.length() < 2) {
            return List.of();
        }

        List<AppUser> matches = appUserRepository.searchByUsernameOrEmail(normalizedQuery);

        return matches.stream()
                .filter(candidate -> !candidate.getId().equals(currentUser.getId()))
                .map(candidate -> toSearchResult(currentUser, candidate))
                .sorted(Comparator.comparing(CircleSearchResultDTO::getUsername, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public CircleRequestResponseDTO createCircleRequest(CircleRequestCreateDTO dto, AppUser currentUser) {
        Long recipientId = requireTargetUserId(dto.getRecipientId(), "Recipient is required");
        validateNotSelfAction(currentUser, recipientId, "You cannot send a circle request to yourself");

        AppUser recipient = requireAppUser(recipientId);
        CircleRequest existingRelation = findRelation(currentUser, recipient).orElse(null);
        if (existingRelation != null && existingRelation.getBlockedAt() != null) {
            throw ServiceErrors.conflict("This user is blocked");
        }

        if (existingRelation != null) {
            throw ServiceErrors.conflict("A circle already exists between these users");
        }

        CircleRequest circleRequest = new CircleRequest();
        circleRequest.setRequester(currentUser);
        circleRequest.setRecipient(recipient);
        CircleRequest saved = circleRequestRepository.save(circleRequest);
        return circleRequestMgr.toDto(saved);
    }

    @Transactional
    public CircleRequestResponseDTO acceptCircleRequest(Long requestId, AppUser currentUser) {
        CircleRequest circleRequest = requireIncomingRequest(requestId, currentUser);
        circleRequest.setAcceptedAt(Instant.now());
        CircleRequest saved = circleRequestRepository.save(circleRequest);
        questNewsService.notifyCircleRequestAccepted(saved.getRequester(), currentUser);
        return circleRequestMgr.toDto(saved);
    }

    @Transactional
    public void deleteCircleRequest(Long requestId, AppUser currentUser) {
        CircleRequest circleRequest = requireRequestAccessibleByCurrentUser(requestId, currentUser);
        circleRequestRepository.delete(circleRequest);
    }

    @Transactional
    public CircleRequestResponseDTO blockCircleUser(CircleBlockCreateDTO dto, AppUser currentUser) {
        Long blockedUserId = requireTargetUserId(dto.getBlockedUserId(), "Blocked user is required");
        validateNotSelfAction(currentUser, blockedUserId, "You cannot block yourself");

        AppUser blockedUser = requireAppUser(blockedUserId);
        CircleRequest existingRelation = findRelation(currentUser, blockedUser).orElse(null);
        if (existingRelation != null && existingRelation.getBlockedAt() != null) {
            if (existingRelation.getBlockedBy() != null && Objects.equals(existingRelation.getBlockedBy().getId(), currentUser.getId())) {
                throw ServiceErrors.conflict("This user is already blocked");
            }

            throw ServiceErrors.conflict("This user has blocked you");
        }

        if (existingRelation != null) {
            circleRequestRepository.delete(existingRelation);
        }

        CircleRequest circleRequest = new CircleRequest();
        circleRequest.setRequester(currentUser);
        circleRequest.setRecipient(blockedUser);
        circleRequest.setAcceptedAt(null);
        circleRequest.setBlockedAt(Instant.now());
        circleRequest.setBlockedBy(currentUser);
        CircleRequest saved = circleRequestRepository.save(circleRequest);
        return circleRequestMgr.toDto(saved);
    }

    @Transactional
    public void unblockCircleUser(Long blockedUserId, AppUser currentUser) {
        Long targetUserId = requireTargetUserId(blockedUserId, "Blocked user is required");
        validateNotSelfAction(currentUser, targetUserId, "You cannot unblock yourself");

        AppUser blockedUser = requireAppUser(targetUserId);
        CircleRequest circleRequest = findRelation(currentUser, blockedUser)
                .orElseThrow(() -> ServiceErrors.notFound("Blocked user not found"));

        if (circleRequest.getBlockedAt() == null) {
            throw ServiceErrors.badRequest("This user is not blocked");
        }

        if (circleRequest.getBlockedBy() == null || !Objects.equals(circleRequest.getBlockedBy().getId(), currentUser.getId())) {
            throw ServiceErrors.forbidden("Only the user who blocked this person can unblock them");
        }

        circleRequestRepository.delete(circleRequest);
    }

    public boolean isCircleBetween(AppUser leftUser, AppUser rightUser) {
        if (leftUser == null || rightUser == null) {
            return false;
        }

        return circleRequestRepository.findBetweenUsers(leftUser.getId(), rightUser.getId())
                .map(circleRequest -> circleRequest.getAcceptedAt() != null)
                .orElse(false);
    }

    private CircleRequest requireIncomingRequest(Long requestId, AppUser currentUser) {
        CircleRequest circleRequest = requireRequest(requestId);
        if (!circleRequest.getRecipient().getId().equals(currentUser.getId())) {
            throw ServiceErrors.forbidden("Only the recipient can accept this circle request");
        }

        if (circleRequest.getAcceptedAt() != null) {
            throw ServiceErrors.badRequest("This circle request has already been accepted");
        }

        return circleRequest;
    }

    private CircleRequest requireRequestAccessibleByCurrentUser(Long requestId, AppUser currentUser) {
        CircleRequest circleRequest = requireRequest(requestId);
        Long requesterId = circleRequest.getRequester().getId();
        Long recipientId = circleRequest.getRecipient().getId();

        if (!currentUser.getId().equals(requesterId) && !currentUser.getId().equals(recipientId)) {
            throw ServiceErrors.forbidden("You can only manage your own circle requests");
        }

        if (circleRequest.getBlockedAt() != null) {
            throw ServiceErrors.forbidden("Blocked relationships must be managed through the unblock action");
        }

        return circleRequest;
    }

    private CircleRequest requireRequest(Long requestId) {
        return circleRequestRepository.findById(requestId)
                .orElseThrow(() -> ServiceErrors.notFound("Circle request not found with id " + requestId));
    }

    private AppUser requireAppUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> ServiceErrors.notFound("AppUser not found with id " + userId));
    }

    private Long requireTargetUserId(Long targetUserId, String message) {
        if (targetUserId == null) {
            throw ServiceErrors.badRequest(message);
        }

        return targetUserId;
    }

    private void validateNotSelfAction(AppUser currentUser, Long targetUserId, String message) {
        if (currentUser.getId().equals(targetUserId)) {
            throw ServiceErrors.badRequest(message);
        }
    }

    private Optional<CircleRequest> findRelation(AppUser leftUser, AppUser rightUser) {
        return circleRequestRepository.findBetweenUsers(leftUser.getId(), rightUser.getId());
    }

    private CircleSearchResultDTO toSearchResult(AppUser currentUser, AppUser candidate) {
        Optional<CircleRequest> relation = findRelation(currentUser, candidate);
        return CircleSearchResultDTO.builder()
                .id(candidate.getId())
                .username(candidate.getUsername())
                .profileDescription(candidate.getProfileDescription())
                .profileAvatarDataUrl(candidate.getProfileAvatarDataUrl())
                .email(candidate.getEmail())
                .relationStatus(resolveRelationStatus(relation, currentUser.getId()))
                .blockedByCurrentUser(isBlockedByCurrentUser(relation, currentUser.getId()))
                .build();
    }

    private CircleRelationStatus resolveRelationStatus(Optional<CircleRequest> relation, Long currentUserId) {
        return relation
                .map(circleRequest -> {
                    if (circleRequest.getBlockedAt() != null) {
                        return CircleRelationStatus.BLOCKED;
                    }

                    if (circleRequest.getAcceptedAt() != null) {
                        return CircleRelationStatus.CIRCLE;
                    }

                    if (Objects.equals(circleRequest.getRequester().getId(), currentUserId)) {
                        return CircleRelationStatus.OUTGOING_REQUEST;
                    }

                    return CircleRelationStatus.INCOMING_REQUEST;
                })
                .orElse(CircleRelationStatus.NONE);
    }

    private boolean isBlockedByCurrentUser(Optional<CircleRequest> relation, Long currentUserId) {
        return relation
                .map(circleRequest -> circleRequest.getBlockedAt() != null
                        && circleRequest.getBlockedBy() != null
                        && Objects.equals(circleRequest.getBlockedBy().getId(), currentUserId))
                .orElse(false);
    }
}
