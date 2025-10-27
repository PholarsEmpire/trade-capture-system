package com.technicalchallenge.repository;

import com.technicalchallenge.model.AdditionalInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdditionalInfoRepository extends JpaRepository<AdditionalInfo, Long> {

    @Query("SELECT a FROM AdditionalInfo a WHERE a.entityType = :entityType AND a.entityId = :entityId AND a.active = true")
    List<AdditionalInfo> findActiveByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    @Query("SELECT a FROM AdditionalInfo a WHERE a.entityType = :entityType AND a.entityId = :entityId AND a.fieldName = :fieldName AND a.active = true")
    AdditionalInfo findActiveByEntityTypeAndEntityIdAndFieldName(@Param("entityType") String entityType, @Param("entityId") Long entityId, @Param("fieldName") String fieldName);



    // Find one record for a specific trade’s settlement instructions
    // Useful when retrieving settlement instructions for a trade for an update operation
    Optional<AdditionalInfo> findByEntityTypeAndEntityIdAndSettlementKey(String entityType, Long entityId, String settlementKey);

    // Search trades whose settlement instructions contain certain text (for operations search)
    // This supports partial text matching and it's case-insensitive
    @Query("""
        SELECT ai FROM AdditionalInfo ai
        WHERE ai.entityType = 'TRADE'
          AND LOWER(ai.settlementValue) LIKE LOWER(CONCAT('%', :searchText, '%'))
    """)
    List<AdditionalInfo> searchSettlementInstructions(String searchText);

    // Retrieve all settlement info records for a specific trade (useful for display or audit)
    List<AdditionalInfo> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<AdditionalInfo> findByEntityTypeAndEntityIdAndActiveTrue(String entityType, Long entityId);
}
