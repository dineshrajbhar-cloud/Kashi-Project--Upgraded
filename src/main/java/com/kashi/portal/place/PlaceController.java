package com.kashi.portal.place;

import com.kashi.portal.common.PageResponse;
import com.kashi.portal.place.dto.PlaceRequest;
import com.kashi.portal.place.dto.PlaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
@Tag(name = "Places")
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping
    @Operation(summary = "List places",
            description = "Public. Optionally filter by type and search by name. The page size is capped at 50.")
    public PageResponse<PlaceResponse> getPlaces(
            @Parameter(description = "GHAT or TEMPLE")
            @RequestParam(name = "type", required = false) PlaceType type,
            @Parameter(description = "Part of the place name, case-insensitive")
            @RequestParam(name = "name", required = false) String name,
            @Parameter(description = "Page number, starting at 0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Items per page (1 to 50)")
            @RequestParam(name = "size", defaultValue = "10") int size,
            @Parameter(description = "Field and direction. Fields: name, type, createdAt", example = "name,asc")
            @RequestParam(name = "sort", defaultValue = "name,asc") String sort) {
        return placeService.getPlaces(type, name, page, size, sort);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one place by id")
    public PlaceResponse getPlace(@PathVariable("id") Long id) {
        return placeService.getPlace(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a place (ADMIN)")
    public PlaceResponse createPlace(@Valid @RequestBody PlaceRequest request) {
        return placeService.createPlace(request);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a place (ADMIN)")
    public PlaceResponse updatePlace(@PathVariable("id") Long id, @Valid @RequestBody PlaceRequest request) {
        return placeService.updatePlace(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a place (ADMIN)")
    public void deletePlace(@PathVariable("id") Long id) {
        placeService.deletePlace(id);
    }
}
