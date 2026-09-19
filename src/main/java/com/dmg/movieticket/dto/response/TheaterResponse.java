package com.dmg.movieticket.dto.response;

public record TheaterResponse(
        Long id,
        String name,
        String address,
        String landmark,
        String postalCode,
        Long cityId,
        String cityName
) {
}
