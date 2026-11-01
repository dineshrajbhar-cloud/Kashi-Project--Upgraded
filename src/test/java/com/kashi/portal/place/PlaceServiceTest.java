package com.kashi.portal.place;

import com.kashi.portal.common.BadRequestException;
import com.kashi.portal.common.PageResponse;
import com.kashi.portal.common.ResourceNotFoundException;
import com.kashi.portal.place.dto.PlaceRequest;
import com.kashi.portal.place.dto.PlaceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private PlaceService placeService;

    @Test
    void getPlacesPassesTypeNameAndSortToTheRepository() {
        Place place = place(1L, "Assi Ghat", PlaceType.GHAT);
        given(placeRepository.search(eq(PlaceType.GHAT), eq("assi"), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(place)));

        PageResponse<PlaceResponse> result = placeService.getPlaces(PlaceType.GHAT, "  assi ", 0, 10, "name,desc");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(placeRepository).search(eq(PlaceType.GHAT), eq("assi"), pageable.capture());
        assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("name").getDirection());
        assertEquals(1, result.content().size());
        assertEquals("Assi Ghat", result.content().get(0).name());
    }

    @Test
    void blankNameAndMissingTypeMeanNoFilter() {
        given(placeRepository.search(isNull(), isNull(), any(Pageable.class))).willReturn(Page.empty());

        PageResponse<PlaceResponse> result = placeService.getPlaces(null, "   ", 0, 10, "name,asc");

        assertTrue(result.content().isEmpty());
    }

    @Test
    void pageSizeIsCappedAtFifty() {
        given(placeRepository.search(isNull(), isNull(), any(Pageable.class))).willReturn(Page.empty());

        placeService.getPlaces(null, null, 0, 500, "name,asc");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(placeRepository).search(isNull(), isNull(), pageable.capture());
        assertEquals(50, pageable.getValue().getPageSize());
    }

    @Test
    void unknownSortFieldIsRejectedBeforeTouchingTheDatabase() {
        assertThrows(BadRequestException.class,
                () -> placeService.getPlaces(null, null, 0, 10, "description,asc"));

        verifyNoInteractions(placeRepository);
    }

    @Test
    void getPlaceReturnsTheMatchingPlace() {
        given(placeRepository.findById(1L)).willReturn(Optional.of(place(1L, "Dashashwamedh Ghat", PlaceType.GHAT)));

        PlaceResponse response = placeService.getPlace(1L);

        assertEquals("Dashashwamedh Ghat", response.name());
        assertEquals(PlaceType.GHAT, response.type());
    }

    @Test
    void getPlaceThrowsWhenThePlaceDoesNotExist() {
        given(placeRepository.findById(99L)).willReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> placeService.getPlace(99L));

        assertEquals("Place not found with id 99", ex.getMessage());
    }

    @Test
    void createPlaceSavesEveryFieldFromTheRequest() {
        PlaceRequest request = new PlaceRequest("Kedar Ghat", PlaceType.GHAT, "Short text", "Long text",
                "https://example.com/kedar.jpg", "https://example.com/source",
                new BigDecimal("25.2999000"), new BigDecimal("83.0102000"), true,
                BookingType.FREE,
                BigDecimal.ZERO);
        given(placeRepository.save(any(Place.class))).willAnswer(invocation -> invocation.getArgument(0));

        PlaceResponse response = placeService.createPlace(request);

        ArgumentCaptor<Place> saved = ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(saved.capture());
        Place place = saved.getValue();
        assertEquals("Kedar Ghat", place.getName());
        assertEquals(PlaceType.GHAT, place.getType());
        assertEquals("Short text", place.getShortDescription());
        assertEquals("Long text", place.getDescription());
        assertEquals("https://example.com/kedar.jpg", place.getImageUrl());
        assertEquals("https://example.com/source", place.getSourceUrl());
        assertEquals(new BigDecimal("25.2999000"), place.getLatitude());
        assertEquals("Kedar Ghat", response.name());
        assertTrue(place.isBookingAvailable());
        assertEquals(BookingType.FREE, place.getBookingType());
        assertEquals(BigDecimal.ZERO, place.getPricePerPerson());

    }

    @Test
    void updatePlaceChangesTheExistingPlace() {
        Place existing = place(5L, "Old name", PlaceType.GHAT);
        given(placeRepository.findById(5L)).willReturn(Optional.of(existing));
        given(placeRepository.save(existing)).willReturn(existing);
        PlaceRequest request = new PlaceRequest("New name", PlaceType.TEMPLE, "New short", null, null, null, null, null, false,
                BookingType.FREE,
                BigDecimal.ZERO);

        PlaceResponse response = placeService.updatePlace(5L, request);

        assertEquals("New name", response.name());
        assertEquals(PlaceType.TEMPLE, response.type());
        assertEquals("New short", existing.getShortDescription());
    }

    @Test
    void updatePlaceThrowsWhenThePlaceDoesNotExist() {
        given(placeRepository.findById(99L)).willReturn(Optional.empty());
        PlaceRequest request = new PlaceRequest("Name", PlaceType.GHAT, "Short", null, null, null, null, null, false,
                BookingType.FREE,
                BigDecimal.ZERO);

        assertThrows(ResourceNotFoundException.class, () -> placeService.updatePlace(99L, request));

        verify(placeRepository, never()).save(any(Place.class));
    }

    @Test
    void deletePlaceRemovesTheExistingPlace() {
        Place existing = place(5L, "Assi Ghat", PlaceType.GHAT);
        given(placeRepository.findById(5L)).willReturn(Optional.of(existing));

        placeService.deletePlace(5L);

        verify(placeRepository).delete(existing);
    }

    @Test
    void deletePlaceThrowsWhenThePlaceDoesNotExist() {
        given(placeRepository.findById(99L)).willReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> placeService.deletePlace(99L));

        verify(placeRepository, never()).delete(any(Place.class));
    }

    private Place place(Long id, String name, PlaceType type) {
        return Place.builder()
                .id(id)
                .name(name)
                .type(type)
                .shortDescription("Short description of " + name)
                .build();
    }
}

