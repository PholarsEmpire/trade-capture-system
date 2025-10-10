package com.technicalchallenge.controller;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.rsql.RsqlSpecificationBuilder;
import com.technicalchallenge.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/trades")
@Validated
@Tag(name = "Trades", description = "Trade management operations including booking, searching, and lifecycle management")
public class TradeController {
    private static final Logger logger = LoggerFactory.getLogger(TradeController.class);

    @Autowired
    private TradeService tradeService;
    @Autowired
    private TradeMapper tradeMapper;
    @Autowired
    private TradeRepository tradeRepository;

    @GetMapping
    @Operation(summary = "Get all trades",
               description = "Retrieves a list of all trades in the system. Returns comprehensive trade information including legs and cashflows.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all trades",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public List<TradeDTO> getAllTrades() {
        logger.info("Fetching all trades");
        return tradeService.getAllTrades().stream()
                .map(tradeMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get trade by ID",
               description = "Retrieves a specific trade by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trade found and returned successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "404", description = "Trade not found"),
        @ApiResponse(responseCode = "400", description = "Invalid trade ID format")
    })
    public ResponseEntity<TradeDTO> getTradeById(
            @Parameter(description = "Unique identifier of the trade", required = true)
            @PathVariable(name = "id") Long id) {
        logger.debug("Fetching trade by id: {}", id);
        return tradeService.getTradeById(id)
                .map(tradeMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new trade",
               description = "Creates a new trade with the provided details. Automatically generates cashflows and validates business rules.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Trade created successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid trade data or business rule violation"),
        @ApiResponse(responseCode = "500", description = "Internal server error during trade creation")
    })
    public ResponseEntity<?> createTrade(
            @Parameter(description = "Trade details for creation", required = true)
            @Valid @RequestBody TradeDTO tradeDTO) {
        logger.info("Creating new trade: {}", tradeDTO);
        try {
          
               // FOLA COMMENTED: Added null checks for bookName and counterpartyName before a trade is created
            if (tradeDTO.getBookName() == null || tradeDTO.getCounterpartyName() == null) {
            return ResponseEntity.badRequest().body("Book and Counterparty are required");
            }

            // FOLA COMMENTED: Added null check for tradeDate before a trade is created
            if (tradeDTO.getTradeDate() == null) {
            return ResponseEntity.badRequest().body("Trade date is required");
            }

            Trade trade = tradeMapper.toEntity(tradeDTO);
            tradeService.populateReferenceDataByName(trade, tradeDTO);
            Trade savedTrade = tradeService.saveTrade(trade, tradeDTO);
            TradeDTO responseDTO = tradeMapper.toDto(savedTrade);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (Exception e) {
            logger.error("Error creating trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error creating trade: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update existing trade",
               description = "Updates an existing trade with new information. Subject to business rule validation and user privileges.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trade updated successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "404", description = "Trade not found"),
        @ApiResponse(responseCode = "400", description = "Invalid trade data or business rule violation"),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges to update trade")
    })
    public ResponseEntity<?> updateTrade(
            @Parameter(description = "Unique identifier of the trade to update", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated trade details", required = true)
            @Valid @RequestBody TradeDTO tradeDTO) {
        logger.info("Updating trade with id: {}", id);
        try {
            
            // FOLA COMMENTED: Added check to ensure the tradeId in the path matches the tradeId in the request body if provided
            // This is important to prevent inconsistencies and ensure the correct trade is being updated
            if (tradeDTO.getTradeId() != null && !tradeDTO.getTradeId().equals(id)) {
                return ResponseEntity.badRequest().body("Error updating trade: Trade ID in path must match Trade ID in request body");
            }
            


            tradeDTO.setTradeId(id); // Ensure the ID matches

            //FOLA COMMENTED: Added the 2 lines below to ensure the tradeId is set in both DTO and entity
            // This is important because the service layer uses the tradeId from the entity for processing
            Trade trade = tradeMapper.toEntity(tradeDTO);// Convert DTO to entity
            trade.setTradeId(id); // Ensure tradeId is set in entity

            Trade amendedTrade = tradeService.amendTrade(id, tradeDTO);
            TradeDTO responseDTO = tradeMapper.toDto(amendedTrade);

            // FOLA COMMENTED: Added this check to ensure the tradeId is set in the response DTO even if the service layer does not return it
            // This is important for consistency in the API response
            if (responseDTO.getTradeId() == null) {
                responseDTO.setTradeId(id);
            }

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            logger.error("Error updating trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error updating trade: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete trade",
               description = "Deletes an existing trade. This is a soft delete that changes the trade status.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trade deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Trade not found"),
        @ApiResponse(responseCode = "400", description = "Trade cannot be deleted in current status"),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges to delete trade")
    })
    public ResponseEntity<?> deleteTrade(
            @Parameter(description = "Unique identifier of the trade to delete", required = true)
            @PathVariable Long id) {
        logger.info("Deleting trade with id: {}", id);
        try {
            tradeService.deleteTrade(id);
            // FOLA COMMENTED: Changed from returning 200 OK with message to 204 No Content to conform with RESTful best practices
           // return ResponseEntity.ok().body("Trade cancelled successfully");
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error deleting trade: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "Terminate trade",
               description = "Terminates an existing trade before its natural maturity date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trade terminated successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "404", description = "Trade not found"),
        @ApiResponse(responseCode = "400", description = "Trade cannot be terminated in current status"),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges to terminate trade")
    })
    public ResponseEntity<?> terminateTrade(
            @Parameter(description = "Unique identifier of the trade to terminate", required = true)
            @PathVariable Long id) {
        logger.info("Terminating trade with id: {}", id);
        try {
            Trade terminatedTrade = tradeService.terminateTrade(id);
            TradeDTO responseDTO = tradeMapper.toDto(terminatedTrade);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            logger.error("Error terminating trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error terminating trade: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel trade",
               description = "Cancels an existing trade by changing its status to cancelled")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trade cancelled successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = TradeDTO.class))),
        @ApiResponse(responseCode = "404", description = "Trade not found"),
        @ApiResponse(responseCode = "400", description = "Trade cannot be cancelled in current status"),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges to cancel trade")
    })
    public ResponseEntity<?> cancelTrade(
            @Parameter(description = "Unique identifier of the trade to cancel", required = true)
            @PathVariable Long id) {
        logger.info("Cancelling trade with id: {}", id);
        try {
            Trade cancelledTrade = tradeService.cancelTrade(id);
            TradeDTO responseDTO = tradeMapper.toDto(cancelledTrade);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            logger.error("Error cancelling trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error cancelling trade: " + e.getMessage());
        }
    }

