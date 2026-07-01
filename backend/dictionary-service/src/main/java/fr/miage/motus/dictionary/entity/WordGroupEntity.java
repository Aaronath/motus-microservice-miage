package fr.miage.motus.dictionary.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "word_groups")
public class WordGroupEntity {

    @Id
    @Column(length = 60)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int length;

    @Column(nullable = false, length = 1)
    private String firstLetter;

    protected WordGroupEntity() {}

    public WordGroupEntity(String id, String name, int length, String firstLetter) {
        this.id = id;
        this.name = name;
        this.length = length;
        this.firstLetter = firstLetter;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getLength() { return length; }
    public String getFirstLetter() { return firstLetter; }

    public void setName(String name) { this.name = name; }
}
