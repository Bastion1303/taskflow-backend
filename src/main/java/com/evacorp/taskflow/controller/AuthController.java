package com.evacorp.taskflow.controller;

import com.evacorp.taskflow.dto.LoginRequest;
import com.evacorp.taskflow.dto.MensajeResponse;
import com.evacorp.taskflow.dto.RegistroRequest;
import com.evacorp.taskflow.dto.VerificarCodigoRequest;
import com.evacorp.taskflow.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    public ResponseEntity<MensajeResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        String mensaje = usuarioService.registrarEstudiante(request);
        return ResponseEntity.ok(new MensajeResponse(mensaje));
    }

    @PostMapping("/verificar-codigo")
    public ResponseEntity<MensajeResponse> verificarCodigo(
            @Valid @RequestBody VerificarCodigoRequest request) {

        String mensaje = usuarioService.verificarCodigo(
                request.getCorreo(),
                request.getCodigo()
        );
        return ResponseEntity.ok(new MensajeResponse(mensaje));
    }

    @PostMapping("/login")
    public ResponseEntity<MensajeResponse> login(@Valid @RequestBody LoginRequest request) {
        String mensaje = usuarioService.loginEstudiante(request);
        return ResponseEntity.ok(new MensajeResponse(mensaje));
    }
}

