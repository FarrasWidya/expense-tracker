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
        if (memberRepo.findByUserId(userId).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already in a Rumah");

        User user = getUser(userId);
        Rumah rumah = new Rumah();
        rumah.setName(name);
        rumah.setEmoji(emoji);
        rumah.setColor(color);
        rumah.setInviteToken(UUID.randomUUID());
        rumah.setCreatedAt(LocalDateTime.now());
        rumah.setAdmin(user);
        rumahRepo.save(rumah);

        RumahMember member = new RumahMember();
        member.setRumah(rumah);
        member.setUser(user);
        member.setJoinedAt(LocalDateTime.now());
        memberRepo.save(member);

        return toResponse(rumah);
    }

    public Optional<RumahResponse> getMyRumah(Long userId) {
        return memberRepo.findByUserId(userId)
                .map(m -> toResponse(m.getRumah()));
    }

    public PreviewResponse previewJoin(UUID token) {
        Rumah rumah = rumahRepo.findByInviteToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid invite link"));
        int count = memberRepo.countByRumah(rumah);
        return new PreviewResponse(rumah.getName(), rumah.getEmoji(), rumah.getColor(), count);
    }

    public RumahResponse joinRumah(Long userId, UUID token) {
        Rumah rumah = rumahRepo.findByInviteToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid invite link"));

        if (memberRepo.findByUserId(userId).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Kamu sudah punya Rumah. Keluar dulu sebelum gabung");

        if (memberRepo.countByRumah(rumah) >= 6)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rumah ini sudah penuh (6 anggota)");

        User user = getUser(userId);
        RumahMember member = new RumahMember();
        member.setRumah(rumah);
        member.setUser(user);
        member.setJoinedAt(LocalDateTime.now());
        memberRepo.save(member);

        return toResponse(rumah);
    }

    public void leaveRumah(Long userId) {
        RumahMember membership = memberRepo.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not in a Rumah"));

        if (membership.getRumah().getAdmin().getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admin tidak bisa meninggalkan Rumah. Hapus Rumah untuk keluar.");

        memberRepo.deleteByRumahAndUserId(membership.getRumah(), userId);
    }

    public void deleteRumah(Long userId, UUID rumahId) {
        Rumah rumah = rumahRepo.findById(rumahId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rumah not found"));

        if (!rumah.getAdmin().getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can delete Rumah");

        sharedExpRepo.deleteByRumah(rumah);
        memberRepo.deleteByRumah(rumah);
        rumahRepo.delete(rumah);
    }

    public SharedExpense addSharedExpense(Long userId, UUID rumahId, Double amount,
                                          String category, String note, LocalDate date) {
        Rumah rumah = rumahRepo.findById(rumahId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertMember(rumah, userId);

        User user = getUser(userId);
        SharedExpense exp = new SharedExpense();
        exp.setRumah(rumah);
        exp.setCreatedBy(user);
        exp.setAmount(amount);
        exp.setCategory(category);
        exp.setNote(note);
        exp.setDate(date != null ? date : LocalDate.now());
        exp.setCreatedAt(LocalDateTime.now());
        return sharedExpRepo.save(exp);
    }

    public void deleteSharedExpense(Long userId, UUID rumahId, UUID expId) {
        Rumah rumah = rumahRepo.findById(rumahId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertMember(rumah, userId);

        SharedExpense exp = sharedExpRepo.findById(expId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean isAdmin = rumah.getAdmin().getId().equals(userId);
        boolean isOwner = exp.getCreatedBy().getId().equals(userId);
        if (!isAdmin && !isOwner)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only delete own expenses");

        sharedExpRepo.delete(exp);
    }

    public Page<SharedExpense> getFeed(Long userId, UUID rumahId, Pageable pageable) {
        Rumah rumah = rumahRepo.findById(rumahId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertMember(rumah, userId);
        return sharedExpRepo.findByRumahOrderByCreatedAtDesc(rumah, pageable);
    }

    public List<ContributionMember> getContribution(Long userId, UUID rumahId) {
        throw new UnsupportedOperationException("TODO");
    }
}
