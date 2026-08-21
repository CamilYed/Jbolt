package github.com.camilyed.jbolt.ui.model;

/** Encapsulates error information for the presentation layer. */
public record UiError(String title, String message, String technicalCode, Throwable cause) {}
