package com.faras.expense_tracker;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class RumahController {

    private final RumahService rumahService;

    public RumahController(RumahService rumahService) {
        this.rumahService = rumahService;
    }

    public record CreateRumahRequest(String name, String emoji, String color) {}
    public record SharedExpenseRequest(Double amount, String category, String note, String date) {}

    private Long userId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @PostMapping("/rumah")
    @ResponseStatus(HttpStatus.CREATED)
    public RumahService.RumahResponse createRumah(@RequestBody CreateRumahRequest req, Authentication auth) {
        return rumahService.createRumah(userId(auth), req.name(), req.emoji(), req.color());
    }

    @GetMapping("/rumah/me")
    public ResponseEntity<RumahService.RumahResponse> getMyRumah(Authentication auth) {
        return rumahService.getMyRumah(userId(auth))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok().build());
    }

    @GetMapping("/rumah/join/{token}")
    public RumahService.PreviewResponse previewJoin(@PathVariable UUID token) {
        return rumahService.previewJoin(token);
    }

    @PostMapping("/rumah/join/{token}")
    public RumahService.RumahResponse joinRumah(@PathVariable UUID token, Authentication auth) {
        return rumahService.joinRumah(userId(auth), token);
    }

    @DeleteMapping("/rumah/me/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveRumah(Authentication auth) {
        rumahService.leaveRumah(userId(auth));
    }

    @DeleteMapping("/rumah/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRumah(@PathVariable UUID id, Authentication auth) {
        rumahService.deleteRumah(userId(auth), id);
    }

    @GetMapping("/rumah/{id}/feed")
    public Page<SharedExpense> getFeed(@PathVariable UUID id,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       Authentication auth) {
        return rumahService.getFeed(userId(auth), id, PageRequest.of(page, size));
    }

    @PostMapping("/rumah/{id}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public SharedExpense addSharedExpense(@PathVariable UUID id,
                                          @RequestBody SharedExpenseRequest req,
                                          Authentication auth) {
        LocalDate date = req.date() != null ? LocalDate.parse(req.date()) : LocalDate.now();
        return rumahService.addSharedExpense(userId(auth), id, req.amount(), req.category(), req.note(), date);
    }

    @DeleteMapping("/rumah/{id}/expenses/{expId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSharedExpense(@PathVariable UUID id, @PathVariable UUID expId, Authentication auth) {
        rumahService.deleteSharedExpense(userId(auth), id, expId);
    }

    @GetMapping("/rumah/{id}/contribution")
    public List<RumahService.ContributionMember> getContribution(@PathVariable UUID id, Authentication auth) {
        return rumahService.getContribution(userId(auth), id);
    }
}
