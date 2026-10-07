package tz.elmkusoma.primary.dto;

import java.util.UUID;

/**
 * Minimal classmate projection exposed to peer students
 * (names + admission number only; no contact or guardian data).
 */
public class ClassmateResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String admissionNumber;

    public ClassmateResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAdmissionNumber() {
        return admissionNumber;
    }

    public void setAdmissionNumber(String admissionNumber) {
        this.admissionNumber = admissionNumber;
    }
}
