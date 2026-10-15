package com.lms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lms.service.MaterialProgressService;

@RestController
@RequestMapping("/api/material-progress")
@CrossOrigin(origins = "*")
public class MaterialProgressController {

    @Autowired
    private MaterialProgressService service;

    @PostMapping("/complete/{materialId}")
    public ResponseEntity<String> completeMaterial(
            @PathVariable Long materialId,
            @RequestParam Long studentId) {

        service.markCompleted(studentId, materialId);

        return ResponseEntity.ok("Material Completed Successfully");
    }
    @GetMapping("/student/{studentId}")
public ResponseEntity<?> getCompletedMaterials(
        @PathVariable Long studentId){

    return ResponseEntity.ok(
            service.getCompletedMaterialIds(studentId)
    );
}
}