package com.technicalchallenge.dto; 

import jakarta.validation.constraints.NotBlank; 
import jakarta.validation.constraints.Size; 

public class SettlementInstructionsUpdateDTO { 
    @NotBlank(message = "Settlement instructions cannot be empty") 
    @Size(min = 10, max = 500, message = "Settlement instructions must be between 10 and 500 characters") 
    private String instructions; 

    public String getInstructions() { 
        return instructions; 
    } 

    public void setInstructions(String instructions) { 
        this.instructions = instructions; 
    } 
}