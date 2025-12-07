package com.evacorp.taskflow.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evacorp.taskflow.entity.VerificacionUsuario;

public interface VerificacionUsuarioRepository extends JpaRepository<VerificacionUsuario, Long> {

    Optional<VerificacionUsuario> findByUsuarioCorreoAndCodigoAndEstado(
            String correo,
            String codigo,
            String estado
    );
}
