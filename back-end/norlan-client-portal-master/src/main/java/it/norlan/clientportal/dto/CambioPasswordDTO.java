package it.norlan.clientportal.dto;

import lombok.Data;

@Data
public class CambioPasswordDTO {
    private String vecchiaPassword;
    private String nuovaPassword;
}
