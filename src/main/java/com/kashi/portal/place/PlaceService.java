package com.kashi.portal.place;

import com.kashi.portal.common.PageResponse;
import com.kashi.portal.common.Paging;
import com.kashi.portal.common.ResourceNotFoundException;
import com.kashi.portal.place.dto.PlaceRequest;
import com.kashi.portal.place.dto.PlaceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private static final Set<String> SORT_FIELDS = Set.of("name", "type", "createdAt");

    private final PlaceRepository placeRepository;

    public PageResponse<PlaceResponse> getPlaces(PlaceType type, String name, int page, int size, String sort) {
        Pageable pageable = Paging.of(page, size, sort, SORT_FIELDS);
        String search = (name == null || name.isBlank()) ? null : name.trim();
        return PageResponse.from(placeRepository.search(type, search, pageable).map(PlaceResponse::from));
    }

    public PlaceResponse getPlace(Long id) {
        return PlaceResponse.from(findPlace(id));
    }

    @Transactional
    public PlaceResponse createPlace(PlaceRequest request) {
        Place place = new Place();
        copyFields(request, place);
        return PlaceResponse.from(placeRepository.save(place));
    }

    @Transactional
    public PlaceResponse updatePlace(Long id, PlaceRequest request) {
        Place place = findPlace(id);
        copyFields(request, place);
        return PlaceResponse.from(placeRepository.save(place));
    }

    @Transactional
    public void deletePlace(Long id) {
        Place place = findPlace(id);
        placeRepository.delete(place);
    }

    private Place findPlace(Long id) {
        return placeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Place not found with id " + id));
    }

    private void copyFields(PlaceRequest request, Place place) {
        place.setName(request.name());
        place.setType(request.type());
        place.setShortDescription(request.shortDescription());
        place.setDescription(request.description());
        place.setImageUrl(request.imageUrl());
        place.setSourceUrl(request.sourceUrl());
        place.setLatitude(request.latitude());
        place.setLongitude(request.longitude());

        if (request.bookingType() == null || request.pricePerPerson() == null) {
            throw new IllegalArgumentException("Booking type and price are required");
        }

        if (request.bookingType() == BookingType.FREE
                && request.pricePerPerson().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Free tours must have zero price");
        }

        if (request.bookingType() == BookingType.PAID
                && request.pricePerPerson().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Paid tours must have a price greater than zero");
        }

        place.setBookingAvailable(request.bookingAvailable());
        place.setBookingType(request.bookingType());
        place.setPricePerPerson(request.pricePerPerson());

    }
}
