package fr.miage.motus.dictionary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @Size(max = 60) String id,
        @NotBlank String name,
        int length,
        @NotBlank @Size(min = 1, max = 1) String firstLetter
) {}
