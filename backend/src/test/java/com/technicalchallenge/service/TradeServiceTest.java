package com.technicalchallenge.service;

import com.technicalchallenge.dto.DailySummaryDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.dto.TradeSummaryDTO;
import com.technicalchallenge.model.AdditionalInfo;
import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Cashflow;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.model.LegType;
import com.technicalchallenge.model.Privilege;
import com.technicalchallenge.model.Schedule;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.model.TradeLeg;
import com.technicalchallenge.model.TradeStatus;
import com.technicalchallenge.model.UserPrivilege;
import com.technicalchallenge.model.UserProfile;
import com.technicalchallenge.repository.AdditionalInfoRepository;
import com.technicalchallenge.repository.ApplicationUserRepository;
import com.technicalchallenge.repository.BookRepository;
import com.technicalchallenge.repository.CashflowRepository;
import com.technicalchallenge.repository.CounterpartyRepository;
import com.technicalchallenge.repository.HolidayCalendarRepository;
import com.technicalchallenge.repository.LegTypeRepository;
import com.technicalchallenge.repository.PrivilegeRepository;
import com.technicalchallenge.repository.ScheduleRepository;
import com.technicalchallenge.repository.TradeLegRepository;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.repository.TradeStatusRepository;
import com.technicalchallenge.repository.UserPrivilegeRepository;
import com.technicalchallenge.repository.UserProfileRepository;

import com.technicalchallenge.validation.ValidationResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.technicalchallenge.model.Currency;
import com.technicalchallenge.model.PayRec;
import com.technicalchallenge.mapper.TradeMapper;




