package fr.miage.motus.dictionary.dto;

public record AdminWordDto(Long id, String word, int length, String groupCode, boolean secretWord) {}
