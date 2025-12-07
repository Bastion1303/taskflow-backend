package com.evacorp.taskflow.controller;

import com.evacorp.taskflow.dto.LoginRequest;
import com.evacorp.taskflow.dto.RegistroRequest;
import com.evacorp.taskflow.dto.VerificarCodigoRequest;
import com.evacorp.taskflow.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class ViewController {

    private final UsuarioService usuarioService;

    public ViewController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        if (!model.containsAttribute("registroRequest")) {
            model.addAttribute("registroRequest", new RegistroRequest());
        }
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(
            @Valid @ModelAttribute("registroRequest") RegistroRequest request,
            BindingResult bindingResult,
            Model model) {

        // Validaciones de Bean Validation (NotBlank, Email, etc.)
        if (bindingResult.hasErrors()) {
            model.addAttribute("mensajeError", "Revisa los campos marcados en rojo.");
            return "registro";
        }

        // Regla de dominio institucional y usuario único (negocio)
        String mensaje = usuarioService.registrarEstudiante(request);

        if (mensaje.startsWith("El correo debe ser institucional")
                || mensaje.startsWith("Ya existe un usuario")) {

            model.addAttribute("mensajeError", mensaje);
            return "registro";
        }

        // Registro correcto: se envió el código al correo
        VerificarCodigoRequest verificarReq = new VerificarCodigoRequest();
        verificarReq.setCorreo(request.getCorreo());

        model.addAttribute("mensajeExito", mensaje);
        model.addAttribute("verificarCodigoRequest", verificarReq);
        return "verificar-codigo";
    }

    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(
            @Valid @ModelAttribute("loginRequest") LoginRequest request,
            BindingResult bindingResult,
            Model model) {

        // Validaciones básicas del formulario (no vacío, email válido)
        if (bindingResult.hasErrors()) {
            model.addAttribute("mensajeError", "Revisa el correo y la contraseña.");
            return "login";
        }

        String mensaje = usuarioService.loginEstudiante(request);

        // El servicio ya distingue entre: cuenta no verificada, credenciales inválidas, etc.
        if (mensaje.startsWith("Credenciales inválidas")) {
            model.addAttribute("mensajeError", "Correo o contraseña incorrectos.");
        } else if (mensaje.startsWith("La cuenta aún no está verificada")) {
            model.addAttribute("mensajeError",
                    "Tu cuenta aún no está verificada. Revisa tu correo institucional.");
        } else {
            model.addAttribute("mensajeExito", mensaje);
        }

        return "login";
    }

    @PostMapping("/verificar-codigo")
    public String procesarVerificacion(
            @Valid @ModelAttribute("verificarCodigoRequest") VerificarCodigoRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("mensajeError", "Debes ingresar el código de verificación.");
            return "verificar-codigo";
        }

        String mensaje = usuarioService.verificarCodigo(
                request.getCorreo().trim(), request.getCodigo().trim());

        if ("Cuenta verificada correctamente".equals(mensaje)) {
            model.addAttribute("loginRequest", new LoginRequest());
            model.addAttribute("mensajeExito", mensaje);
            return "login";
        }

        model.addAttribute("mensajeError", mensaje);
        model.addAttribute("verificarCodigoRequest", request);
        return "verificar-codigo";
    }
}

