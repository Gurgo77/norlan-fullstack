package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.ComplianceDTO;
import it.norlan.clientportal.service.ComplianceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    @Autowired
    private ComplianceService complianceService;

    @GetMapping("/azienda/{idAzienda}")

    public ResponseEntity<ComplianceDTO> getComplianceStatus(@PathVariable Integer idAzienda) {
        return ResponseEntity.ok(complianceService.calcolaComplianceAzienda(idAzienda));
    }
}
