package com.example.campusmarket.lostitem.dto;

import java.util.List;

public record LostItemResponse(
    String id,
    String title,
    String description,
    String location,
    String lostDate,
    List<String> imageUrls,
    String userid,
    Long createdAt
) {}
