package com.csci318.recommendationservice.domain;

/**
 * Value object: a technical attribute of a Catalog product, e.g. "Socket" = "AM5".
 */
public record Specification(String name, String value) {
}