    // FOLA ADDED ADDITIONAL ENDPOINT FOR SERACHING AND FILTERING TRADES
    @GetMapping("/search")
    @Operation(summary = "Searches trades based on multiple criteria",
               description = "Searches for trades based on optional criteria such as book name, counterparty name, trader, trade date range, and status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trades retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter(s)"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<TradeDTO>> searchTrades(
            @RequestParam(required = false) String counterparty,
            @RequestParam(required = false) String book,
            @RequestParam(required = false) Long trader,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        logger.info("Searching trades with criteria: counterparty={}, book={}, trader={}, status={}, from={}, to={}",
                    counterparty, book, trader, status, from, to);
        
        List<Trade> trades = tradeService.searchTrades(counterparty, book, trader, status, from, to);
        List<TradeDTO> results = trades.stream().map(tradeMapper::toDto).toList();
        return ResponseEntity.ok(results);
    }


    @GetMapping("/filter")
    @Operation(summary = "Filter trades with pagination and sorting",
               description = "Filters trades based on optional criteria such as book name, counterparty name, trader, trade date range, and status. Supports pagination and sorting.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trades retrieved, filtered, sorted and paginated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameter(s)"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
            public ResponseEntity<?> filterTrades(
            @RequestParam(required = false) String counterparty,
            @RequestParam(required = false) String book,
            @RequestParam(required = false) Long trader,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "tradeDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        var tradesPage = tradeService.searchTrades(counterparty, book, trader, status, from, to, page, size, sortBy, direction);
        var tradeDTOs = tradesPage.getContent().stream().map(tradeMapper::toDto).toList();

        return ResponseEntity.ok(Map.of(
                "content", tradeDTOs,
                "page", tradesPage.getNumber(),
                "size", tradesPage.getSize(),
                "totalElements", tradesPage.getTotalElements(),
                "totalPages", tradesPage.getTotalPages(),
                "sortBy", sortBy,
                "direction", direction
        ));
    }



   /* RSQL search endpoint for complex queries
    RSQL stands for RESTful Service Query Language
    It allows clients to construct complex queries using a simple and standardized syntax
    This endpoint parses the RSQL query string, converts it into a Specification, and retrieves matching trades
    Example RSQL queries:
    - counterparty==AcmeCorp
    - book.bookName==TradingBook1;tradeStatus.tradeStatus==NEW
    - tradeDate=ge=2025-01-01;tradeDate=le=2025-12-31
    - traderUser.firstName==John,traderUser.lastName==Doe*/
    @GetMapping("/rsql")
    @Operation(summary = "Advanced search for trades with RSQL (RESTful Service Query Language)",
               description = "Searches for trades using RSQL query syntax for complex filtering")
     @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trades retrieved successfully"),   
        @ApiResponse(responseCode = "400", description = "Invalid RSQL syntax"),
        @ApiResponse(responseCode = "500", description = "Internal server error") 
    })
    public ResponseEntity<List<TradeDTO>> searchRsql(
            @RequestParam(required = false) String query
    ) {
        RsqlSpecificationBuilder<Trade> builder = new RsqlSpecificationBuilder<>();
        Specification<Trade> spec = builder.parse(query);
        List<Trade> trades = tradeRepository.findAll(spec);
        List<TradeDTO> result = trades.stream().map(tradeMapper::toDto).toList();
        return ResponseEntity.ok(result);
    }



    // private Sort parseSort(String sortParam) {
    //     // e.g. "tradeDate,desc;tradeId,asc"
    //     List<Sort.Order> orders = Arrays.stream(sortParam.split(";"))
    //             .map(s -> s.split(","))
    //             .map(arr -> new Sort.Order(
    //                     (arr.length > 1 && "desc".equalsIgnoreCase(arr[1])) ? Sort.Direction.DESC : Sort.Direction.ASC,
    //                     arr[0]
    //             )).toList();
    //     return Sort.by(orders);
    // }

}
