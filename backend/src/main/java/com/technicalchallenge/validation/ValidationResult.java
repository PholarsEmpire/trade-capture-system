package com.technicalchallenge.validation;

import java.util.ArrayList;
import java.util.List;


// FOLA ADDED: A simple object class to store validation results and error messages
// This helps in aggregating multiple validation errors and returning them together
// to the caller, improving user feedback and debugging.
// It can be extended in the future to include warning messages or other metadata if needed.


public class ValidationResult {
    private boolean valid;
    private final List<String> errors = new ArrayList<>();

    // Constructor initializes the result as valid with no errors
    public ValidationResult() {
        this.valid = true;
    }
    // Method to check if the validation passed without errors
    public boolean isValid() {
        return valid && errors.isEmpty();
    }
    // Method to add an error message and mark the result as invalid
    public void addError(String error) {
        this.valid = false;
        this.errors.add(error);
    }

    // Getter for errors list. 
    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + isValid() +
                ", errors=" + errors +
                '}';
    }
}
