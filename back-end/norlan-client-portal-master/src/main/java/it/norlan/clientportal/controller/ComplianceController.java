package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.ComplianceDTO;
import it.norlan.clientportal.service.ComplianceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST deputato al monitoraggio della conformità normativa (Compliance).
 * Fornisce gli endpoint per recuperare lo stato di adempimento globale di un'azienda
 * in merito alla sicurezza sul lavoro, scadenze corsi e visite mediche.
 */

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    @Autowired
    private ComplianceService complianceService;

    // Interroga il servizio di compliance per calcolare e restituire gli indicatori di conformità dell'azienda
    @GetMapping("/azienda/{idAzienda}")
    public ResponseEntity<ComplianceDTO> getComplianceStatus(@PathVariable Integer idAzienda) {
        return ResponseEntity.ok(complianceService.calcolaComplianceAzienda(idAzienda));
    }
}
