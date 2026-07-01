package fr.miage.motus.dictionary.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "words", indexes = @Index(columnList = "length"))
public class WordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String word;

    @Column(nullable = false)
    private int length;

    @Column(length = 60)
    private String groupCode;

    @Column(nullable = false)
    private boolean secretWord;

    protected WordEntity() {}

    public WordEntity(String word) {
        this(word, null, false);
    }

    public WordEntity(String word, String groupCode) {
        this(word, groupCode, false);
    }

    public WordEntity(String word, String groupCode, boolean secretWord) {
        this.word = word.toUpperCase();
        this.length = this.word.length();
        this.groupCode = groupCode;
        this.secretWord = secretWord;
    }

    public Long getId() { return id; }
    public String getWord() { return word; }
    public int getLength() { return length; }
    public String getGroupCode() { return groupCode; }
    public boolean isSecretWord() { return secretWord; }

    public void setWord(String word) {
        this.word = word.toUpperCase();
        this.length = this.word.length();
    }

    public void setSecretWord(boolean secretWord) {
        this.secretWord = secretWord;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = (groupCode == null || groupCode.isBlank()) ? null : groupCode;
    }
}
