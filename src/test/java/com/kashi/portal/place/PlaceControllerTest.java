package com.kashi.portal.place;

import com.kashi.portal.auth.JwtService;
import com.kashi.portal.common.BadRequestException;
import com.kashi.portal.common.PageResponse;
import com.kashi.portal.common.ResourceNotFoundException;
import com.kashi.portal.place.dto.PlaceRequest;
import com.kashi.portal.place.dto.PlaceResponse;
import com.kashi.portal.support.TestUsers;
import com.kashi.portal.support.WebTestSetup;
import com.kashi.portal.user.Role;
import com.kashi.portal.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaceController.class)
@WebTestSetup
class PlaceControllerTest {

    private static final String VALID_PLACE = """
            {"name": "Kedar Ghat", "type": "GHAT", "shortDescription": "A striped-step ghat.",
             "imageUrl": "https://example.com/kedar.jpg", "latitude": 25.2999, "longitude": 83.0102}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private PlaceService placeService;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    void listingPlacesNeedsNoTokenAndPassesFiltersToTheService() throws Exception {
        given(placeService.getPlaces(any(), any(), anyInt(), anyInt(), anyString()))
                .willReturn(pageOf(samplePlace()));

        mockMvc.perform(get("/api/places")
                        .param("type", "GHAT")
                        .param("name", "assi")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Assi Ghat"))
                .andExpect(jsonPath("$.content[0].type").value("GHAT"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        verify(placeService).getPlaces(PlaceType.GHAT, "assi", 1, 5, "name,desc");
    }

    @Test
    void listingUsesDefaultsWhenNoParametersAreGiven() throws Exception {
        given(placeService.getPlaces(any(), any(), anyInt(), anyInt(), anyString())).willReturn(pageOf());

        mockMvc.perform(get("/api/places")).andExpect(status().isOk());

        verify(placeService).getPlaces(isNull(), isNull(), eq(0), eq(10), eq("name,asc"));
    }

    @Test
    void unknownPlaceTypeReturns400() throws Exception {
        mockMvc.perform(get("/api/places").param("type", "MUSEUM"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'type'"));

        verifyNoInteractions(placeService);
    }

    @Test
    void invalidSortFieldReturns400() throws Exception {
        given(placeService.getPlaces(any(), any(), anyInt(), anyInt(), anyString()))
                .willThrow(new BadRequestException("Cannot sort by 'password'. Allowed fields: createdAt, name, type"));

        mockMvc.perform(get("/api/places").param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot sort by 'password'. Allowed fields: createdAt, name, type"));
    }

    @Test
    void gettingAPlaceByIdReturnsIt() throws Exception {
        given(placeService.getPlace(1L)).willReturn(samplePlace());

        mockMvc.perform(get("/api/places/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Assi Ghat"));
    }

    @Test
    void gettingAMissingPlaceReturns404WithTheStandardErrorShape() throws Exception {
        given(placeService.getPlace(99L)).willThrow(new ResourceNotFoundException("Place not found with id 99"));

        mockMvc.perform(get("/api/places/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Place not found with id 99"))
                .andExpect(jsonPath("$.path").value("/api/places/99"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void anAdminCanCreateAPlace() throws Exception {
        given(placeService.createPlace(any(PlaceRequest.class))).willReturn(samplePlace());

        mockMvc.perform(post("/api/places")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor("admin@kashi.com", Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PLACE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Assi Ghat"));
    }

    @Test
    void creatingAPlaceWithInvalidDataReturns400WithFieldErrors() throws Exception {
        String invalid = """
                {"name": "  ", "shortDescription": "", "imageUrl": "not-a-url", "latitude": 123.5}
                """;

        mockMvc.perform(post("/api/places")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor("admin@kashi.com", Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").value("Name is required"))
                .andExpect(jsonPath("$.fieldErrors.type").value("Type is required (GHAT or TEMPLE)"))
                .andExpect(jsonPath("$.fieldErrors.shortDescription").value("Short description is required"))
                .andExpect(jsonPath("$.fieldErrors.imageUrl").value("Image URL must be a valid http or https URL"))
                .andExpect(jsonPath("$.fieldErrors.latitude").value("Latitude must be between -90 and 90"));

        verify(placeService, never()).createPlace(any(PlaceRequest.class));
    }

    @Test
    void updatingAMissingPlaceReturns404() throws Exception {
        given(placeService.updatePlace(eq(99L), any(PlaceRequest.class)))
                .willThrow(new ResourceNotFoundException("Place not found with id 99"));

        mockMvc.perform(put("/api/places/99")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor("admin@kashi.com", Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PLACE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Place not found with id 99"));
    }

    @Test
    void anAdminCanDeleteAPlace() throws Exception {
        mockMvc.perform(delete("/api/places/5")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor("admin@kashi.com", Role.ADMIN)))
                .andExpect(status().isNoContent());

        verify(placeService).deletePlace(5L);
    }

    @Test
    void aNormalUserCannotCreateUpdateOrDeletePlaces() throws Exception {
        String userToken = bearerFor("user@kashi.com", Role.USER);

        mockMvc.perform(post("/api/places").header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_PLACE))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/places/1").header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_PLACE))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/places/1").header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());

        verifyNoInteractions(placeService);
    }

    @Test
    void anonymousVisitorsCannotCreateUpdateOrDeletePlaces() throws Exception {
        mockMvc.perform(post("/api/places").contentType(MediaType.APPLICATION_JSON).content(VALID_PLACE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(delete("/api/places/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(placeService);
    }

    private String bearerFor(String email, Role role) {
        given(userRepository.findByEmail(email)).willReturn(Optional.of(TestUsers.withRole(email, role)));
        return "Bearer " + jwtService.generateToken(email);
    }

    private PlaceResponse samplePlace() {
        return new PlaceResponse(1L, "Assi Ghat", PlaceType.GHAT, "Southern ghat", "Long text",
                null, null, new BigDecimal("25.2884"), new BigDecimal("83.0063"),
                LocalDateTime.now(), LocalDateTime.now());
    }

    private PageResponse<PlaceResponse> pageOf(PlaceResponse... places) {
        return new PageResponse<>(List.of(places), 0, 10, places.length, places.length == 0 ? 0 : 1, true, true);
    }
}
