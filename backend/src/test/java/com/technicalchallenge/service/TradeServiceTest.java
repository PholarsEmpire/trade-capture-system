package com.technicalchallenge.service;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Cashflow;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.model.LegType;
import com.technicalchallenge.model.Schedule;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.model.TradeLeg;
import com.technicalchallenge.model.TradeStatus;
import com.technicalchallenge.repository.ApplicationUserRepository;
import com.technicalchallenge.repository.BookRepository;
import com.technicalchallenge.repository.CashflowRepository;
import com.technicalchallenge.repository.CounterpartyRepository;
import com.technicalchallenge.repository.HolidayCalendarRepository;
import com.technicalchallenge.repository.LegTypeRepository;
import com.technicalchallenge.repository.ScheduleRepository;
import com.technicalchallenge.repository.TradeLegRepository;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.repository.TradeStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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


   // End of FOLA ADDED

    @Mock
    private AdditionalInfoService additionalInfoService;

    @InjectMocks
    private TradeService tradeService;

    private TradeDTO tradeDTO;
    private Trade trade;

    // Declare mockBook and other mock objects as fields
    private Book mockBook;
    private Counterparty mockCounterparty;
    private TradeStatus mockTradeStatus;
    private ApplicationUser mockTraderUser;
    private TradeLeg mockTradeLeg;

    @BeforeEach
    void setUp() {
        // Set up test data
        tradeDTO = new TradeDTO();
        tradeDTO.setTradeId(100001L);
        tradeDTO.setTradeDate(LocalDate.of(2025, 1, 15));
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 17));
        tradeDTO.setTradeMaturityDate(LocalDate.of(2026, 1, 17));


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
        mockTraderUser = new ApplicationUser();
        mockTradeLeg = new TradeLeg();
        mockTradeLeg.setLegId(1L);

        trade = new Trade();
        trade.setId(1L);
        trade.setTradeId(100001L);
        trade.setVersion(1); //FOLA ADDED: version to avoid null pointer exceptions
    }

    @Test
    void testCreateTrade_Success() {
        // Given

         //FOLA ADDED: Stubbing dependent repository methods
        when(bookRepository.findByBookName(anyString())).thenReturn(Optional.of(new Book()));
        when(counterpartyRepository.findByName(anyString())).thenReturn(Optional.of(new com.technicalchallenge.model.Counterparty()));
        when(tradeStatusRepository.findByTradeStatus(anyString())).thenReturn(Optional.of(new com.technicalchallenge.model.TradeStatus()));
        when(tradeLegRepository.save(any(TradeLeg.class))).thenReturn(new TradeLeg());
            // End of FOLA ADDED

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
        assertEquals("Start date cannot be before trade date", exception.getMessage());
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
}
