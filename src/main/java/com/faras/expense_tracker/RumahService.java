package com.faras.expense_tracker;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class RumahService {

    private final RumahRepository rumahRepo;
    private final RumahMemberRepository memberRepo;
    private final SharedExpenseRepository sharedExpRepo;
    private final UserRepository userRepo;

    public RumahService(RumahRepository rumahRepo, RumahMemberRepository memberRepo,
                        SharedExpenseRepository sharedExpRepo, UserRepository userRepo) {
        this.rumahRepo = rumahRepo;
        this.memberRepo = memberRepo;
        this.sharedExpRepo = sharedExpRepo;
        this.userRepo = userRepo;
    }

    public record MemberInfo(Long userId, String name, String avatarData) {}
    public record RumahResponse(UUID id, String name, String emoji, String color,
                                UUID inviteToken, Long adminId, List<MemberInfo> members) {}
    public record PreviewResponse(String name, String emoji, String color, int memberCount) {}
    public record ContributionMember(Long userId, String name, String avatarData,
                                     double amount, int pct) {}

    private User getUser(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void assertMember(Rumah rumah, Long userId) {
        if (!memberRepo.existsByRumahAndUserId(rumah, userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member");
    }

    private RumahResponse toResponse(Rumah rumah) {
        List<MemberInfo> members = memberRepo.findByRumah(rumah).stream()
                .map(m -> new MemberInfo(m.getUser().getId(), m.getUser().getName(), m.getUser().getAvatarData()))
                .toList();
        return new RumahResponse(rumah.getId(), rumah.getName(), rumah.getEmoji(), rumah.getColor(),
                rumah.getInviteToken(), rumah.getAdmin().getId(), members);
    }

    public RumahResponse createRumah(Long userId, String name, String emoji, String color) {
        throw new UnsupportedOperationException("TODO");
    }

    public Optional<RumahResponse> getMyRumah(Long userId) {
        throw new UnsupportedOperationException("TODO");
    }

    public PreviewResponse previewJoin(UUID token) {
        throw new UnsupportedOperationException("TODO");
    }

    public RumahResponse joinRumah(Long userId, UUID token) {
        throw new UnsupportedOperationException("TODO");
    }

    public void leaveRumah(Long userId) {
        throw new UnsupportedOperationException("TODO");
    }

    public void deleteRumah(Long userId, UUID rumahId) {
        throw new UnsupportedOperationException("TODO");
    }

    public SharedExpense addSharedExpense(Long userId, UUID rumahId, Double amount,
                                          String category, String note, LocalDate date) {
        throw new UnsupportedOperationException("TODO");
    }

    public void deleteSharedExpense(Long userId, UUID rumahId, UUID expId) {
        throw new UnsupportedOperationException("TODO");
    }

    public Page<SharedExpense> getFeed(Long userId, UUID rumahId, Pageable pageable) {
        throw new UnsupportedOperationException("TODO");
    }

    public List<ContributionMember> getContribution(Long userId, UUID rumahId) {
        throw new UnsupportedOperationException("TODO");
    }
}
