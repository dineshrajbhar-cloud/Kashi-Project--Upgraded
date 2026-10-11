package com.kashi.portal.place;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    /** Both filters are optional: pass null to ignore one. The name match is a case-insensitive "contains". */
    @Query("""
            select p from Place p
            where (:type is null or p.type = :type)
              and (:name is null or lower(p.name) like lower(concat('%', :name, '%')))
            """)
    Page<Place> search(@Param("type") PlaceType type, @Param("name") String name, Pageable pageable);
}
