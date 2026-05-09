package com.example.campusmarket.lostitem.controller;

import com.example.campusmarket.common.dto.ApiResponse;
import com.example.campusmarket.lostitem.dto.LostItemRequest;
import com.example.campusmarket.lostitem.dto.LostItemResponse;
import com.example.campusmarket.lostitem.service.LostItemService;
import com.google.firebase.auth.FirebaseToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lost-items")
@RequiredArgsConstructor
public class LostItemController {

    private final LostItemService lostItemService;

    @PostMapping
    public ResponseEntity<ApiResponse<LostItemResponse>> create(
        @RequestBody @Valid LostItemRequest request,
        Authentication auth
    ) {
        try {
            String uid = getUid(auth);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(lostItemService.create(request, uid)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LostItemResponse>>> findAll() {
        try {
            return ResponseEntity.ok(ApiResponse.success(lostItemService.findAll()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LostItemResponse>> findById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(lostItemService.findById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
        @PathVariable String id,
        Authentication auth
    ) {
        try {
            lostItemService.delete(id, getUid(auth));
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    private String getUid(Authentication auth) {
        return ((FirebaseToken) auth.getPrincipal()).getUid();
    }
}
