package com.technicalchallenge.service;

import com.technicalchallenge.dto.AdditionalInfoDTO;
import com.technicalchallenge.model.AdditionalInfo;
import com.technicalchallenge.repository.AdditionalInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.technicalchallenge.service.AdditionalInfoService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j  // Added this for logging
public class AdditionalInfoService {

    @Autowired
    private AdditionalInfoRepository additionalInfoRepository;

    @Autowired
    private ModelMapper modelMapper;

    public static final String SETTLEMENT_INSTRUCTIONS_KEY = "SETTLEMENT_INSTRUCTIONS";

    // Existing methods
    public List<AdditionalInfoDTO> getAdditionalInfoForEntity(String entityType, Long entityId) {
        List<AdditionalInfo> additionalInfoList = additionalInfoRepository.findActiveByEntityTypeAndEntityId(entityType, entityId);
        return additionalInfoList.stream()
                .map(info -> modelMapper.map(info, AdditionalInfoDTO.class))
                .collect(Collectors.toList());
    }

    public AdditionalInfoDTO addAdditionalInfo(AdditionalInfoDTO dto) {
        // Check if field already exists and deactivate old version
        AdditionalInfo existing = additionalInfoRepository.findActiveByEntityTypeAndEntityIdAndFieldName(
                dto.getEntityType(), dto.getEntityId(), dto.getFieldName());

        if (existing != null) {
            existing.setActive(false);
            existing.setDeactivatedDate(LocalDateTime.now());
            additionalInfoRepository.save(existing);
        }

        // Create new version
        AdditionalInfo newInfo = modelMapper.map(dto, AdditionalInfo.class);
        newInfo.setId(null); // Ensure new record
        newInfo.setActive(true);
        newInfo.setCreatedDate(LocalDateTime.now());
        newInfo.setLastModifiedDate(LocalDateTime.now());
        newInfo.setVersion(existing != null ? existing.getVersion() + 1 : 1);

        AdditionalInfo saved = additionalInfoRepository.save(newInfo);
        return modelMapper.map(saved, AdditionalInfoDTO.class);
    }

    public void removeAdditionalInfo(String entityType, Long entityId, String fieldName) {
        AdditionalInfo existing = additionalInfoRepository.findActiveByEntityTypeAndEntityIdAndFieldName(
                entityType, entityId, fieldName);

        if (existing != null) {
            existing.setActive(false);
            existing.setDeactivatedDate(LocalDateTime.now());
            additionalInfoRepository.save(existing);
        }
    }

    public AdditionalInfoDTO updateAdditionalInfo(AdditionalInfoDTO dto) {
        return addAdditionalInfo(dto); // Same logic as add - version control
    }

    // // New method to add/update settlement instructions for a trade
    // public AdditionalInfo updateSettlementInstructions(Long tradeId, String instructions) {
    //     String currentUser;
    //     try {
    //         currentUser = TradeService.getLoggedInUsername();
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     } // Get current user for audit fields

    //     // Check if there is an existing active settlement instructions entry for this trade
    //     AdditionalInfo existing = additionalInfoRepository.findActiveByEntityTypeAndEntityIdAndFieldName(
    //             "TRADE", tradeId, SETTLEMENT_INSTRUCTIONS_KEY);

    //     // Create new settlement instructions record
    //     AdditionalInfo newInfo = new AdditionalInfo();
    //     newInfo.setEntityType("TRADE");
    //     newInfo.setEntityId(tradeId);
    //     newInfo.setFieldName(SETTLEMENT_INSTRUCTIONS_KEY);
    //     newInfo.setFieldValue(instructions);
    //     newInfo.setFieldType("STRING");
    //     newInfo.setActive(true);
    //     newInfo.setCreatedDate(LocalDateTime.now());
    //     newInfo.setLastModifiedDate(LocalDateTime.now());
    //     newInfo.setVersion(existing != null ? existing.getVersion() + 1 : 1);

    //     // Set settlement-specific fields
    //     newInfo.setSettlementKey(SETTLEMENT_INSTRUCTIONS_KEY);
    //     newInfo.setSettlementValue(instructions);

    //     // Set audit fields
    //     newInfo.setCreatedBy(currentUser);
    //     newInfo.setCreatedDate(LocalDateTime.now());

    //     AdditionalInfo saved = additionalInfoRepository.save(newInfo);
    //     log.info("Settlement instructions saved/updated for tradeId {} by user {}", tradeId, currentUser);
    //     return saved;
    // }
    // }

    public AdditionalInfo saveSettlementInstructions(Long tradeId, String instructions) {
        String currentUser = getCurrentUsername();

        // Check for existing active settlement instructions
        AdditionalInfo existing = additionalInfoRepository.findActiveByEntityTypeAndEntityIdAndFieldName(
                "TRADE", tradeId, SETTLEMENT_INSTRUCTIONS_KEY);

        if (existing != null) {
            existing.setActive(false);
            existing.setDeactivatedDate(LocalDateTime.now());
            additionalInfoRepository.save(existing);
        }

        AdditionalInfo newInfo = new AdditionalInfo();
        newInfo.setEntityType("TRADE");
        newInfo.setEntityId(tradeId);
        newInfo.setFieldName(SETTLEMENT_INSTRUCTIONS_KEY);
        newInfo.setFieldValue(instructions);
        newInfo.setActive(true);
        newInfo.setCreatedDate(LocalDateTime.now());
        newInfo.setLastModifiedDate(LocalDateTime.now());
        newInfo.setVersion(existing != null ? existing.getVersion() + 1 : 1);


        AdditionalInfo saved = additionalInfoRepository.save(newInfo);
        log.info("Saved settlement instructions for trade ID {} version {} by user {}", tradeId, saved.getVersion(), currentUser);
        return saved;
    }

    // ===== GET CURRENT =====
    public Optional<String> getSettlementInstructions(Long tradeId) {
        return additionalInfoRepository
                .findByEntityTypeAndEntityIdAndFieldName("TRADE", tradeId, SETTLEMENT_INSTRUCTIONS_KEY)
                .map(AdditionalInfo::getFieldValue);
    }

    // ===== SEARCH =====
    public List<Long> findTradesBySettlementInstructions(String searchText) {
        return additionalInfoRepository.findEntityIdsByFieldNameAndValueContainingIgnoreCase(
                "TRADE", SETTLEMENT_INSTRUCTIONS_KEY, searchText);
    }

    // ===== HELPER =====
    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}