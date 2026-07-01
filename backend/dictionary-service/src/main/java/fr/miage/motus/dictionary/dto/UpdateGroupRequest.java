package fr.miage.motus.dictionary.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateGroupRequest(@NotBlank String name) {}