@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private TradeLegRepository tradeLegRepository;

    @Mock
    private CashflowRepository cashflowRepository;

    @Mock
    private TradeStatusRepository tradeStatusRepository;


    //FOLA ADDED: Mocking additional dependent repositories/services
    @Mock
    private BookRepository bookRepository;

    @Mock
    private CounterpartyRepository counterpartyRepository;

   @Mock
   private HolidayCalendarRepository holidayCalendarRepository;

   @Mock
   private ScheduleRepository scheduleRepository;

   @Mock
   private LegTypeRepository legTypeRepository;
   
    @Mock
    private ApplicationUserRepository applicationUserRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PrivilegeRepository privilegeRepository;

    @Mock
    private UserPrivilegeRepository userPrivilegeRepository;



   // End of FOLA ADDED

    @Mock
    private AdditionalInfoService additionalInfoService;

    @Mock
    private AdditionalInfoRepository additionalInfoRepository;

    @Mock
    private TradeMapper tradeMapper;

    @Spy // This will create a spy for the TradeService. A spy allows us to call real methods unless they are stubbed.
    @InjectMocks
    private TradeService tradeService;

    private TradeDTO tradeDTO;
    private Trade trade;


    // Declare mockBook and other mock objects as fields
    private Book mockBook;
    private Counterparty mockCounterparty;
    private TradeStatus mockTradeStatus;
    private ApplicationUser testUser;
    private TradeLeg mockTradeLeg;

    @BeforeEach
    void setUp() {
        // Initialize Mockito annotations
        MockitoAnnotations.openMocks(this);
        // Set up test data
        tradeDTO = new TradeDTO();
        tradeDTO.setTradeId(100001L);
        tradeDTO.setTradeDate(LocalDate.now().minusDays(5));
        tradeDTO.setTradeStartDate(LocalDate.now().minusDays(3));
        tradeDTO.setTradeMaturityDate(LocalDate.now().plusYears(1));


        //FOLA ADDED: Setting bookName and counterpartyName to avoid null pointer exceptions in service methods
        tradeDTO.setBookName("TestBook");
        tradeDTO.setCounterpartyName("TestCounterparty");
        tradeDTO.setTradeStatus("NEW");

        TradeLegDTO leg1 = new TradeLegDTO();
        leg1.setNotional(BigDecimal.valueOf(1000000));
        leg1.setRate(0.05);

        TradeLegDTO leg2 = new TradeLegDTO();
        leg2.setNotional(BigDecimal.valueOf(1000000));
        leg2.setRate(0.0);

        tradeDTO.setTradeLegs(Arrays.asList(leg1, leg2));

        // Configure the DTO to explicitly use a Monthly schedule
        tradeDTO.getTradeLegs().forEach(leg -> {
            // "Monthly" is recognized by the parseSchedule method
            leg.setCalculationPeriodSchedule("Monthly"); 
        });
        
        mockBook = new Book();
        mockCounterparty = new Counterparty();
        mockTradeStatus = new TradeStatus();
        testUser = new ApplicationUser();
        mockTradeLeg = new TradeLeg();
        mockTradeLeg.setLegId(1L);

        testUser.setLoginId("testUser");
        testUser.setActive(true);
        UserProfile userProfile = new UserProfile();
        userProfile.setUserType("SUPERUSER");
       

         // Initialize a Trade object for reuse

        trade = new Trade();
        trade.setId(1L);
        trade.setTradeId(100001L);
        trade.setVersion(1); //FOLA ADDED: version to avoid null pointer exceptions

        // Stub the validateUserPrivileges method to always return true for testing
        tradeService = spy(tradeService); // Re-spy to ensure we have a fresh spy instance. Spy will call real methods unless stubbed.
        //doReturn(true).when(tradeService).validateUserPrivileges(anyString(), anyString(), any());
        lenient().doReturn(true).when(tradeService).validateUserPrivileges(any(), any(), any());

    }

    @Test
    void testCreateTrade_Success() {
        // Given

         //FOLA ADDED: Stubbing dependent repository methods
        when(bookRepository.findByBookName(anyString())).thenReturn(Optional.of(new Book()));
        when(counterpartyRepository.findByName(anyString())).thenReturn(Optional.of(new com.technicalchallenge.model.Counterparty()));
        when(tradeStatusRepository.findByTradeStatus(anyString())).thenReturn(Optional.of(new com.technicalchallenge.model.TradeStatus()));
        when(tradeLegRepository.save(any(TradeLeg.class))).thenReturn(new TradeLeg());
        lenient().when(applicationUserRepository.findByLoginId(anyString())).thenReturn(Optional.of(testUser));
        lenient().when(tradeService.validateUserPrivileges(anyString(), anyString(), any())).thenReturn(true);
        

        when(tradeRepository.save(any(Trade.class))).thenReturn(trade);

       

        // When
        Trade result = tradeService.createTrade(tradeDTO); 

        // Then
        assertNotNull(result);
        assertEquals(100001L, result.getTradeId());
        verify(tradeRepository).save(any(Trade.class));
        
    }

    @Test
    void testCreateTrade_InvalidDates_ShouldFail() {
        // Given - This test is intentionally failing for candidates to fix
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 10)); // Before trade date

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeService.createTrade(tradeDTO);
        });

        
        // This assertion is intentionally wrong - candidates need to fix it
        //FOLA ADDED: I changed the expected message to match the actual exception message thrown in the service
        assertTrue(exception.getMessage().contains("Start date cannot be before trade date"));

    }

    @Test
    void testCreateTrade_InvalidLegCount_ShouldFail() {
        // Given
        tradeDTO.setTradeLegs(Arrays.asList(new TradeLegDTO())); // Only 1 leg

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeService.createTrade(tradeDTO);
        });

        assertTrue(exception.getMessage().contains("exactly 2 legs"));
    }

    @Test
    void testGetTradeById_Found() {
        // Given
        when(tradeRepository.findByTradeIdAndActiveTrue(100001L)).thenReturn(Optional.of(trade));

        // When
        Optional<Trade> result = tradeService.getTradeById(100001L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(100001L, result.get().getTradeId());
    }

    @Test
    void testGetTradeById_NotFound() {
        // Given
        when(tradeRepository.findByTradeIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        // When
        Optional<Trade> result = tradeService.getTradeById(999L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testAmendTrade_Success() {
        // Given
        when(tradeRepository.findByTradeIdAndActiveTrue(100001L)).thenReturn(Optional.of(trade));
        when(tradeStatusRepository.findByTradeStatus("AMENDED")).thenReturn(Optional.of(new com.technicalchallenge.model.TradeStatus()));
        
        //FOLA ADDED:Add stubbing for "NEW" status as well. Its good to stub all posible statuses that can be modified or amended
        when(tradeStatusRepository.findByTradeStatus("NEW")).thenReturn(Optional.of(new com.technicalchallenge.model.TradeStatus()));

        when(tradeLegRepository.save(any(TradeLeg.class))).thenReturn(new TradeLeg());
        // End of FOLA ADDED

        when(tradeRepository.save(any(Trade.class))).thenReturn(trade);

        // When
        Trade result = tradeService.amendTrade(100001L, tradeDTO);

        // Then
        assertNotNull(result);
        verify(tradeRepository, times(2)).save(any(Trade.class)); // Save old and new
    }

    @Test
    void testAmendTrade_TradeNotFound() {
        // Given
        when(tradeRepository.findByTradeIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeService.amendTrade(999L, tradeDTO);
        });

        assertTrue(exception.getMessage().contains("Trade not found"));
    }

    // This test has a deliberate bug for candidates to find and fix
    @Test
    void testCashflowGeneration_MonthlySchedule() {
        // This test method is incomplete and has logical errors
        // Candidates need to implement proper cashflow testing

        // Given - setup is incomplete
     // Mock reference data lookups for Trade (from testCreateTrade_Success)
        when(tradeRepository.save(any(Trade.class))).thenReturn(trade);
        when(bookRepository.findByBookName(anyString())).thenReturn(Optional.of(mockBook));
        when(counterpartyRepository.findByName(anyString())).thenReturn(Optional.of(mockCounterparty));
        when(tradeStatusRepository.findByTradeStatus("NEW")).thenReturn(Optional.of(mockTradeStatus));
        lenient().when(applicationUserRepository.findByLoginId(anyString())).thenReturn(Optional.of(testUser));
        lenient().when(userProfileRepository.findById(anyLong())).thenReturn(Optional.of(new UserProfile()));

        // Mock reference data lookups for TradeLegs
        Schedule mockSchedule = new Schedule();
        mockSchedule.setSchedule("Monthly");
        // Mocking the reference data lookup for the schedule
        when(scheduleRepository.findBySchedule("Monthly")).thenReturn(Optional.of(mockSchedule));

        // Mock the TradeLegRepository.save() to ensure the saved leg has the Schedule
        // The generateCashflows logic relies on the Schedule being on the saved leg.
        when(tradeLegRepository.save(any(TradeLeg.class))).thenAnswer(invocation -> {
            TradeLeg savedLeg = invocation.getArgument(0);
            savedLeg.setLegId(1L); // Simulate ID generation
            savedLeg.setCalculationPeriodSchedule(mockSchedule); // Inject the Schedule entity
            return savedLeg;
        });

        // WHEN
        // Call the public method which triggers the entire cashflow generation process
        tradeService.createTrade(tradeDTO);

        // Then
        // I expect 12 cashflows per leg, 2 legs = 24 cashflows
        verify(cashflowRepository, times(24)).save(any(Cashflow.class));

    }




    @Test
    @DisplayName("Test searchTrades - basic search with all parameters provided. This should pass.")
    void testSearchTrades_BasicSearch_AllParameters_ShouldPass() {
        // Given
        String counterparty = "CounterpartyA";
        String book = "BookA";
        Long trader = 101L; // Assuming trader is represented by user ID
        String status = "NEW";
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any()))
            .thenReturn(List.of(trade));

        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any()))
            .thenReturn(List.of());

        // When
        List<Trade> result = tradeService.searchTrades(counterparty, book, trader, status, from, to);

        // Then
        assertNotNull(result);
    }


    @Test
    @DisplayName("Test searchTrades - basic search with partial parameters provided. This should pass.")
    void testSearchTrades_BasicSearch_PartialParameters_ShouldPass() {
        // Arrange - only counterparty and status provided
        String counterparty = "Pholar Counterparty";
        String book = null;
        Long trader = null;
        String status = "TERMINATED";
        LocalDate from = null;
        LocalDate to = null;

        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any()))
            .thenReturn(List.of(trade));

        // When
        List<Trade> result = tradeService.searchTrades(counterparty, book, trader, status, from, to);

        // Then
        assertNotNull(result);
    }


    @Test
    @DisplayName("Test searchTrades - basic search with all parameters provided but no results. This should pass.")
    void testSearchTrades_BasicSearch_NoResults_ShouldPass() {
        // Arrange - all parameters provided but no matching trades
        String counterparty = "Pholar Counterparty";
        String book = "Test Book";
        Long trader = 201L;
        String status = "DELETED";
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any()))
            .thenReturn(List.of()); // No results

        // When
        List<Trade> result = tradeService.searchTrades(counterparty, book, trader, status, from, to);

        // Then
        assertTrue(result.isEmpty());
    }


      @Test
    @DisplayName("Test searchTrades - basic search with all null parameters. This should pass.")
    void testSearchTrades_BasicSearch_AllNullParameters_ShouldPass() {
        // Arrange - all parameters null, so no value assisgned to any variable

        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any()))
            .thenReturn(List.of()); // No results

        // When
        List<Trade> result = tradeService.searchTrades(null, null, null, null, null, null);

        // Then
        assertTrue(result.isEmpty());
    }


    @Test
    @DisplayName("Test searchTrades - paginated search with sorting")
    void testSearchTrades_PaginatedSearch_WithSorting() {
        // Arrange
        String counterparty = "Goldman Sachs";
        String book = "FX-BOOK-1";
        Long trader = 1L;
        String status = "NEW";
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);
        int page = 0;
        int size = 10;
        String sortBy = "tradeDate";
        String direction = "DESC";

        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any(), eq(PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy)))))
            .thenReturn(new PageImpl<>(List.of(trade)));

        // When
        Page<Trade> result = tradeService.searchTrades(counterparty, book, trader, status, from, to, page, size, sortBy, direction);

        // Then
        assertNotNull(result);
    }


    @Test
    @DisplayName("Test searchByRsql - date range query")
    void testSearchByRsql_DateRangeQuery() {
        // Arrange
        String query = "tradeDate=ge=2025-01-01;tradeDate=le=2025-12-31";
        Pageable pageable = PageRequest.of(0, 10);
        
        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any(), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(trade)));

        // When
        Page<Trade> result = tradeService.searchByRsql(query, pageable);
        // Then
        assertNotNull(result);

    }

    @Test
    @DisplayName("Test searchByRsql - complex query with multiple conditions")
    void testSearchByRsql_ComplexQuery() {
        // Arrange
        String query = "counterparty.name==Goldman;tradeStatus.tradeStatus==NEW;book.bookName==FX-BOOK-1";
        Pageable pageable = PageRequest.of(0, 20);

        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any(), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(trade)));

        // When
        Page<Trade> result = tradeService.searchByRsql(query, pageable);

        // Then
        assertNotNull(result);

    }

    
    @Test
    @DisplayName("Test RSQL search with invalid query. This test should fail gracefully.")
    void testRsqlSearchInvalidQuery_shouldFail() {
        String query = "invalid.field==value";
        Pageable pageable = PageRequest.of(0, 10);
        
        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any(), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of()));

        Page<Trade> result = tradeService.searchByRsql(query, pageable);

        assertEquals(0, result.getTotalElements());
    }


    @Test
    @DisplayName("Test to validate trade business rules with invalid dates. This should fail.")
    void testValidateTradeBusinessRules_InvalidDates_ShouldFail() {
        tradeDTO.setTradeDate(LocalDate.now());
        tradeDTO.setTradeStartDate(LocalDate.now().minusDays(5)); // invalid date given for testing

        ValidationResult result = tradeService.validateTradeBusinessRules(tradeDTO);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Start date cannot be before trade date"));
    }

    @Test
    @DisplayName("Test to validate trade business rules with valid dates. This should pass.")
    void testValidateTradeBusinessRules_ValidDates_ShouldPass() {
        tradeDTO.setTradeDate(LocalDate.now());
        tradeDTO.setTradeStartDate(LocalDate.now().plusDays(5)); // valid date given for testing

        ValidationResult result = tradeService.validateTradeBusinessRules(tradeDTO);

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }


    @Test
    @DisplayName("Test to validate that trade date is not more than 30 days in the past. This should fail.")
    void testValidateTradeBusinessRules_TradeDateNotMoreThan30DaysInPast_ShouldFail() {
        tradeDTO.setTradeDate(LocalDate.now().minusDays(35));
        tradeDTO.setTradeStartDate(LocalDate.now());
        ValidationResult result = tradeService.validateTradeBusinessRules(tradeDTO);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Trade date cannot be more than 30 days in the past"));
    }

    @Test
    @DisplayName("Test to validate that trade date within 30 days in the past. This should pass.")
    void testValidateTradeBusinessRules_TradeDateWithin30DaysInPast_ShouldPass() {
        tradeDTO.setTradeDate(LocalDate.now().minusDays(20));
        tradeDTO.setTradeStartDate(LocalDate.now());
        ValidationResult result = tradeService.validateTradeBusinessRules(tradeDTO);

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }


    @Test
    @DisplayName("Test to validate maturity date is after trade start/execution date and trade date. This should fail.")
    void testValidateTradeBusinessRules_MaturityDateAfterTradeDates_ShouldFail() {
        //tradeDTO.setTradeDate(LocalDate.now());
        tradeDTO.setTradeStartDate(LocalDate.now().plusDays(5));
        tradeDTO.setTradeMaturityDate(LocalDate.now().plusDays(2)); // Invalid maturity date for testing
        ValidationResult result = tradeService.validateTradeBusinessRules(tradeDTO);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Maturity date cannot be before start date or trade date"));
    }


    @Test
    @DisplayName("Test trade leg consistency validation")
    void testValidateTradeLegConsistency_shouldFail() {
        TradeLegDTO leg1 = new TradeLegDTO();
        leg1.setNotional(BigDecimal.valueOf(1000000));
        leg1.setRate(0.05);
        leg1.setLegType("Fixed");
        leg1.setPayReceiveFlag("PAY");
        

        TradeLegDTO leg2 = new TradeLegDTO();
        leg2.setNotional(BigDecimal.valueOf(1000000));
        leg2.setRate(0.0);
        leg2.setLegType("Floating");
        leg2.setPayReceiveFlag("PAY"); // Both legs set to PAY, should fail consistency check
        leg2.setIndexName("LIBOR");  //Floating leg needs index

        List<TradeLegDTO> legs = List.of(leg1, leg2);

        ValidationResult result = tradeService.validateTradeLegConsistency(legs);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Legs must have opposite pay/receive flags"));
    }



    @Test
    @DisplayName("Test trade leg consistency validation - should pass")
    void testValidateTradeLegConsistency_shouldPass() {
        TradeLegDTO leg1 = new TradeLegDTO();
        leg1.setNotional(BigDecimal.valueOf(1000000));
        leg1.setRate(0.05);
        leg1.setLegType("Fixed");
        leg1.setPayReceiveFlag("PAY");  // PAY leg
        
        TradeLegDTO leg2 = new TradeLegDTO();
        leg2.setNotional(BigDecimal.valueOf(1000000));
        leg2.setRate(0.0);
        leg2.setLegType("Floating");
        leg2.setPayReceiveFlag("RECEIVE");  // RECEIVE leg (opposite)
        leg2.setIndexName("LIBOR"); //Floating leg needs index

        List<TradeLegDTO> legs = List.of(leg1, leg2);

        ValidationResult result = tradeService.validateTradeLegConsistency(legs);

        assertTrue(result.isValid());  // Should pass
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Test to validate trade creation privilege for SUPERUSER. This should pass.")   
    void testValidateUserPrivileges_SuperUser_ShouldPass() {
        // Given
        ApplicationUser user = new ApplicationUser();
        user.setLoginId("superuser");
        UserProfile profile = new UserProfile();
        profile.setUserType("SUPERUSER");
        user.setUserProfile(profile);
        lenient().when(applicationUserRepository.findByLoginId("superuser")).thenReturn(Optional.of(user));
        // When
        boolean hasPrivileges = tradeService.validateUserPrivileges("superuser", "CREATE_TRADE", tradeDTO);

        // Then
        assertTrue(hasPrivileges);
    }





    
    // @Test
    // @DisplayName("Test to validate trade creation privilege for middle office user. This should fail.")
    // void testValidateUserPrivileges_middleoffice_ShouldFail() {
    //     // Reset the spy to remove the global stubbing for this specific test
    //     reset(tradeService);
        
    //     // Given
    //     ApplicationUser user = new ApplicationUser();
    //     user.setId(1001L);
    //     user.setLoginId("middleofficeuser");

    //     UserProfile profile = new UserProfile();
    //     profile.setId(1001L);
    //     profile.setUserType("MO");
    //     user.setUserProfile(profile);

    //     when(applicationUserRepository.findByLoginId("middleofficeuser")).thenReturn(Optional.of(user));
    //     when(userProfileRepository.findById(1001L)).thenReturn(Optional.of(profile));
    
    //     // Mock userPrivilegeRepository to return privileges that do NOT include "BOOK_TRADE"
    //     UserPrivilege readPrivilege = new UserPrivilege();
    //     readPrivilege.setPrivilegeId(1002L); // This corresponds to "READ_TRADE"

    //     when(userPrivilegeRepository.findByUserId(1001L)).thenReturn(List.of(readPrivilege));

    //     // Mock privilegeRepository to map that privilege ID to "READ_TRADE"
    //     Privilege privilege = new Privilege();
    //     privilege.setId(1002L);
    //     privilege.setName("READ_TRADE"); // User only has READ_TRADE, not BOOK_TRADE

    //     when(privilegeRepository.findAllById(List.of(1002L))).thenReturn(List.of(privilege));

    //     // When - Check for BOOK_TRADE privilege (which user doesn't have)
    //     boolean hasPrivileges = tradeService.validateUserPrivileges("middleofficeuser", "BOOK_TRADE", tradeDTO);

    //     // Then - Should be false because user doesn't have BOOK_TRADE privilege
    //     assertFalse(hasPrivileges);
    // }


    @Test
    @DisplayName("Test getTradesByTrader - returns trades for logged-in user")
    void testGetTradesByTrader_ReturnsTradesForLoggedInUser() {
        // Arrange
        String loggedInUsername = "trader1";
        
        // Mock the getLoggedInUsername() method to return our test user
        // Spy the tradeService to override getLoggedInUsername, so we can simulate logged-in user
        TradeService spyService = Mockito.spy(tradeService);
        doReturn(loggedInUsername).when(spyService).getLoggedInUsername();
        
        // Create test trades with trader information
        ApplicationUser trader1 = new ApplicationUser();
        trader1.setLoginId(loggedInUsername);
        trader1.setFirstName("Test");
        trader1.setLastName("Trader");
        
        Trade userTrade = new Trade();
        userTrade.setTradeId(1001L);
        userTrade.setTraderUser(trader1);
        userTrade.setActive(true);
        
        // ✅ FIX: Use findAll() without arguments
        when(tradeRepository.findAll()).thenReturn(List.of(userTrade));
        
        // Act
        List<Trade> result = spyService.getTradesByTrader();
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(loggedInUsername, result.get(0).getTraderUser().getLoginId());
    }


    @Test
    @DisplayName("Test getTradesByTrader - returns empty list when user has no trades")
    void testGetTradesByTrader_UserHasNoTrades() {
        // Arrange
        String loggedInUsername = "trader3";

        TradeService spyService = Mockito.spy(tradeService);
        doReturn(loggedInUsername).when(spyService).getLoggedInUsername();

        // Create trades for other users only
        ApplicationUser otherTrader = new ApplicationUser();
        otherTrader.setLoginId("other_trader");
        otherTrader.setFirstName("Other");
        otherTrader.setLastName("Trader");
        
        Trade otherTrade = new Trade();
        otherTrade.setTradeId(1002L);
        otherTrade.setTraderUser(otherTrader);
        otherTrade.setActive(true);

        // ✅ FIX: Use findAll() without arguments
        when(tradeRepository.findAll()).thenReturn(List.of(otherTrade));

        // Act
        List<Trade> result = spyService.getTradesByTrader();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }


    @Test
    @DisplayName("Test getTradeSummary - returns summary with correct totals")
    void testGetTradeSummary_ReturnsCorrectSummary() {
        // Arrange
        Trade trade1 = createTestTradeForSummary("Goldman Sachs", "FX-BOOK-1", "NEW", "FX_SWAP");
        Trade trade2 = createTestTradeForSummary("JP Morgan", "RATES-BOOK-1", "CONFIRMED", "IRS");
        Trade trade3 = createTestTradeForSummary("Goldman Sachs", "FX-BOOK-1", "NEW", "FX_SWAP");
        
        // ✅ FIX: Use findAll() without arguments
        when(tradeRepository.findAll()).thenReturn(List.of(trade1, trade2, trade3));
        
        // Act
        TradeSummaryDTO result = tradeService.getTradeSummary();
        
        // Assert
        assertNotNull(result);
        assertEquals(3L, result.getTotalTrades());
        
        // Check trades by status
        assertEquals(2L, result.getTradesByStatus().get("NEW"));
        assertEquals(1L, result.getTradesByStatus().get("CONFIRMED"));
        
        // Check trades by counterparty
        assertEquals(2L, result.getTradesByCounterparty().get("Goldman Sachs"));
        assertEquals(1L, result.getTradesByCounterparty().get("JP Morgan"));
        
        verify(tradeRepository).findAll();
    }


    @Test
    @DisplayName("Test cashflow calculation - Bug Fix Verification, this should pass now")
    void testCalculateCashflowValue_FixedLeg_BugFixed_ShouldPass() {
        // Arrange: $10M notional, 3.5% rate, quarterly (3 months)
        TradeLeg fixedLeg = new TradeLeg();
        fixedLeg.setNotional(new BigDecimal("10000000.00"));  // $10M
        fixedLeg.setRate(3.5);  // 3.5% stored as percentage
        
        LegType legType = new LegType();
        legType.setType("Fixed");
        fixedLeg.setLegRateType(legType);
        
        // Act: Calculate quarterly cashflow (3 months)
        BigDecimal result = tradeService.calculateCashflowValue(fixedLeg, 3);
        
        // Assert: Should be $87,500 (not $8,750,000)
        BigDecimal expected = new BigDecimal("87500.00");
        assertEquals(0, expected.compareTo(result), 
            "Expected $87,500 but got $" + result + " - Bug not fixed!");
    }


     @Test
    @DisplayName("Test cashflow calculation - Bug Fix Verification, this should pass returning zero for floating leg with zero rate")
    void testCalculateCashflowValue_FloatingLeg_BugFixed_ShouldPass() {
        // Arrange: $20M notional, quarterly (6 months)
        TradeLeg floatingLeg = new TradeLeg();
        floatingLeg.setNotional(new BigDecimal("20000000.00"));  // $20M
        // Rate is zero for floating leg in this test. Floating legs have an index rate, so we simulate zero rate here.
        
        LegType legType = new LegType();
        legType.setType("Floating");
        floatingLeg.setLegRateType(legType);

        // Act: Calculate semi-annual cashflow (6 months)
        BigDecimal result = tradeService.calculateCashflowValue(floatingLeg, 6);

        // Assert: Should be 0 since rate is zero
        BigDecimal expected = BigDecimal.ZERO;
        assertEquals(0, expected.compareTo(result),
            "Expected $0 but got $" + result + " - Bug not fixed!");
    }



    @Test
    @DisplayName("Test cashflow calculation - Edge cases- when notional is zero")
    void testCalculateCashflowValue_EdgeCases_WhenNotionalIsZero() {
        // Arrange: Create trade leg with zero notional
        TradeLeg legWithZeroNotional = new TradeLeg();
        legWithZeroNotional.setNotional(BigDecimal.ZERO);
        legWithZeroNotional.setRate(3.5);
        
        LegType legType = new LegType();
        legType.setType("Fixed");
        legWithZeroNotional.setLegRateType(legType);

        // Act: Calculate cashflow
        BigDecimal result = tradeService.calculateCashflowValue(legWithZeroNotional, 3);

        // Assert: Should be zero cashflow
        assertEquals(0, result.compareTo(BigDecimal.ZERO), "Expected zero cashflow for zero notional");
    }


    // This tests for when rate is zero 
    @Test
    @DisplayName("Test cashflow calculation - Edge cases- when rate is zero")
    void testCalculateCashflowValue_EdgeCases_WhenRateIsZero() {
        // Arrange: Create trade leg with zero rate
        TradeLeg legWithZeroRate = new TradeLeg();
        legWithZeroRate.setNotional(new BigDecimal(2000000));
        legWithZeroRate.setRate(0.0);
        
        LegType legType = new LegType();
        legType.setType("Fixed");
        legWithZeroRate.setLegRateType(legType);

        // Act: Calculate cashflow
        BigDecimal result = tradeService.calculateCashflowValue(legWithZeroRate, 3);

        // Assert: Should be zero cashflow
        assertEquals(0, result.compareTo(BigDecimal.ZERO), "Expected zero cashflow for zero rate");
    }


    // Add this helper method to your test class
    private Trade createTestTradeForSummary(String counterpartyName, String bookName, String status, String tradeType) {
        Trade trade = new Trade();
        trade.setTradeDate(LocalDate.now());
        trade.setActive(true);
        
        // Set counterparty
        Counterparty counterparty = new Counterparty();
        counterparty.setName(counterpartyName);
        trade.setCounterparty(counterparty);
        
        // Set book
        Book book = new Book();
        book.setBookName(bookName);
        trade.setBook(book);
        
        // Set status
        TradeStatus tradeStatus = new TradeStatus();
        tradeStatus.setTradeStatus(status);
        trade.setTradeStatus(tradeStatus);
        
        // Set trade type (if you have this entity)
        // TradeType type = new TradeType();
        // type.setTradeType(tradeType);
        // trade.setTradeType(type);
        
        // Add trade legs to avoid null pointer exceptions
        TradeLeg leg1 = new TradeLeg();
        leg1.setNotional(BigDecimal.valueOf(500000));
        
        // Add currency
        Currency usd = new Currency();
        usd.setCurrency("USD");
        leg1.setCurrency(usd);
        
        // Add pay/receive flag
        PayRec payRec = new PayRec();
        payRec.setPayRec("PAY");
        leg1.setPayReceiveFlag(payRec);
        
        trade.setTradeLegs(List.of(leg1));
        
        return trade;
    }

}