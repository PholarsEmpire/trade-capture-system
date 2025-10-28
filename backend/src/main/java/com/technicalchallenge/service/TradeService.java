package com.technicalchallenge.service;

import com.technicalchallenge.dto.DailySummaryDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.dto.TradeSummaryDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.*;
import com.technicalchallenge.repository.*;
import com.technicalchallenge.specifications.TradeSpecifications;

import com.technicalchallenge.validation.ValidationResult;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;



import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
//import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@PreAuthorize("isAuthenticated()") // Ensures all methods require authentication by default, i.e user must be logged in. That means no unauthenticated user can reach this service.
public class TradeService {
    private static final Logger logger = LoggerFactory.getLogger(TradeService.class);

    @Autowired
    private TradeRepository tradeRepository;
    @Autowired
    private TradeLegRepository tradeLegRepository;
    @Autowired
    private TradeMapper tradeMapper;
    @Autowired
    private CashflowRepository cashflowRepository;
    @Autowired
    private TradeStatusRepository tradeStatusRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private CounterpartyRepository counterpartyRepository;
    @Autowired
    private ApplicationUserRepository applicationUserRepository;
    @Autowired
    private TradeTypeRepository tradeTypeRepository;
    @Autowired
    private TradeSubTypeRepository tradeSubTypeRepository;
    @Autowired
    private CurrencyRepository currencyRepository;
    @Autowired
    private LegTypeRepository legTypeRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private HolidayCalendarRepository holidayCalendarRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private BusinessDayConventionRepository businessDayConventionRepository;
    @Autowired
    private PayRecRepository payRecRepository;
    @Autowired
    private AdditionalInfoService additionalInfoService;
    @Autowired
    private AdditionalInfoRepository additionalInfoRepository;
    @Autowired
    private UserPrivilegeRepository userPrivilegeRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private PrivilegeRepository privilegeRepository;
   
   
   


