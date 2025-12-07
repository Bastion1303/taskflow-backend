package com.evacorp.taskflow.service;

import com.evacorp.taskflow.dto.LoginRequest;
import com.evacorp.taskflow.dto.RegistroRequest;
import com.evacorp.taskflow.entity.Usuario;
import com.evacorp.taskflow.entity.VerificacionUsuario;
import com.evacorp.taskflow.repository.UsuarioRepository;
import com.evacorp.taskflow.repository.VerificacionUsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final VerificacionUsuarioRepository verificacionUsuarioRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Random random = new Random();

    public UsuarioService(UsuarioRepository usuarioRepository,
                          VerificacionUsuarioRepository verificacionUsuarioRepository,
                          EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.verificacionUsuarioRepository = verificacionUsuarioRepository;
        this.emailService = emailService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    private String generarCodigoVerificacion() {
        int num = random.nextInt(100000); // 0..99999
        return String.format("%05d", num);
    }

    @Transactional
    public String registrarEstudiante(RegistroRequest request) {

        if (!request.getCorreo().endsWith("@tecazuay.edu.ec")) {
            return "El correo debe ser institucional (@tecazuay.edu.ec)";
        }

        // Buscar si ya existe un usuario con ese correo
        Optional<Usuario> existenteOpt = usuarioRepository.findByCorreo(request.getCorreo());
        if (existenteOpt.isPresent()) {
            Usuario existente = existenteOpt.get();

            if ("PENDIENTE".equals(existente.getEstado())) {
                // Ya se registró pero no ha verificado
                return "Ya existe una cuenta con este correo. Revisa tu correo institucional y utiliza el código de verificación que ya fue enviado.";
            }

            // Cuenta ya habilitada
            return "Ya existe un usuario con ese correo. Inicia sesión.";
        }

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(request.getNombreCompleto());
        usuario.setCorreo(request.getCorreo());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRol("ESTUDIANTE");
        usuario.setEstado("PENDIENTE");
        usuario.setCreadoEn(Instant.now());

        usuario = usuarioRepository.save(usuario);

        String codigo = generarCodigoVerificacion();

        VerificacionUsuario verificacion = new VerificacionUsuario();
        verificacion.setUsuario(usuario);
        verificacion.setCodigo(codigo);
        verificacion.setEstado("PENDIENTE");
        verificacion.setCreadoEn(Instant.now());

        verificacionUsuarioRepository.save(verificacion);

        // Enviar el código solo por correo, no devolverlo al frontend.
        emailService.enviarCodigoVerificacion(usuario.getCorreo(), codigo);

        return "Usuario registrado. Revisa tu correo para el código de verificación.";
    }

    @Transactional
    public String verificarCodigo(String correo, String codigoIngresado) {

        Optional<VerificacionUsuario> opt = verificacionUsuarioRepository
                .findByUsuarioCorreoAndCodigoAndEstado(correo, codigoIngresado, "PENDIENTE");

        if (opt.isEmpty()) {
            return "Código inválido o ya utilizado";
        }

        VerificacionUsuario verificacion = opt.get();

        Usuario usuario = verificacion.getUsuario();
        usuario.setEstado("HABILITADO");
        usuario.setActualizadoEn(Instant.now());
        usuarioRepository.save(usuario);

        verificacion.setEstado("USADO");
        verificacion.setResueltoEn(Instant.now());
        verificacionUsuarioRepository.save(verificacion);

        return "Cuenta verificada correctamente";
    }

    @Transactional(readOnly = true)
    public String loginEstudiante(LoginRequest request) {

        Optional<Usuario> optUsuario = usuarioRepository.findByCorreo(request.getCorreo());

        if (optUsuario.isEmpty()) {
            return "Credenciales inválidas";
        }

        Usuario usuario = optUsuario.get();

        if (!"HABILITADO".equals(usuario.getEstado())) {
            return "La cuenta aún no está verificada";
        }

        boolean passwordOk = passwordEncoder.matches(
                request.getPassword(),
                usuario.getPasswordHash()
        );

        if (!passwordOk) {
            return "Credenciales inválidas";
        }

        // Más adelante aquí generaremos un JWT o token.
        return "Login correcto. Bienvenido, " + usuario.getNombreCompleto();
    }
}
