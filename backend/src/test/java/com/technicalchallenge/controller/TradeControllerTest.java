package com.technicalchallenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.technicalchallenge.dto.DailySummaryDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeSummaryDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.repository.ApplicationUserRepository;
import com.technicalchallenge.security.CustomUserDetailsService;
import com.technicalchallenge.service.TradeService;

import io.swagger.v3.oas.annotations.Operation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = TradeController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "simon", roles = "TRADER")
public class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradeService tradeService;

    @MockBean
    private TradeMapper tradeMapper;

    @MockBean
    private ApplicationUserRepository applicationUserRepository;
    
    @MockBean
    private com.technicalchallenge.service.AdditionalInfoService additionalInfoService;
    
    @MockBean
    private com.technicalchallenge.repository.TradeRepository tradeRepository;
    
    @MockBean
    private com.technicalchallenge.repository.TradeLegRepository tradeLegRepository;
    
    @MockBean
    private com.technicalchallenge.repository.BookRepository bookRepository;
    
    @MockBean
    private com.technicalchallenge.repository.CounterpartyRepository counterpartyRepository;
    
    @MockBean
    private com.technicalchallenge.repository.AdditionalInfoRepository additionalInfoRepository;
    
    @MockBean
    private com.technicalchallenge.mapper.TradeLegMapper tradeLegMapper;

    private ObjectMapper objectMapper;

    private TradeDTO tradeDTO;
    private Trade trade;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Initialize Trade entity
        trade = new Trade();
        trade.setTradeId(1001L);
        trade.setVersion(1);
        trade.setTradeDate(LocalDate.now());
        trade.setTradeStartDate(LocalDate.now().plusDays(2));
        trade.setTradeMaturityDate(LocalDate.now().plusYears(5));

        // Initialize TradeDTO
        tradeDTO = new TradeDTO();
        tradeDTO.setTradeId(1001L);
        tradeDTO.setVersion(1);
        tradeDTO.setTradeDate(LocalDate.now());
        tradeDTO.setTradeStartDate(LocalDate.now().plusDays(2));
        tradeDTO.setTradeMaturityDate(LocalDate.now().plusYears(5));
        tradeDTO.setTradeStatus("LIVE");
        tradeDTO.setBookName("TestBook");
        tradeDTO.setCounterpartyName("TestCounterparty");
        tradeDTO.setTraderUserName("TestTrader");
        tradeDTO.setInputterUserName("TestInputter");

        authentication = org.mockito.Mockito.mock(Authentication.class);
        org.mockito.Mockito.when(authentication.getName()).thenReturn("simon");
    }

    @Test
    void testGetAllTrades() throws Exception {
        // Given
        List<Trade> trades = List.of(trade);

        when(tradeService.getAllTrades()).thenReturn(trades);
        when(tradeMapper.toDto(trade)).thenReturn(tradeDTO);

        // When/Then
        mockMvc.perform(get("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tradeId", is(1001)))
                .andExpect(jsonPath("$[0].bookName", is("TestBook")))
                .andExpect(jsonPath("$[0].counterpartyName", is("TestCounterparty")));

        verify(tradeService).getAllTrades();
    }

    @Test
    void testGetTradeById() throws Exception {
        // Given
        when(tradeService.getTradeById(1001L)).thenReturn(Optional.of(trade));
        when(tradeMapper.toDto(trade)).thenReturn(tradeDTO);

        // When/Then
        mockMvc.perform(get("/api/trades/1001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId", is(1001)))
                .andExpect(jsonPath("$.bookName", is("TestBook")))
                .andExpect(jsonPath("$.counterpartyName", is("TestCounterparty")));

        verify(tradeService).getTradeById(1001L);
    }

    @Test
    void testGetTradeByIdNotFound() throws Exception {
        // Given
        when(tradeService.getTradeById(9999L)).thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/api/trades/9999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(tradeService).getTradeById(9999L);
    }

    @Test
    void testCreateTrade() throws Exception {
        // Given
        when(authentication.getName()).thenReturn("simon"); //returns simon as the logged-in user
        when(tradeMapper.toEntity(any(TradeDTO.class))).thenReturn(trade); // ADD: Mock toEntity conversion
        when(tradeService.saveTrade(any(TradeDTO.class))).thenReturn(trade);
        when(tradeMapper.toDto(trade)).thenReturn(tradeDTO);
        when(tradeService.validateUserPrivileges(anyString(), eq("CREATE_TRADE"), any(TradeDTO.class)))
                            .thenReturn(true); //avoids calling real method
        
        doNothing().when(tradeService).populateReferenceDataByName(any(Trade.class), any(TradeDTO.class));

        // When/Then
        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tradeDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tradeId", is(1001)));
        verify(tradeService).saveTrade(any(TradeDTO.class));
        verify(tradeService).populateReferenceDataByName(any(Trade.class), any(TradeDTO.class));
    }

    @Test
    void testCreateTradeValidationFailure_MissingTradeDate() throws Exception {
        // Given
        TradeDTO invalidDTO = new TradeDTO();
        invalidDTO.setBookName("TestBook");
        invalidDTO.setCounterpartyName("TestCounterparty");
        // Trade date is purposely missing

        // When/Then
        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Trade date is required"));

        verify(tradeService, never()).saveTrade(any(TradeDTO.class));
    }

    @Test
    void testCreateTradeValidationFailure_MissingBook() throws Exception {
        // Given
        TradeDTO invalidDTO = new TradeDTO();
        invalidDTO.setTradeDate(LocalDate.now());
        invalidDTO.setCounterpartyName("TestCounterparty");
        // Book name is purposely missing

        // When/Then
        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Book and Counterparty are required"));

        verify(tradeService, never()).saveTrade(any(TradeDTO.class));
    }

    @Test
    void testUpdateTrade() throws Exception {
        // Given
        Long tradeId = 1001L;
        tradeDTO.setTradeId(tradeId);
        

        // FOLA COMMENTED: This ensures the trade entity also has the tradeId set; needed before calling the service layer
        trade.setTradeId(tradeId);

        // ADD: Mock toEntity conversion
        when(tradeMapper.toEntity(any(TradeDTO.class))).thenReturn(trade);
        // when(tradeService.saveTrade(any(Trade.class), any(TradeDTO.class))).thenReturn(trade);
        when(tradeService.amendTrade(eq(tradeId), any(TradeDTO.class))).thenReturn(trade);
        when(tradeMapper.toDto(trade)).thenReturn(tradeDTO); // ADD: Mock toDto conversion for response
        doNothing().when(tradeService).populateReferenceDataByName(any(Trade.class), any(TradeDTO.class));


        // When/Then
        mockMvc.perform(put("/api/trades/{id}", tradeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tradeDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId", is(1001)));

        //verify(tradeService).saveTrade(any(Trade.class), any(TradeDTO.class));
        verify(tradeService).amendTrade(eq(tradeId), any(TradeDTO.class));
    }

    @Test
    void testUpdateTradeIdMismatch() throws Exception {
        // Given
        Long pathId = 1001L;
        tradeDTO.setTradeId(2002L); // Different from path ID

        // When/Then
        mockMvc.perform(put("/api/trades/{id}", pathId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tradeDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error updating trade: Trade ID in path must match Trade ID in request body"));

        verify(tradeService, never()).saveTrade(any(TradeDTO.class));
    }

    @Test
    void testDeleteTrade() throws Exception {
        // Given
        doNothing().when(tradeService).deleteTrade(1001L);

        // When/Then
        mockMvc.perform(delete("/api/trades/1001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(tradeService).deleteTrade(1001L);
    }

    @Test
    void testCreateTradeWithValidationErrors() throws Exception {
        // Given
        TradeDTO invalidDTO = new TradeDTO();
        invalidDTO.setTradeDate(LocalDate.now()); // Fixed: LocalDate instead of LocalDateTime
        // Missing required fields to trigger validation errors

        // When/Then
        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        //verify(tradeService, never()).createTrade(any(TradeDTO.class));
        verify(tradeService, never()).saveTrade(any(TradeDTO.class));

    }

    // ========================================
    // NEW TESTS FOR UNCOVERED ENDPOINTS
    // ========================================

    @Test
    void testSearchTrades() throws Exception {
        // Given
        String counterparty = "Goldman Sachs";
        String book = "FX-BOOK-1";
        Long trader = null;
        String status = "CONFIRMED";
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();
        
        List<Trade> trades = List.of(trade);
        
        when(tradeService.searchTrades(counterparty, book, trader, status, from, to)).thenReturn(trades);
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(tradeDTO);
        
        // When/Then
        mockMvc.perform(get("/api/trades/search")
                        .param("counterparty", counterparty)
                        .param("book", book)
                        .param("status", status)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        
        verify(tradeService).searchTrades(counterparty, book, trader, status, from, to);
    }

    @Test
    void testFilterTrades() throws Exception {
        // Given
        String counterparty = "JP Morgan";
        String book = "RATES-BOOK-1";
        Long trader = null;
        String status = "NEW";
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();
        int page = 0;
        int size = 10;
        String sortBy = "tradeDate";
        String direction = "DESC";
        
        org.springframework.data.domain.Page<Trade> tradePage = 
                new org.springframework.data.domain.PageImpl<>(List.of(trade));
        
        when(tradeService.searchTrades(counterparty, book, trader, status, from, to, page, size, sortBy, direction))
                .thenReturn(tradePage);
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(tradeDTO);
        
        // When/Then
        mockMvc.perform(get("/api/trades/filter")
                        .param("counterparty", counterparty)
                        .param("book", book)
                        .param("status", status)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .param("sortBy", sortBy)
                        .param("direction", direction)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        
        verify(tradeService).searchTrades(counterparty, book, trader, status, from, to, page, size, sortBy, direction);
    }

    @Test
    void testSearchByRsql() throws Exception {
        // Given
        String rsqlQuery = "bookName==FX-BOOK-1";
        org.springframework.data.domain.Page<Trade> tradePage = 
                new org.springframework.data.domain.PageImpl<>(List.of(trade));
        
        when(tradeService.searchByRsql(eq(rsqlQuery), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(tradePage);
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(tradeDTO);
        
        // When/Then
        mockMvc.perform(get("/api/trades/rsql")
                        .param("query", rsqlQuery)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        
        verify(tradeService).searchByRsql(eq(rsqlQuery), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("Returns trades for the logged-in trader")
    void testGetMyTrades() throws Exception {
        // Given
        List<Trade> trades = List.of(trade);
        
        when(tradeService.getTradesByTrader()).thenReturn(trades);
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(tradeDTO);
        
        // When/Then
        mockMvc.perform(get("/api/trades/my-trades")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        
        verify(tradeService).getTradesByTrader();
    }

    @Test
    @DisplayName("Returns trades belonging to a specific trading book")
    void testGetBookTrades() throws Exception {
        // Given
        Long bookId = 1L;
        List<Trade> trades = List.of(trade);
        
        when(tradeService.getTradesByBook(bookId)).thenReturn(trades);
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(tradeDTO);
        
        // When/Then
        mockMvc.perform(get("/api/trades/book/{id}/trades", bookId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        
        verify(tradeService).getTradesByBook(bookId);
    }

    @Test
    @DisplayName("Returns a summary of all trades")
    void testGetTradeSummary() throws Exception {
        // Given
        TradeSummaryDTO summaryDTO = TradeSummaryDTO.builder()
                .totalTrades(100L)
                .build();
        
        when(tradeService.getTradeSummary()).thenReturn(summaryDTO);
        
        // When/Then
        mockMvc.perform(get("/api/trades/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTrades", is(100)));
        
        verify(tradeService).getTradeSummary();
    }

    @Test
    @DisplayName("Returns daily summary of trades")
    void testGetDailySummary() throws Exception {
        // Given
        DailySummaryDTO dailySummaryDTO = DailySummaryDTO.builder()
                .todaysTradeCount(25L)
                .todaysNewTrades(10L)
                .build();
        
        when(tradeService.getDailySummary()).thenReturn(dailySummaryDTO);
        
        // When/Then
        mockMvc.perform(get("/api/trades/daily-summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todaysTradeCount", is(25)));
        
        verify(tradeService).getDailySummary();
    }

    @Test
    @DisplayName("Searches trades by settlement instructions")
    void testSearchBySettlementInstructions() throws Exception {
        // Given
        String instructions = "Bank ABC";
        List<Trade> trades = List.of(trade);
        
        when(tradeService.searchBySettlementInstructions(instructions)).thenReturn(trades);
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(tradeDTO);
        
        // When/Then
        mockMvc.perform(get("/api/trades/search/settlement-instructions")
                        .param("instructions", instructions)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        
        verify(tradeService).searchBySettlementInstructions(instructions);
    }

    @Test
    @DisplayName("Updates settlement instructions for a trade")
    void testUpdateSettlementInstructions() throws Exception {
        // Given
        Long tradeId = 1001L;
        String instructions = "Pay to account XYZ123 at Bank ABC, routing number 123456789, SWIFT code ABCDEF12";
        
        com.technicalchallenge.dto.SettlementInstructionsUpdateDTO requestDTO = 
                new com.technicalchallenge.dto.SettlementInstructionsUpdateDTO();
        requestDTO.setInstructions(instructions);
        
        doNothing().when(tradeService).updateSettlementInstructions(tradeId, instructions);
        
        // When/Then
        mockMvc.perform(put("/api/trades/{id}/settlement-instructions", tradeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());
        
        verify(tradeService).updateSettlementInstructions(tradeId, instructions);
    }


}