    // NEW METHOD: Get logged-in username automatically for authenticated actions
    // This avoids passing username from controller, reducing risk of spoofing
    // It retrieves the username from Spring Security context
    // The visibility is package-private for testing purposes
    static String getLoggedInUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
    }


    public List<Trade> getAllTrades() {
        logger.info("Retrieving all trades");
        return tradeRepository.findAll();
    }

    public Optional<Trade> getTradeById(Long tradeId) {
        logger.debug("Retrieving trade by id: {}", tradeId);
        Optional<Trade> tradeOpt = tradeRepository.findByTradeIdAndActiveTrue(tradeId);
        return tradeOpt;

    }


    @Transactional
    @PreAuthorize("hasAnyRole('TRADER', 'TRADER_SALES', 'SALES', 'SUPERUSER', 'ADMIN')")
    public Trade createTrade(TradeDTO tradeDTO) {
        //validate user privileges before creating trade
        String loginId= getLoggedInUsername();
        if (!validateUserPrivileges(loginId, "BOOK_TRADE", tradeDTO)) {
            throw new AccessDeniedException("User does not have privilege to create or book a trade");
        }

        logger.info("Creating new trade with ID: {}", tradeDTO.getTradeId());

        // Generate trade ID if not provided
        if (tradeDTO.getTradeId() == null) {
            // Generate sequential trade ID starting from 10000
            Long generatedTradeId = generateNextTradeId();
            tradeDTO.setTradeId(generatedTradeId);
            logger.info("Generated trade ID: {}", generatedTradeId);
        }


        // Validate general trade creation rules
        validateTradeCreation(tradeDTO);


            // Validate trade-specific business rules
        ValidationResult validation = validateTradeBusinessRules(tradeDTO);
        if (!validation.isValid()) {
            throw new RuntimeException("Trade validation failed: " + String.join(", ", validation.getErrors()));
        }

        
         validateSettlementInstructions(tradeDTO.getSettlementInstructions());

        // Create trade entity
        Trade trade = mapDTOToEntity(tradeDTO);
        trade.setVersion(1);
        trade.setActive(true);
        trade.setCreatedDate(LocalDateTime.now());
        trade.setLastTouchTimestamp(LocalDateTime.now());

        // Set default trade status to NEW if not provided
        if (tradeDTO.getTradeStatus() == null) {
            tradeDTO.setTradeStatus("NEW");
        }


        // Populate reference data
        populateReferenceDataByName(trade, tradeDTO);

        // Ensure we have essential reference data
        validateReferenceData(trade);


        Trade savedTrade = tradeRepository.save(trade);

        // Create trade legs and cashflows
        createTradeLegsWithCashflows(tradeDTO, savedTrade);


        logger.info("Successfully created trade with ID: {}", savedTrade.getTradeId());
        return savedTrade;
    }

    // NEW METHOD: For controller compatibility
    @Transactional
    @PreAuthorize("hasAnyRole('TRADER', 'TRADER_SALES', 'SALES', 'SUPERUSER')")
    public Trade saveTrade(TradeDTO tradeDTO) {
         Trade trade = mapDTOToEntity(tradeDTO);
        logger.info("Saving trade with ID: {}", trade.getTradeId());
       
        // If this is an existing trade (has ID), handle as amendment
        if (trade.getId() != null) {
            return amendTrade(trade.getTradeId(), tradeDTO);
        } else {
            return createTrade(tradeDTO);
        }
    }

    // FIXED: Populate reference data by names from DTO
    public void populateReferenceDataByName(Trade trade, TradeDTO tradeDTO) {
        logger.debug("Populating reference data for trade");

        // Populate Book
        if (tradeDTO.getBookName() != null) {
            bookRepository.findByBookName(tradeDTO.getBookName())
                    .ifPresent(trade::setBook);
        } else if (tradeDTO.getBookId() != null) {
            bookRepository.findById(tradeDTO.getBookId())
                    .ifPresent(trade::setBook);
        }

        // Populate Counterparty
        if (tradeDTO.getCounterpartyName() != null) {
            counterpartyRepository.findByName(tradeDTO.getCounterpartyName())
                    .ifPresent(trade::setCounterparty);
        } else if (tradeDTO.getCounterpartyId() != null) {
            counterpartyRepository.findById(tradeDTO.getCounterpartyId())
                    .ifPresent(trade::setCounterparty);
        }

        // Populate TradeStatus
        if (tradeDTO.getTradeStatus() != null) {
            tradeStatusRepository.findByTradeStatus(tradeDTO.getTradeStatus())
                    .ifPresent(trade::setTradeStatus);
        } else if (tradeDTO.getTradeStatusId() != null) {
            tradeStatusRepository.findById(tradeDTO.getTradeStatusId())
                    .ifPresent(trade::setTradeStatus);
        }

        // Populate other reference data
        populateUserReferences(trade, tradeDTO);
        populateTradeTypeReferences(trade, tradeDTO);
    }

    private void populateUserReferences(Trade trade, TradeDTO tradeDTO) {
        // Handle trader user by name or ID with enhanced logging
        if (tradeDTO.getTraderUserName() != null) {
            logger.debug("Looking up trader user by name: {}", tradeDTO.getTraderUserName());
            String[] nameParts = tradeDTO.getTraderUserName().trim().split("\\s+");
            if (nameParts.length >= 1) {
                String firstName = nameParts[0];
                logger.debug("Searching for user with firstName: {}", firstName);
                Optional<ApplicationUser> userOpt = applicationUserRepository.findByFirstName(firstName);
                if (userOpt.isPresent()) {
                    trade.setTraderUser(userOpt.get());
                    logger.debug("Found trader user: {} {}", userOpt.get().getFirstName(), userOpt.get().getLastName());
                } else {
                    logger.warn("Trader user not found with firstName: {}", firstName);
                    // Try with loginId as fallback
                    Optional<ApplicationUser> byLoginId = applicationUserRepository.findByLoginId(tradeDTO.getTraderUserName().toLowerCase());
                    if (byLoginId.isPresent()) {
                        trade.setTraderUser(byLoginId.get());
                        logger.debug("Found trader user by loginId: {}", tradeDTO.getTraderUserName());
                    } else {
                        logger.warn("Trader user not found by loginId either: {}", tradeDTO.getTraderUserName());
                    }
                }
            }
        } else if (tradeDTO.getTraderUserId() != null) {
            applicationUserRepository.findById(tradeDTO.getTraderUserId())
                    .ifPresent(trade::setTraderUser);
        }

        // Handle inputter user by name or ID with enhanced logging
        if (tradeDTO.getInputterUserName() != null) {
            logger.debug("Looking up inputter user by name: {}", tradeDTO.getInputterUserName());
            String[] nameParts = tradeDTO.getInputterUserName().trim().split("\\s+");
            if (nameParts.length >= 1) {
                String firstName = nameParts[0];
                logger.debug("Searching for inputter with firstName: {}", firstName);
                Optional<ApplicationUser> userOpt = applicationUserRepository.findByFirstName(firstName);
                if (userOpt.isPresent()) {
                    trade.setTradeInputterUser(userOpt.get());
                    logger.debug("Found inputter user: {} {}", userOpt.get().getFirstName(), userOpt.get().getLastName());
                } else {
                    logger.warn("Inputter user not found with firstName: {}", firstName);
                    // Try with loginId as fallback
                    Optional<ApplicationUser> byLoginId = applicationUserRepository.findByLoginId(tradeDTO.getInputterUserName().toLowerCase());
                    if (byLoginId.isPresent()) {
                        trade.setTradeInputterUser(byLoginId.get());
                        logger.debug("Found inputter user by loginId: {}", tradeDTO.getInputterUserName());
                    } else {
                        logger.warn("Inputter user not found by loginId either: {}", tradeDTO.getInputterUserName());
                    }
                }
            }
        } else if (tradeDTO.getTradeInputterUserId() != null) {
            applicationUserRepository.findById(tradeDTO.getTradeInputterUserId())
                    .ifPresent(trade::setTradeInputterUser);
        }
    }

    private void populateTradeTypeReferences(Trade trade, TradeDTO tradeDTO) {
        if (tradeDTO.getTradeType() != null) {
            logger.debug("Looking up trade type: {}", tradeDTO.getTradeType());
            Optional<TradeType> tradeTypeOpt = tradeTypeRepository.findByTradeType(tradeDTO.getTradeType());
            if (tradeTypeOpt.isPresent()) {
                trade.setTradeType(tradeTypeOpt.get());
                logger.debug("Found trade type: {} with ID: {}", tradeTypeOpt.get().getTradeType(), tradeTypeOpt.get().getId());
            } else {
                logger.warn("Trade type not found: {}", tradeDTO.getTradeType());
            }
        } else if (tradeDTO.getTradeTypeId() != null) {
            tradeTypeRepository.findById(tradeDTO.getTradeTypeId())
                    .ifPresent(trade::setTradeType);
        }

        if (tradeDTO.getTradeSubType() != null) {
            Optional<TradeSubType> tradeSubTypeOpt = tradeSubTypeRepository.findByTradeSubType(tradeDTO.getTradeSubType());
            if (tradeSubTypeOpt.isPresent()) {
                trade.setTradeSubType(tradeSubTypeOpt.get());
            } else {
                List<TradeSubType> allSubTypes = tradeSubTypeRepository.findAll();
                for (TradeSubType subType : allSubTypes) {
                    if (subType.getTradeSubType().equalsIgnoreCase(tradeDTO.getTradeSubType())) {
                        trade.setTradeSubType(subType);
                        break;
                    }
                }
            }
        } else if (tradeDTO.getTradeSubTypeId() != null) {
            tradeSubTypeRepository.findById(tradeDTO.getTradeSubTypeId())
                    .ifPresent(trade::setTradeSubType);
        }
    }

    // NEW METHOD: Delete trade (mark as cancelled)
    @Transactional
    @PreAuthorize("hasAnyRole('TRADER', 'TRADER_SALES', 'SALES', 'SUPERUSER')")
    public void deleteTrade(Long tradeId) {
        String loginId= getLoggedInUsername();
        if(!validateUserPrivileges(loginId, "AMEND_TRADE", null)){
            throw new AccessDeniedException("User does not have privilege to delete trade");
        }
        logger.info("Deleting (cancelling) trade with ID: {}", tradeId);
        cancelTrade(tradeId);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TRADER', 'TRADER_SALES', 'SALES', 'SUPERUSER', 'MIDDLE_OFFICE', 'MO')")
    public Trade amendTrade(Long tradeId, TradeDTO tradeDTO) {
        //validate user privileges before amending trade
        String loginId= getLoggedInUsername();
        if(!validateUserPrivileges(loginId, "AMEND_TRADE", tradeDTO)){
            throw new AccessDeniedException("User does not have privilege to amend trade");
        }

        logger.info("Amending trade with ID: {}", tradeId);

        Optional<Trade> existingTradeOpt = getTradeById(tradeId);
        if (existingTradeOpt.isEmpty()) {
            throw new RuntimeException("Trade not found: " + tradeId);
        }

        Trade existingTrade = existingTradeOpt.get();

        tradeDTO.setTradeId(tradeId); // Ensure tradeId is set in DTO
        existingTrade.setTradeId(tradeId); // Ensure tradeId is set in entity

        // Validate settlement instructions
        validateSettlementInstructions(tradeDTO.getSettlementInstructions());

        // Deactivate existing trade
        existingTrade.setActive(false);
        existingTrade.setDeactivatedDate(LocalDateTime.now());
        tradeRepository.save(existingTrade);

        // Create new version
        Trade amendedTrade = mapDTOToEntity(tradeDTO);
        amendedTrade.setTradeId(tradeId);
        amendedTrade.setVersion(existingTrade.getVersion() + 1);
        amendedTrade.setActive(true);
        amendedTrade.setCreatedDate(LocalDateTime.now());
        amendedTrade.setLastTouchTimestamp(LocalDateTime.now());

        // Populate reference data
        populateReferenceDataByName(amendedTrade, tradeDTO);

        // Set status to AMENDED
        TradeStatus amendedStatus = tradeStatusRepository.findByTradeStatus("AMENDED")
                .orElseThrow(() -> new RuntimeException("AMENDED status not found"));
        amendedTrade.setTradeStatus(amendedStatus);

        Trade savedTrade = tradeRepository.save(amendedTrade);

        // Create new trade legs and cashflows
        createTradeLegsWithCashflows(tradeDTO, savedTrade);

        logger.info("Successfully amended trade with ID: {}", savedTrade.getTradeId());
        return savedTrade;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TRADER', 'TRADER_SALES', 'SALES', 'SUPERUSER')")
    public Trade terminateTrade(Long tradeId) {
        String loginId= getLoggedInUsername();
        if(!validateUserPrivileges(loginId, "AMEND_TRADE", null)){
            throw new AccessDeniedException("User does not have privilege to terminate trade");
        }
        logger.info("Terminating trade with ID: {}", tradeId);

        Optional<Trade> tradeOpt = getTradeById(tradeId);
        if (tradeOpt.isEmpty()) {
            throw new RuntimeException("Trade not found: " + tradeId);
        }

        Trade trade = tradeOpt.get();
        TradeStatus terminatedStatus = tradeStatusRepository.findByTradeStatus("TERMINATED")
                .orElseThrow(() -> new RuntimeException("TERMINATED status not found"));

        trade.setTradeStatus(terminatedStatus);
        trade.setLastTouchTimestamp(LocalDateTime.now());

        return tradeRepository.save(trade);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TRADER', 'TRADER_SALES', 'SALES', 'SUPERUSER')")
    public Trade cancelTrade(Long tradeId) {
        String loginId= getLoggedInUsername();
        if(!validateUserPrivileges(loginId, "AMEND_TRADE", null)){
            throw new AccessDeniedException("User does not have privilege to cancel trade");
        }
        logger.info("Cancelling trade with ID: {}", tradeId);

        Optional<Trade> tradeOpt = getTradeById(tradeId);
        if (tradeOpt.isEmpty()) {
            throw new RuntimeException("Trade not found: " + tradeId);
        }

        Trade trade = tradeOpt.get();
        TradeStatus cancelledStatus = tradeStatusRepository.findByTradeStatus("CANCELLED")
                .orElseThrow(() -> new RuntimeException("CANCELLED status not found"));

        trade.setTradeStatus(cancelledStatus);
        trade.setLastTouchTimestamp(LocalDateTime.now());

        return tradeRepository.save(trade);
    }

    private void validateTradeCreation(TradeDTO tradeDTO) {
        // Validate dates - Fixed to use consistent field names
        if (tradeDTO.getTradeStartDate() != null && tradeDTO.getTradeDate() != null) {
            if (tradeDTO.getTradeStartDate().isBefore(tradeDTO.getTradeDate())) {
                throw new RuntimeException("Start date cannot be before trade date");
            }
        }
        if (tradeDTO.getTradeMaturityDate() != null && tradeDTO.getTradeStartDate() != null) {
            if (tradeDTO.getTradeMaturityDate().isBefore(tradeDTO.getTradeStartDate())) {
                throw new RuntimeException("Maturity date cannot be before start date");
            }
        }

        // Validate trade has exactly 2 legs
        if (tradeDTO.getTradeLegs() == null || tradeDTO.getTradeLegs().size() != 2) {
            throw new RuntimeException("Trade must have exactly 2 legs");
        }
    }

    private Trade mapDTOToEntity(TradeDTO dto) {
        Trade trade = new Trade();
        trade.setTradeId(dto.getTradeId());
        trade.setTradeDate(dto.getTradeDate()); // Fixed field names
        trade.setTradeStartDate(dto.getTradeStartDate());
        trade.setTradeMaturityDate(dto.getTradeMaturityDate());
        trade.setTradeExecutionDate(dto.getTradeExecutionDate());
        trade.setUtiCode(dto.getUtiCode());
        trade.setValidityStartDate(dto.getValidityStartDate());
        trade.setLastTouchTimestamp(LocalDateTime.now());
        return trade;
    }

   

    private void createTradeLegsWithCashflows(TradeDTO tradeDTO, Trade savedTrade) {
        for (int i = 0; i < tradeDTO.getTradeLegs().size(); i++) {
            var legDTO = tradeDTO.getTradeLegs().get(i);

            TradeLeg tradeLeg = new TradeLeg();
            tradeLeg.setTrade(savedTrade);
            tradeLeg.setNotional(legDTO.getNotional());
            tradeLeg.setRate(legDTO.getRate());
            tradeLeg.setActive(true);
            tradeLeg.setCreatedDate(LocalDateTime.now());

            // Populate reference data for leg
            populateLegReferenceData(tradeLeg, legDTO);

            TradeLeg savedLeg = tradeLegRepository.save(tradeLeg);

            // Generate cashflows for this leg
            if (tradeDTO.getTradeStartDate() != null && tradeDTO.getTradeMaturityDate() != null) {
                generateCashflows(savedLeg, tradeDTO.getTradeStartDate(), tradeDTO.getTradeMaturityDate());
            }
        }
    }

    private void populateLegReferenceData(TradeLeg leg, TradeLegDTO legDTO) {
        // Populate currency by name or ID
        if (legDTO.getCurrency() != null) {
            currencyRepository.findByCurrency(legDTO.getCurrency())
                    .ifPresent(leg::setCurrency);
        } else if (legDTO.getCurrencyId() != null) {
            currencyRepository.findById(legDTO.getCurrencyId())
                    .ifPresent(leg::setCurrency);
        }

        // Populate leg type by name or ID
        if (legDTO.getLegType() != null) {
            legTypeRepository.findByType(legDTO.getLegType())
                    .ifPresent(leg::setLegRateType);
        } else if (legDTO.getLegTypeId() != null) {
            legTypeRepository.findById(legDTO.getLegTypeId())
                    .ifPresent(leg::setLegRateType);
        }

        // Populate index by name or ID
        if (legDTO.getIndexName() != null) {
            indexRepository.findByIndex(legDTO.getIndexName())
                    .ifPresent(leg::setIndex);
        } else if (legDTO.getIndexId() != null) {
            indexRepository.findById(legDTO.getIndexId())
                    .ifPresent(leg::setIndex);
        }

        // Populate holiday calendar by name or ID
        if (legDTO.getHolidayCalendar() != null) {
            holidayCalendarRepository.findByHolidayCalendar(legDTO.getHolidayCalendar())
                    .ifPresent(leg::setHolidayCalendar);
        } else if (legDTO.getHolidayCalendarId() != null) {
            holidayCalendarRepository.findById(legDTO.getHolidayCalendarId())
                    .ifPresent(leg::setHolidayCalendar);
        }

        // Populate schedule by name or ID
        if (legDTO.getCalculationPeriodSchedule() != null) {
            scheduleRepository.findBySchedule(legDTO.getCalculationPeriodSchedule())
                    .ifPresent(leg::setCalculationPeriodSchedule);
        } else if (legDTO.getScheduleId() != null) {
            scheduleRepository.findById(legDTO.getScheduleId())
                    .ifPresent(leg::setCalculationPeriodSchedule);
        }

        // Populate payment business day convention by name or ID
        if (legDTO.getPaymentBusinessDayConvention() != null) {
            businessDayConventionRepository.findByBdc(legDTO.getPaymentBusinessDayConvention())
                    .ifPresent(leg::setPaymentBusinessDayConvention);
        } else if (legDTO.getPaymentBdcId() != null) {
            businessDayConventionRepository.findById(legDTO.getPaymentBdcId())
                    .ifPresent(leg::setPaymentBusinessDayConvention);
        }

        // Populate fixing business day convention by name or ID
        if (legDTO.getFixingBusinessDayConvention() != null) {
            businessDayConventionRepository.findByBdc(legDTO.getFixingBusinessDayConvention())
                    .ifPresent(leg::setFixingBusinessDayConvention);
        } else if (legDTO.getFixingBdcId() != null) {
            businessDayConventionRepository.findById(legDTO.getFixingBdcId())
                    .ifPresent(leg::setFixingBusinessDayConvention);
        }

        // Populate pay/receive flag by name or ID
        if (legDTO.getPayReceiveFlag() != null) {
            payRecRepository.findByPayRec(legDTO.getPayReceiveFlag())
                    .ifPresent(leg::setPayReceiveFlag);
        } else if (legDTO.getPayRecId() != null) {
            payRecRepository.findById(legDTO.getPayRecId())
                    .ifPresent(leg::setPayReceiveFlag);
        }
    }

    /**
     * FIXED: Generate cashflows based on schedule and maturity date
     */
    private void generateCashflows(TradeLeg leg, LocalDate startDate, LocalDate maturityDate) {
        logger.info("Generating cashflows for leg {} from {} to {}", leg.getLegId(), startDate, maturityDate);

        // Use default schedule if not set
        String schedule = "3M"; // Default to quarterly
        if (leg.getCalculationPeriodSchedule() != null) {
            schedule = leg.getCalculationPeriodSchedule().getSchedule();
        }

        int monthsInterval = parseSchedule(schedule);
        List<LocalDate> paymentDates = calculatePaymentDates(startDate, maturityDate, monthsInterval);

        for (LocalDate paymentDate : paymentDates) {
            Cashflow cashflow = new Cashflow();
            cashflow.setTradeLeg(leg); // Fixed field name
            cashflow.setValueDate(paymentDate);
            cashflow.setRate(leg.getRate());

            // Calculate value based on leg type
            BigDecimal cashflowValue = calculateCashflowValue(leg, monthsInterval);
            cashflow.setPaymentValue(cashflowValue);

            cashflow.setPayRec(leg.getPayReceiveFlag());
            cashflow.setPaymentBusinessDayConvention(leg.getPaymentBusinessDayConvention());
            cashflow.setCreatedDate(LocalDateTime.now());
            cashflow.setActive(true);

            cashflowRepository.save(cashflow);
        }

        logger.info("Generated {} cashflows for leg {}", paymentDates.size(), leg.getLegId());
    }

    private int parseSchedule(String schedule) {
        if (schedule == null || schedule.trim().isEmpty()) {
            return 3; // Default to quarterly
        }

        schedule = schedule.trim();

        // Handle common schedule names
        switch (schedule.toLowerCase()) {
            case "monthly":
                return 1;
            case "quarterly":
                return 3;
            case "semi-annually":
            case "semiannually":
            case "half-yearly":
                return 6;
            case "annually":
            case "yearly":
                return 12;
            default:
                // Parse "1M", "3M", "12M" format
                if (schedule.endsWith("M") || schedule.endsWith("m")) {
                    try {
                        return Integer.parseInt(schedule.substring(0, schedule.length() - 1));
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Invalid schedule format: " + schedule);
                    }
                }
                throw new RuntimeException("Invalid schedule format: " + schedule + ". Supported formats: Monthly, Quarterly, Semi-annually, Annually, or 1M, 3M, 6M, 12M");
        }
    }

    private List<LocalDate> calculatePaymentDates(LocalDate startDate, LocalDate maturityDate, int monthsInterval) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = startDate.plusMonths(monthsInterval);

        while (!currentDate.isAfter(maturityDate)) {
            dates.add(currentDate);
            currentDate = currentDate.plusMonths(monthsInterval);
        }

        return dates;
    }

    // FIXED: Calculate cashflow value based on leg type
    // NOTE: I changed the visibility to package-private from private for testing purposes
    // This still maintains encapsulation within the service package and not exposed publicly, so external clients cannot access it.
    BigDecimal calculateCashflowValue(TradeLeg leg, int monthsInterval) {
        if (leg.getLegRateType() == null || leg.getNotional() == null) {
            return BigDecimal.ZERO;
        }

        String legType = leg.getLegRateType().getType();

        if ("Fixed".equals(legType)) {
            BigDecimal notional = leg.getNotional();

            // Bug fix 1: convert the percentage rate to decimal
            BigDecimal ratePercentage = BigDecimal.valueOf(leg.getRate());
            BigDecimal rate = ratePercentage.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP); //scale of 6 for precision. Scale means number of digits after decimal point.


            BigDecimal months = BigDecimal.valueOf(monthsInterval); 


            // Bug fix 2: Use proper decimal rate in calculation
            return notional.multiply(rate).multiply(months).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        } else if ("Floating".equals(legType)) {
            // For floating legs, there is no rate fixed in advance, so we return zero here. In real scenarios, this would be based on index rates at fixing dates.
            return BigDecimal.ZERO;
        }

        return BigDecimal.ZERO;
    }

    private void validateReferenceData(Trade trade) {
        // Validate essential reference data is populated
        if (trade.getBook() == null) {
            throw new RuntimeException("Book not found or not set");
        }
        if (trade.getCounterparty() == null) {
            throw new RuntimeException("Counterparty not found or not set");
        }
        if (trade.getTradeStatus() == null) {
            throw new RuntimeException("Trade status not found or not set");
        }

        logger.debug("Reference data validation passed for trade");
    }

    // NEW METHOD: Generate the next trade ID (sequential)
    private Long generateNextTradeId() {
        // For simplicity, using a static variable. In real scenario, this should be atomic and thread-safe.
        return 10000L + tradeRepository.count();
    }





    //FOLA ADDED: NEW METHOD FOR DYNAMIC SEARCH AND FILTERING USING SPECIFICATIONS
    public List<Trade> searchTrades(String counterparty,
                                String book,
                                Long trader, // trader is Long type representing trader user ID since its a unique identifier
                                String status,
                                LocalDate from,
                                LocalDate to) {

        Specification<Trade> spec = Specification
                .where(TradeSpecifications.hasCounterparty(counterparty))
                .and(TradeSpecifications.hasBook(book))
                .and(TradeSpecifications.hasTrader(trader))
                .and(TradeSpecifications.hasStatus(status))
                .and(TradeSpecifications.dateBetween(from, to));

        return tradeRepository.findAll(spec); // Fetch all trades matching the specification
    }


    // FOLA ADDED: NEW METHOD FOR PAGINATED SEARCH WITH SORTING
    public Page<Trade> searchTrades(String counterparty,
                                    String book,
                                    Long trader,
                                    String status,
                                    LocalDate from,
                                    LocalDate to,
                                    int page, // zero-based page index. This specifies which page of results to retrieve.
                                    int size, // page size i.e number of records per page
                                    String sortBy, // field to sort by
                                    String direction) { // sort direction (ASC or DESC)

        Specification<Trade> spec = Specification
                .where(TradeSpecifications.hasCounterparty(counterparty))
                .and(TradeSpecifications.hasBook(book))
                .and(TradeSpecifications.hasTrader(trader))
                .and(TradeSpecifications.hasStatus(status))
                .and(TradeSpecifications.dateBetween(from, to));

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return tradeRepository.findAll(spec, pageable);
    }


    // FOLA ADDED: NEW METHOD FOR RSQL SEARCH
    public Page<Trade> searchByRsql(String query, Pageable pageable) {
    // very small parser: split ANDs first
        Specification<Trade> spec = Specification.where(null);
        for (String andPart : query.split(";")) {
            Specification<Trade> orSpec = Specification.where(null);
            for (String orPart : andPart.split(",")) {
                orSpec = orSpec == null ? parseToken(orPart) : orSpec.or(parseToken(orPart));
            }
            spec = spec == null ? orSpec : spec.and(orSpec);
        }
        return tradeRepository.findAll(spec == null ? Specification.where(null) : spec, pageable);
    }


    // Simple RSQL token parser for basic ==,=ge=,=le= operations meaning equal, greater-equal, less-equal
    // This is a very basic parser and may not cover all edge cases
    // It assumes that the input is well-formed and does not perform extensive validation
    // Examples of supported tokens:
    // counterparty.name==BigBank
    // tradeStatus.tradeStatus==NEW
    // tradeDate=ge=2025-01-01

    private Specification<Trade> parseToken(String token) {
        token = token.trim();
        if (token.contains("=ge=")) {
            String[] parts = token.split("=ge=");
            if ("tradeDate".equals(parts[0])) {
                return TradeSpecifications.dateBetween(LocalDate.parse(parts[1]), null);
            }
        } else if (token.contains("=le=")) {
            String[] parts = token.split("=le=");
            if ("tradeDate".equals(parts[0])) {
                return TradeSpecifications.dateBetween(null, LocalDate.parse(parts[1]));
            }
        } else if (token.contains("==")) {
            String[] parts = token.split("==");
            String field = parts[0];
            String value = parts[1];
            return switch (field) {
                case "counterparty.name" -> TradeSpecifications.hasCounterparty(value);
                case "book.bookName", "book.name" -> TradeSpecifications.hasBook(value);
                case "tradeStatus.tradeStatus" -> TradeSpecifications.hasStatus(value);
                default -> (root, q, cb) -> cb.conjunction(); // ignore unknowns
            };
        }
        return (root, q, cb) -> cb.conjunction();
    }



    // FOLA ADDED: Validation method for dates validation, trade legs validation, and entity existence checks
    public ValidationResult validateTradeBusinessRules(TradeDTO tradeDTO) {
        ValidationResult result = new ValidationResult();

        // ✅ Date Validation
        if (tradeDTO.getTradeDate() == null) {
            result.addError("Trade date is required");
        } else {
            if (tradeDTO.getTradeStartDate() != null && tradeDTO.getTradeStartDate().isBefore(tradeDTO.getTradeDate())) {
                result.addError("Start date cannot be before trade date");
            }
            if (tradeDTO.getTradeMaturityDate() != null && ((tradeDTO.getTradeMaturityDate().isBefore(tradeDTO.getTradeStartDate())) || (tradeDTO.getTradeMaturityDate().isBefore(tradeDTO.getTradeDate())))) {
                result.addError("Maturity date cannot be before start date or trade date");
            }
            if (tradeDTO.getTradeDate().isBefore(LocalDate.now().minusDays(30))) {
                result.addError("Trade date cannot be more than 30 days in the past"); // A trade date too far in the past is considered stale.
            }
        }

        // ✅ Leg Consistency
        if (tradeDTO.getTradeLegs() == null || tradeDTO.getTradeLegs().size() != 2) {
            result.addError("Trade must have exactly 2 legs");
        } else {
            ValidationResult legResult = validateTradeLegConsistency(tradeDTO.getTradeLegs());
            if (!legResult.isValid()) result.getErrors().addAll(legResult.getErrors());
        }

        // ✅ Entity Existence Checks
        if (tradeDTO.getBookName() == null) result.addError("Book is required");
        if (tradeDTO.getCounterpartyName() == null) result.addError("Counterparty is required");

        return result;
     }


    // FOLA ADDED: Validation method for trade legs consistency
    public ValidationResult validateTradeLegConsistency(List<TradeLegDTO> legs) {
        ValidationResult result = new ValidationResult();

        if (legs.size() != 2) {
            result.addError("Trade must have exactly 2 legs");
            return result;
        }

        TradeLegDTO leg1 = legs.get(0);
        TradeLegDTO leg2 = legs.get(1);

        // Same maturity date 
        if (leg1.getCalculationPeriodSchedule() != null && leg2.getCalculationPeriodSchedule() != null &&
            !leg1.getCalculationPeriodSchedule().equals(leg2.getCalculationPeriodSchedule())) {
            result.addError("Both legs must have identical maturity dates");
        }

        // ✅ Opposite pay/receive
        // If one leg is "Pay" the other must be "Receive"
        if (leg1.getPayReceiveFlag() != null && leg2.getPayReceiveFlag() != null &&
            leg1.getPayReceiveFlag().equalsIgnoreCase(leg2.getPayReceiveFlag())) {
            result.addError("Legs must have opposite pay/receive flags");
        }

        // ✅ Floating leg must have an index. An index is required for floating legs to determine the reference rate.
        if ("Floating".equalsIgnoreCase(leg1.getLegType()) && leg1.getIndexName() == null)
            result.addError("Floating leg must have an index specified");
        if ("Floating".equalsIgnoreCase(leg2.getLegType()) && leg2.getIndexName() == null)
            result.addError("Floating leg must have an index specified");

        // ✅ Fixed leg must have a rate. A rate is required for fixed legs to determine the payment amount.
        if ("Fixed".equalsIgnoreCase(leg1.getLegType()) && leg1.getRate() == 0.0)
            result.addError("Fixed leg must have a valid rate");
        if ("Fixed".equalsIgnoreCase(leg2.getLegType()) && leg2.getRate() == 0.0)
            result.addError("Fixed leg must have a valid rate");

        return result;
    }



    // FOLA ADDED: New method for user privileges validation based on the different user roles and the type of operations they can perform
    // Roles: TRADER, SALES, MIDDLE_OFFICE, SUPPORT, ADMIN, SUPERUSER
    // Operations: CREATE, AMEND, TERMINATE, CANCEL, DELETE, VIEW
    // This method checks if the logged-in user with a given role can perform a specific operation on a trade e.g a Support user should not be able to CREATE a trade
    // So instead of hardcoding the role, we fetch the user from the application user database table using their loginId and get their role dynamically from the user profile table
    // Then we check if the user's role allows them to perform the requested operation based on predefined rules on the user_privilege table
    // If the user does not have the required privileges, we log the unauthorized attempt and return false
    // If the user has the required privileges, we return true allowing the operation to proceed

    //LOGIC:Takes a user’s loginId and an actionName (e.g., "READ_TRADE")
    //Confirms that the user’s profile and privileges allow that action.

    public boolean validateUserPrivileges(String userId, String operation, TradeDTO tradeDTO) {
        if (userId == null || operation == null) {
            logger.warn("LoginId or privilege is null");
            return false;
        }

        // Normalize operation name
        operation = operation.toUpperCase();

        // Retrieve user record
        var user = applicationUserRepository.findByLoginId(userId).orElse(null);
        if (user == null || !user.isActive()) {
            logger.warn("User not found or inactive: {}", userId);
            return false;
        }

        // Retrieve user profile (role)
        var userProfile = userProfileRepository.findById(user.getUserProfile().getId()).orElse(null);
        if (userProfile == null) {
            logger.warn("User {} does not have a profile assigned", userId);
            return false;
        }

        String userType = userProfile.getUserType().toUpperCase();

        // SUPERUSER: always allowed to perform any operation
        if (userType.equals("SUPERUSER")) {
            logger.info("SUPERUSER detected - full access granted for {}", userId);
            return true;
        }

        // Get all privileges assigned to this user from user_privilege
        List<UserPrivilege> userPrivileges = userPrivilegeRepository.findByUserId(user.getId());
        if (userPrivileges == null || userPrivileges.isEmpty()) {
            logger.warn("User {} has no privileges assigned", userId);
            return false;
        }

        // Extract privilege IDs
        List<Long> privilegeIds = userPrivileges.stream()
                .map(UserPrivilege::getPrivilegeId)
                .toList();

        // Retrieve privilege names from the privilege table
        List<String> privilegeNames = privilegeRepository.findAllById(privilegeIds).stream()
                .map(Privilege::getName)
                .map(String::toUpperCase)
                .toList();

        // Check if operation exists in user's privileges
        boolean hasPrivilege = privilegeNames.contains(operation);

        logger.info("User {} of type {} requesting operation {} -> {}", userId, userType, operation, hasPrivilege);
        return hasPrivilege;
    }



    // FOLA ADDED: New method to get trades analytics by trader ID
    public List<Trade> getTradesByTrader() {
        // Get logged-in user's username
        String username = getLoggedInUsername();

        // If no user is logged in, send a warning and return empty list
        if (username == null) {
            logger.warn("No authenticated user found");
            return new ArrayList<>();
        }

        return tradeRepository.findAll().stream()
                    .filter(trade -> trade.getTraderUser() != null &&
                            trade.getTraderUser().getLoginId().equals(username))
                    .collect(Collectors.toList());
    }

    // FOLA ADDED: New method to get trades summary/ analytics by book ID
    public List<Trade> getTradesByBook(Long bookId) {
        return tradeRepository.findAll().stream()
                .filter(trade -> trade.getBook() != null && trade.getBook().getId().equals(bookId))
                .collect(Collectors.toList());
    }

    // FOLA ADDED: New method to get overall trade summary analytics
    public TradeSummaryDTO getTradeSummary() {
        List<Trade> trades = tradeRepository.findAll();

        // Total number of trades
        long totalTrades = trades.size();

        // Calculate trades by status
        var tradesByStatus = trades.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTradeStatus() != null ? t.getTradeStatus().getTradeStatus() : "UNKNOWN",
                        Collectors.counting()
                ));

        
       
        // Calculate trades by currency (count each trade for every currency it uses)
        //Trades don't have a direct currency field, so we derive it from trade legs
        var tradesByCurrency = trades.stream()
                .filter(t -> !t.getTradeLegs().isEmpty())
                .flatMap(t -> t.getTradeLegs().stream()
                        .map(leg -> leg.getCurrency() != null ? 
                                leg.getCurrency().getCurrency() : "UNKNOWN")
                        .distinct() // Remove duplicates within the same trade
                        .map(currency -> new AbstractMap.SimpleEntry<>(currency, 1L)))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.summingLong(Map.Entry::getValue)
                ));


        // Calculate notional by currency
        var notionalByCurrency = trades.stream()
                .flatMap(t -> t.getTradeLegs().stream())
                .collect(Collectors.groupingBy(
                        leg -> leg.getCurrency() != null ? leg.getCurrency().getCurrency() : "UNKNOWN",
                        Collectors.reducing(BigDecimal.ZERO, TradeLeg::getNotional, BigDecimal::add)
                ));

        // Calculate notional by trade type
        //This will filter out trades without a trade type to avoid null pointer exceptions
        // group by trade type and sum notionals across all legs of trades for that type
       var notionalByType = trades.stream()
        .filter(t -> t.getTradeType() != null)
        .collect(Collectors.toMap(
                t -> t.getTradeType().getTradeType(),
                t -> t.getTradeLegs().stream()
                      .map(TradeLeg::getNotional)
                      .reduce(BigDecimal.ZERO, BigDecimal::add),
                BigDecimal::add // Merge function for duplicate keys
        ));


        // Calculate notional by counterparty
        //This will filter out trades without a counterparty to avoid null pointer exceptions
        // group by counterparty name and sum notionals across all legs of trades for that counterparty
       var notionalByCounterparty = trades.stream()
        .filter(t -> t.getCounterparty() != null)
        .collect(Collectors.toMap(
                t -> t.getCounterparty().getName(),
                t -> t.getTradeLegs().stream()
                      .map(TradeLeg::getNotional)
                      .reduce(BigDecimal.ZERO, BigDecimal::add),
                BigDecimal::add // Merge function for duplicate keys
        ));


        var tradesByCounterparty = trades.stream()
                .filter(t -> t.getCounterparty() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCounterparty().getName(),
                        Collectors.counting()
                ));

        var tradesByType = trades.stream()
                .filter(t -> t.getTradeType() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getTradeType().getTradeType(),
                        Collectors.counting()
                ));

        
        var tradesBySubType = trades.stream()
                .filter(t -> t.getTradeSubType() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getTradeSubType().getTradeSubType(),
                        Collectors.counting()
                ));



        // Risk exposure calculation (simplified as total notional across all trades)
        // Calculating risks based on net and gross exposure by counterparty, currency, and book
        // Net exposure = sum of all trade values (positive and negative)
        // Gross exposure = sum of absolute values (no netting off)
        //PAY is negative (just like an expense; money going out), RECEIVE is positive (like income, money coming in)
        // Risk calculations helps with limit monitoring and credit risk assessment
        var netExposureByCounterparty = calculateNetExposureByCounterparty(trades);
        var grossExposureByCounterparty = calculateGrossExposureByCounterparty(trades);
        var netExposureByCurrency = calculateNetExposureByCurrency(trades);
        var grossExposureByCurrency = calculateGrossExposureByCurrency(trades);
        var netExposureByBook = calculateNetExposureByBook(trades);
        var grossExposureByBook = calculateGrossExposureByBook(trades);
        
        BigDecimal totalNetExposure = netExposureByCounterparty.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGrossExposure = grossExposureByCounterparty.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        //return new TradeSummaryDTO(totalTrades, tradesByStatus, tradesByCurrency, notionalByCurrency, notionalByType, notionalByCounterparty, tradesByCounterparty, tradesByType, tradesBySubType, risk);
        return TradeSummaryDTO.builder()
                .totalTrades(totalTrades)
                .tradesByStatus(tradesByStatus)
                .tradesByCurrency(tradesByCurrency)
                .notionalByCurrency(notionalByCurrency)
                .notionalByType(notionalByType)
                .notionalByCounterparty(notionalByCounterparty)
                .tradesByCounterparty(tradesByCounterparty)
                .tradesByType(tradesByType)
                .tradesBySubType(tradesBySubType)
                .netExposureByCounterparty(netExposureByCounterparty)
                .grossExposureByCounterparty(grossExposureByCounterparty)
                .netExposureByCurrency(netExposureByCurrency)
                .grossExposureByCurrency(grossExposureByCurrency)
                .netExposureByBook(netExposureByBook)
                .grossExposureByBook(grossExposureByBook)
                .totalNetExposure(totalNetExposure)
                .totalGrossExposure(totalGrossExposure)
                .build();
    }


    // Net exposure = sum of all trade values (positive and negative)
    private Map<String, BigDecimal> calculateNetExposureByCounterparty(List<Trade> trades) {
        return trades.stream()
                .filter(t -> t.getCounterparty() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCounterparty().getName(),
                        Collectors.reducing(BigDecimal.ZERO,
                                this::calculateTradeNetValue,
                                BigDecimal::add)
                ));
    }

    // Gross exposure = sum of absolute values
    private Map<String, BigDecimal> calculateGrossExposureByCounterparty(List<Trade> trades) {
        return trades.stream()
                .filter(t -> t.getCounterparty() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCounterparty().getName(),
                        Collectors.reducing(BigDecimal.ZERO,
                                t -> calculateTradeNetValue(t).abs(),
                                BigDecimal::add)
                ));
    }

    private Map<String, BigDecimal> calculateNetExposureByCurrency(List<Trade> trades) {
        return trades.stream()
                .flatMap(t -> t.getTradeLegs().stream())
                .filter(leg -> leg.getCurrency() != null)
                .collect(Collectors.groupingBy(
                        leg -> leg.getCurrency().getCurrency(),
                        Collectors.reducing(BigDecimal.ZERO,
                                this::calculateLegValue,
                                BigDecimal::add)
                ));
    }

    private Map<String, BigDecimal> calculateGrossExposureByCurrency(List<Trade> trades) {
        return trades.stream()
                .flatMap(t -> t.getTradeLegs().stream())
                .filter(leg -> leg.getCurrency() != null)
                .collect(Collectors.groupingBy(
                        leg -> leg.getCurrency().getCurrency(),
                        Collectors.reducing(BigDecimal.ZERO,
                                leg -> calculateLegValue(leg).abs(),
                                BigDecimal::add)
                ));
    }

    private Map<String, BigDecimal> calculateNetExposureByBook(List<Trade> trades) {
        return trades.stream()
                .filter(t -> t.getBook() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getBook().getBookName(),
                        Collectors.reducing(BigDecimal.ZERO,
                                this::calculateTradeNetValue,
                                BigDecimal::add)
                ));
    }

    private Map<String, BigDecimal> calculateGrossExposureByBook(List<Trade> trades) {
        return trades.stream()
                .filter(t -> t.getBook() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getBook().getBookName(),
                        Collectors.reducing(BigDecimal.ZERO,
                                t -> calculateTradeNetValue(t).abs(),
                                BigDecimal::add)
                ));
    }

    // Helper method to calculate trade's net value (sum of all legs with proper signs)
    private BigDecimal calculateTradeNetValue(Trade trade) {
        return trade.getTradeLegs().stream()
                .map(this::calculateLegValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    // Helper method to calculate individual leg value (considering buy/sell direction)
    private BigDecimal calculateLegValue(TradeLeg leg) {
        BigDecimal notional = leg.getNotional() != null ? leg.getNotional() : BigDecimal.ZERO;
        
        // For swaps: pay leg is negative, receive leg is positive
        if (leg.getPayReceiveFlag() != null) {
            String direction = leg.getPayReceiveFlag().getPayRec();
            if ("PAY".equalsIgnoreCase(direction)) {
                return notional.negate();
            } else if ("RECEIVE".equalsIgnoreCase(direction)) {
                return notional;
            }
        }
        
        return notional; // Default to positive if no direction specified
    }






    // FOLA ADDED: New method to get daily trade summary/ analytics
    public DailySummaryDTO getDailySummary() {
        LocalDate today = LocalDate.now();

        List<Trade> todaysTrades = tradeRepository.findAll().stream()
                .filter(t -> t.getTradeDate() != null && t.getTradeDate().isEqual(today))
                .collect(Collectors.toList());

        long todaysTradeCount = todaysTrades.size();

        BigDecimal todaysTotalNotional = todaysTrades.stream()
                .flatMap(t -> t.getTradeLegs().stream())
                .map(TradeLeg::getNotional)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal todaysAverageNotional = todaysTradeCount > 0
                ? todaysTotalNotional.divide(BigDecimal.valueOf(todaysTradeCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long todaysNewTrades = todaysTrades.stream()
                .filter(t -> t.getTradeStatus() != null && "NEW".equalsIgnoreCase(t.getTradeStatus().getTradeStatus()))
                .count();

        long todaysAmendedTrades = todaysTrades.stream()
                .filter(t -> t.getTradeStatus() != null && "AMENDED".equalsIgnoreCase(t.getTradeStatus().getTradeStatus()))
                .count();

        long todaysTerminatedTrades = todaysTrades.stream()
                .filter(t -> t.getTradeStatus() != null && "TERMINATED".equalsIgnoreCase(t.getTradeStatus().getTradeStatus()))
                .count();


        // Yesterday's summary for comparison
        LocalDate yesterday = today.minusDays(1);
        
        List<Trade> listOfYesterdayTrades = tradeRepository.findAll().stream()
                .filter(t -> t.getTradeDate() != null && t.getTradeDate().isEqual(yesterday))
                .collect(Collectors.toList());

        long yesterdayTradeCount = listOfYesterdayTrades.size();

        BigDecimal yesterdayTotalNotional = listOfYesterdayTrades.stream()
                .flatMap(t -> t.getTradeLegs().stream())
                .map(TradeLeg::getNotional)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal yesterdayAverageNotional = yesterdayTradeCount > 0
                ? yesterdayTotalNotional.divide(BigDecimal.valueOf(yesterdayTradeCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

       
        // Calculate day-over-day change percentage (current-previous)/previous * 100
        long difference = todaysTradeCount - yesterdayTradeCount;
        BigDecimal dayOverDayChange = yesterdayTradeCount > 0
                ? BigDecimal.valueOf(((double) difference / yesterdayTradeCount) * 100)
                : BigDecimal.ZERO;




        //USER SPECIFIC PERFORMANCE METRICS
        // Get logged-in user's username and filter today's trades for that user
        String username = getLoggedInUsername();

        Map<String, Long> todaysTradesByLoggedInTrader = new java.util.HashMap<>();
        if (username != null) {
            todaysTradesByLoggedInTrader = todaysTrades.stream()
                    .filter(t -> t.getTraderUser() != null &&
                            t.getTraderUser().getLoginId().equals(username))
                    .collect(Collectors.groupingBy(
                            t -> t.getTraderUser().getLoginId(),
                            Collectors.counting()
                    ));

            long userTodaysTradeCount = todaysTradesByLoggedInTrader.values().stream().mapToLong(Long::longValue).sum();
            // You can add more user-specific metrics here as needed
            logger.info("User {} made {} trades today", username, userTodaysTradeCount);
        } else {
            logger.warn("No authenticated user found for user-specific metrics");
        }


        //calculate notional for today's trades by the logged-in user
        String loggedInUsername = getLoggedInUsername();
        Map<String, BigDecimal> todaysNotionalByLoggedInTrader = todaysTrades.stream()
                .filter(t -> t.getTraderUser() != null && t.getTraderUser().getLoginId().equals(loggedInUsername))
                .collect(Collectors.groupingBy(
                        t -> t.getTraderUser().getLoginId(),
                        Collectors.reducing(BigDecimal.ZERO,
                            t -> t.getTradeLegs().stream()
                                    .map(TradeLeg::getNotional)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add),
                            BigDecimal::add)
            ));

        //Get today's trades by all Trader
        Map<String, Long> tradesByTrader = todaysTrades.stream()
            .filter(t -> t.getTraderUser() != null)
            .collect(Collectors.groupingBy(
                    t -> t.getTraderUser().getFirstName() + " " + t.getTraderUser().getLastName(),
                    Collectors.counting()
            ));

        //Get today's notional by all traders
        Map<String, BigDecimal> notionalByTrader = todaysTrades.stream()
            .filter(t -> t.getTraderUser() != null)
            .collect(Collectors.groupingBy(
                    t -> t.getTraderUser().getFirstName() + " " + t.getTraderUser().getLastName(),
                    Collectors.reducing(BigDecimal.ZERO,
                            t -> t.getTradeLegs().stream()
                                    .map(TradeLeg::getNotional)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add),
                            BigDecimal::add)
            ));

        //Get today's trades by all Inputter
        Map<String, Long> tradesByInputter = todaysTrades.stream()
            .filter(t -> t.getTradeInputterUser() != null)
            .collect(Collectors.groupingBy(
                    t -> t.getTradeInputterUser().getFirstName() + " " + t.getTradeInputterUser().getLastName(),
                    Collectors.counting()
            ));


        //TRADE SUMMARIES BY BOOK
        Map<String, Long> tradesByBook = todaysTrades.stream()
            .filter(t -> t.getBook() != null)
            .collect(Collectors.groupingBy(
                    t -> t.getBook().getBookName(),
                    Collectors.counting()
            ));
        
        Map<String, BigDecimal> notionalByBook = todaysTrades.stream()
                .filter(t -> t.getBook() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getBook().getBookName(),
                        Collectors.reducing(BigDecimal.ZERO,
                                t -> t.getTradeLegs().stream()
                                        .map(TradeLeg::getNotional)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                                BigDecimal::add)
                ));
        
        Map<String, Map<String, Long>> statusByBook = todaysTrades.stream()
                .filter(t -> t.getBook() != null && t.getTradeStatus() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getBook().getBookName(),
                        Collectors.groupingBy(
                                t -> t.getTradeStatus().getTradeStatus(),
                                Collectors.counting()
                        )
                ));

        // Populate the DailySummaryDTO with the calculated values
        return DailySummaryDTO.builder()
                .todaysTradeCount(todaysTradeCount)
                .todaysTotalNotional(todaysTotalNotional)
                .todaysAverageNotional(todaysAverageNotional)
                .todaysNewTrades(todaysNewTrades)
                .todaysAmendedTrades(todaysAmendedTrades)
                .todaysTerminatedTrades(todaysTerminatedTrades)
                .yesterdayTradeCount(yesterdayTradeCount)
                .yesterdayTotalNotional(yesterdayTotalNotional)
                .yesterdayAverageNotional(yesterdayAverageNotional)
                .dayOverDayChangePercentage(dayOverDayChange)
                .todaysTradesByLoggedInTrader(todaysTradesByLoggedInTrader)
                .todaysNotionalByLoggedInTrader(todaysNotionalByLoggedInTrader)
                .tradesByTrader(tradesByTrader)
                .notionalByTrader(notionalByTrader)
                .tradesByInputter(tradesByInputter)
                .tradesByBook(tradesByBook)
                .notionalByBook(notionalByBook)
                .statusByBook(statusByBook)
                .build();
    }


    // FOLA ADDED: New method to update settlement instructions for a trade
    // This method checks if the trade exists and calls the AdditionalInfoService to save or version the new instructions.
    @Transactional
    public void updateSettlementInstructions(Long tradeId, String instructions) {
        String userId = getLoggedInUsername();
        // Verify that the trade exists
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new RuntimeException("Trade not found with ID: " + tradeId));

        // Delegate to AdditionalInfoService for versioned storage
        additionalInfoService.saveSettlementInstructions(tradeId, instructions);

        // Log action for audit trail
        logger.info("User " + userId + " updated settlement instructions for Trade ID " + tradeId);
    }

    // FOLA ADDED: New method to search trades by settlement instructions content (partial match)
    // This method queries the AdditionalInfoService to find trade IDs with matching instructions
     public List<Trade> searchBySettlementInstructions(String instructions) {
        List<Long> tradeIds = additionalInfoService.findTradesBySettlementInstructions(instructions);
        return tradeRepository.findAllById(tradeIds);
    }




    /**
     * Validates settlement instruction text before saving.
     * Ensures the text meets business and security requirements.
     */
    private void validateSettlementInstructions(String instructions) {
        // Optional field — skip if not provided
        if (instructions == null || instructions.isBlank()) return;

        String trimmed = instructions.trim();

        // Length validation
        if (trimmed.length() < 10 || trimmed.length() > 500) {
            throw new RuntimeException("Settlement instructions must be between 10 and 500 characters");
        }

        // Content validation — reject special characters that might cause injection or formatting issues
        if (!trimmed.matches("^[a-zA-Z0-9.,;:()\\-\\s]+$")) {
            throw new RuntimeException("Settlement instructions contain invalid characters");
        }
    }


}
