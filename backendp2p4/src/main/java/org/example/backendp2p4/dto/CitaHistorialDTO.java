package org.example.backendp2p4.dto;

import org.example.backendp2p4.logic.Cita;

import java.time.LocalDateTime;

public class CitaHistorialDTO {
    private Integer id;
    private String doctorNombre;
    private String especialidad;
    private String ciudad;
    private String instalacion;
    private LocalDateTime fechaHora;
    private String estado;
    private String notas;

    public CitaHistorialDTO(Cita cita) {
        this.id = cita.getId();
        this.doctorNombre = cita.getDoctor().getUsuario().getNombre();
        this.especialidad = cita.getDoctor().getEspecialidad();
        this.ciudad = cita.getDoctor().getCiudad();
        this.instalacion = cita.getDoctor().getInstalacion();
        this.fechaHora = cita.getFechaHora();
        this.estado = cita.getEstado();
        this.notas = cita.getNotas();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDoctorNombre() {
        return doctorNombre;
    }

    public void setDoctorNombre(String doctorNombre) {this.doctorNombre = doctorNombre;}

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getInstalacion() {
        return instalacion;
    }

    public void setInstalacion(String instalacion) {
        this.instalacion = instalacion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }
}
