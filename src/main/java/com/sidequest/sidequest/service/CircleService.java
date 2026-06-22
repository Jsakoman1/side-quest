package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.CircleRequestCreateDTO;
import com.sidequest.sidequest.dto.CircleBlockCreateDTO;
import com.sidequest.sidequest.dto.CircleRelationDTO;
import com.sidequest.sidequest.dto.CircleRelationStatus;
import com.sidequest.sidequest.dto.CircleOverviewDTO;
import com.sidequest.sidequest.dto.CircleContactDTO;
import com.sidequest.sidequest.dto.CircleRequestResponseDTO;
import com.sidequest.sidequest.dto.CircleSearchResultDTO;
import com.sidequest.sidequest.dto.AdminCircleOverviewDTO;
import com.sidequest.sidequest.dto.AdminCircleGroupResponseDTO;
import com.sidequest.sidequest.dto.ConnectionCircleUpdateDTO;
import com.sidequest.sidequest.dto.CircleMemberDTO;
import com.sidequest.sidequest.dto.CircleGroupRequestDTO;
import com.sidequest.sidequest.dto.CircleGroupResponseDTO;
import com.sidequest.sidequest.mapper.CircleRequestMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.AppUserRole;
import com.sidequest.sidequest.model.CircleRequest;
import com.sidequest.sidequest.model.CircleGroup;
import com.sidequest.sidequest.model.CircleMembership;
import com.sidequest.sidequest.repository.AppUserRepository;
import com.sidequest.sidequest.repository.CircleRequestRepository;
import com.sidequest.sidequest.repository.CircleMembershipRepository;
import com.sidequest.sidequest.repository.CircleGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CircleService {
    private final CircleRequestRepository circleRequestRepository;
    private final AppUserRepository appUserRepository;
    private final CircleGroupRepository circleGroupRepository;
    private final CircleMembershipRepository circleMembershipRepository;
    private final CircleRequestMgr circleRequestMgr;
    private final QuestNewsService questNewsService;

    public List<CircleRequestResponseDTO> getMyCircles(AppUser currentUser) {
        return circleRequestRepository.findAcceptedByUserId(currentUser.getId())
                .stream()
                .map(circleRequestMgr::toDto)
                .toList();
    }

    public AdminCircleOverviewDTO getAdminOverview(AppUser currentUser) {
        validateAdmin(currentUser);

        List<CircleRequest> relations = circleRequestRepository.findAllDetailed();
        return AdminCircleOverviewDTO.builder()
                .circles(getAllCirclesForAdmin())
                .acceptedConnections(relations.stream()
                        .filter(relation -> relation.getAcceptedAt() != null && relation.getBlockedAt() == null)
                        .map(circleRequestMgr::toDto)
                        .toList())
                .pendingRequests(relations.stream()
                        .filter(relation -> relation.getAcceptedAt() == null && relation.getBlockedAt() == null)
                        .map(circleRequestMgr::toDto)
                        .toList())
                .blockedRelations(relations.stream()
                        .filter(relation -> relation.getBlockedAt() != null)
                        .map(circleRequestMgr::toDto)
                        .toList())
                .build();
    }

    public CircleOverviewDTO getOverview(AppUser currentUser) {
        return CircleOverviewDTO.builder()
                .circles(getCircles(currentUser))
                .connections(getConnections(currentUser))
                .incomingRequests(getIncomingRequests(currentUser))
                .outgoingRequests(getOutgoingRequests(currentUser))
                .inviteCandidates(getInviteCandidates(currentUser))
                .build();
    }

    public List<CircleGroupResponseDTO> getCircles(AppUser currentUser) {
        return circleGroupRepository.findAllDetailedByOwnerId(currentUser.getId()).stream()
                .map(this::toCircleDto)
                .toList();
    }

    public List<AdminCircleGroupResponseDTO> getAllCirclesForAdmin() {
        return circleGroupRepository.findAllDetailed().stream()
                .map(circle -> AdminCircleGroupResponseDTO.builder()
                        .id(circle.getId())
                        .name(circle.getName())
                        .ownerId(circle.getOwner().getId())
                        .ownerUsername(circle.getOwner().getUsername())
                        .memberCount(circle.getMemberships().size())
                        .members(circle.getMemberships().stream()
                                .map(CircleMembership::getMember)
                                .sorted(Comparator.comparing(AppUser::getUsername, String.CASE_INSENSITIVE_ORDER))
                                .map(member -> CircleMemberDTO.builder()
                                        .userId(member.getId())
                                        .username(member.getUsername())
                                        .profileDescription(member.getProfileDescription())
                                        .profileAvatarDataUrl(member.getProfileAvatarDataUrl())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    public List<CircleContactDTO> getConnections(AppUser currentUser) {
        Map<Long, List<CircleMembership>> membershipsByUserId = circleMembershipRepository.findByCircleOwnerId(currentUser.getId())
                .stream()
                .collect(Collectors.groupingBy(membership -> membership.getMember().getId()));

        return circleRequestRepository.findAcceptedByUserId(currentUser.getId()).stream()
                .map(relation -> toContact(currentUser, relation, membershipsByUserId))
                .toList();
    }

    private CircleContactDTO toContact(AppUser currentUser, CircleRequest relation, Map<Long, List<CircleMembership>> membershipsByUserId) {
        AppUser contact = relation.getRequester().getId().equals(currentUser.getId())
                ? relation.getRecipient()
                : relation.getRequester();
        List<CircleMembership> memberships = membershipsByUserId.getOrDefault(contact.getId(), List.of());

        return CircleContactDTO.builder()
                .relationId(relation.getId())
                .userId(contact.getId())
                .username(contact.getUsername())
                .profileDescription(contact.getProfileDescription())
                .profileAvatarDataUrl(contact.getProfileAvatarDataUrl())
                .circleIds(memberships.stream().map(membership -> membership.getCircle().getId()).toList())
                .circleNames(memberships.stream().map(membership -> membership.getCircle().getName()).toList())
                .build();
    }

    private CircleGroupResponseDTO toCircleDto(CircleGroup circle) {
        List<CircleMemberDTO> members = circle.getMemberships().stream()
                .map(CircleMembership::getMember)
                .sorted(Comparator.comparing(AppUser::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(member -> CircleMemberDTO.builder()
                        .userId(member.getId())
                        .username(member.getUsername())
                        .profileDescription(member.getProfileDescription())
                        .profileAvatarDataUrl(member.getProfileAvatarDataUrl())
                        .build())
                .toList();

        return CircleGroupResponseDTO.builder()
                .id(circle.getId())
                .name(circle.getName())
                .memberCount(members.size())
                .members(members)
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
        if (isAdmin(currentUser)) {
            circleRequestRepository.delete(requireRequest(requestId));
            return;
        }

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

    public boolean isCircleMember(Long circleId, Long memberUserId) {
        return circleMembershipRepository.existsByCircleIdAndMemberId(circleId, memberUserId);
    }

    public List<CircleGroup> getOwnedCirclesByIds(AppUser owner, List<Long> circleIds) {
        if (circleIds == null || circleIds.isEmpty()) {
            return List.of();
        }

        List<CircleGroup> circles = circleGroupRepository.findAllByOwnerIdAndIdIn(owner.getId(), circleIds);
        if (circles.size() != new LinkedHashSet<>(circleIds).size()) {
            throw ServiceErrors.badRequest("One or more selected circles are invalid");
        }

        return circles;
    }

    @Transactional
    public CircleGroupResponseDTO createCircle(CircleGroupRequestDTO dto, AppUser currentUser) {
        String normalizedName = normalizeCircleName(dto.getName());
        if (circleGroupRepository.existsByOwnerIdAndNameIgnoreCase(currentUser.getId(), normalizedName)) {
            throw ServiceErrors.conflict("You already have a circle with this name");
        }

        CircleGroup circle = new CircleGroup();
        circle.setOwner(currentUser);
        circle.setName(normalizedName);
        return toCircleDto(circleGroupRepository.save(circle));
    }

    @Transactional
    public CircleGroupResponseDTO updateCircle(Long circleId, CircleGroupRequestDTO dto, AppUser currentUser) {
        CircleGroup circle = requireCircleOwnedByCurrentUser(circleId, currentUser);
        String normalizedName = normalizeCircleName(dto.getName());

        if (!circle.getName().equalsIgnoreCase(normalizedName)
                && circleGroupRepository.existsByOwnerIdAndNameIgnoreCase(currentUser.getId(), normalizedName)) {
            throw ServiceErrors.conflict("You already have a circle with this name");
        }

        circle.setName(normalizedName);
        return toCircleDto(circleGroupRepository.save(circle));
    }

    @Transactional
    public void deleteCircle(Long circleId, AppUser currentUser) {
        CircleGroup circle = requireCircleOwnedByCurrentUser(circleId, currentUser);
        circleGroupRepository.delete(circle);
    }

    @Transactional
    public void deleteCircleAsAdmin(Long circleId, AppUser currentUser) {
        validateAdmin(currentUser);

        CircleGroup circle = circleGroupRepository.findById(circleId)
                .orElseThrow(() -> ServiceErrors.notFound("Circle not found with id " + circleId));
        circleGroupRepository.delete(circle);
    }

    @Transactional
    public CircleContactDTO updateConnectionCircles(Long userId, ConnectionCircleUpdateDTO dto, AppUser currentUser) {
        AppUser contact = requireAppUser(userId);
        if (!isCircleBetween(currentUser, contact)) {
            throw ServiceErrors.badRequest("You can only organize connected users into circles");
        }

        Set<Long> requestedCircleIds = dto.getCircleIds() == null ? Set.of() : new LinkedHashSet<>(dto.getCircleIds());
        List<CircleGroup> requestedCircles = requestedCircleIds.isEmpty()
                ? List.of()
                : circleGroupRepository.findAllByOwnerIdAndIdIn(currentUser.getId(), requestedCircleIds);

        if (requestedCircles.size() != requestedCircleIds.size()) {
            throw ServiceErrors.badRequest("One or more selected circles are invalid");
        }

        Map<Long, CircleGroup> requestedCircleById = requestedCircles.stream()
                .collect(Collectors.toMap(CircleGroup::getId, Function.identity()));

        List<CircleMembership> existingMemberships = circleMembershipRepository.findByMemberIdAndCircleOwnerId(contact.getId(), currentUser.getId());
        Map<Long, CircleMembership> existingByCircleId = existingMemberships.stream()
                .collect(Collectors.toMap(membership -> membership.getCircle().getId(), Function.identity()));

        for (CircleMembership membership : existingMemberships) {
            if (!requestedCircleIds.contains(membership.getCircle().getId())) {
                circleMembershipRepository.delete(membership);
            }
        }

        for (Long circleId : requestedCircleIds) {
            if (existingByCircleId.containsKey(circleId)) {
                continue;
            }

            CircleMembership membership = new CircleMembership();
            membership.setCircle(requestedCircleById.get(circleId));
            membership.setMember(contact);
            circleMembershipRepository.save(membership);
        }

        CircleRequest relation = findRelation(currentUser, contact)
                .orElseThrow(() -> ServiceErrors.notFound("Connection not found"));
        Map<Long, List<CircleMembership>> membershipsByUserId = circleMembershipRepository.findByMemberIdAndCircleOwnerId(contact.getId(), currentUser.getId())
                .stream()
                .collect(Collectors.groupingBy(membership -> membership.getMember().getId()));
        return toContact(currentUser, relation, membershipsByUserId);
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

    private CircleGroup requireCircleOwnedByCurrentUser(Long circleId, AppUser currentUser) {
        return circleGroupRepository.findByIdAndOwnerId(circleId, currentUser.getId())
                .orElseThrow(() -> ServiceErrors.notFound("Circle not found with id " + circleId));
    }

    private String normalizeCircleName(String name) {
        if (name == null) {
            throw ServiceErrors.badRequest("Circle name is required");
        }

        String normalized = name.trim();
        if (normalized.isBlank()) {
            throw ServiceErrors.badRequest("Circle name is required");
        }

        return normalized;
    }

    private void validateAdmin(AppUser currentUser) {
        if (!isAdmin(currentUser)) {
            throw ServiceErrors.forbidden("Admin access is required");
        }
    }

    private boolean isAdmin(AppUser currentUser) {
        return currentUser != null && currentUser.getRole() == AppUserRole.ADMIN;
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
