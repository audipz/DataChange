package io.github.audipz.datachange.example;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

/**
 * OneToOne detail entity to demonstrate dependent inserts.
 */
@Audited
@Entity(name = "PersonProfile")
@Table(name = "person_profile")
public class PersonProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String preferredLanguage;

    @OneToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false, unique = true)
    private Person person;

    public Long getId() {
        return id;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}

